package br.com.taina.constantia.core.repository

import br.com.taina.constantia.core.database.*
import br.com.taina.constantia.core.model.FrequencyType
import br.com.taina.constantia.core.model.OccurrenceStatus
import br.com.taina.constantia.engine.*
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil

data class ExerciseProgressLine(
    val exerciseCode: String,
    val exerciseName: String,
    val latestLoadKg: Double,
    val latestReps: Int,
    val previousLoadKg: Double?,
    val previousReps: Int?,
    val latestAtMillis: Long
)

data class WeeklyProgressSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val training: AdherenceMetric,
    val study: AdherenceMetric,
    val activities: AdherenceMetric,
    val focus: FocusMetric,
    val studyMinutes: Int,
    val focusMinutes: Int,
    val focusInterruptions: Int,
    val weight: WeightWeekComparison,
    val failureReasons: List<FailureReasonCount>,
    val exerciseProgress: List<ExerciseProgressLine>,
    val suggestions: List<ProgressSuggestion>
)

class ProgressRepository(
    private val profileDao: ProfileDao,
    private val activityDao: ActivityDao,
    private val trainingDao: TrainingDao,
    private val studyDao: StudyDao,
    private val trainingScheduleEngine: TrainingScheduleEngine = TrainingScheduleEngine(),
    private val engine: ProgressEngine = ProgressEngine()
) {
    suspend fun weeklySummary(anchor: LocalDate = LocalDate.now()): WeeklyProgressSummary {
        val weekStart = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
        val weekEnd = weekStart.plusDays(6)
        val periodEnd = minOf(anchor, weekEnd)
        val zone = ZoneId.systemDefault()
        val startMillis = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = periodEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val trainingMetric = trainingMetric(weekStart, periodEnd, startMillis, endMillis)
        val studyData = studyMetric(periodEnd, startMillis, endMillis)
        val activityData = activityMetric(weekStart, periodEnd)
        val focusSessions = studyDao.getFocusSessionsBetween(startMillis, endMillis)
        val focusMetric = FocusMetric(
            completed = focusSessions.count { it.completed },
            started = focusSessions.size
        )
        val failures = engine.aggregateFailureReasons(activityData.second)
        val weight = weightComparison(weekStart, periodEnd, zone)
        val exerciseProgress = exerciseProgress(periodEnd, zone)
        val suggestions = engine.suggestions(
            training = trainingMetric,
            study = studyData.first,
            activities = activityData.first,
            focus = focusMetric,
            failureReasons = failures
        )

        return WeeklyProgressSummary(
            startDate = weekStart,
            endDate = periodEnd,
            training = trainingMetric,
            study = studyData.first,
            activities = activityData.first,
            focus = focusMetric,
            studyMinutes = studyData.second.sumOf { it.actualMinutes },
            focusMinutes = focusSessions.sumOf { it.actualMinutes },
            focusInterruptions = focusSessions.sumOf { it.interruptions },
            weight = weight,
            failureReasons = failures,
            exerciseProgress = exerciseProgress,
            suggestions = suggestions
        )
    }

    private suspend fun trainingMetric(
        weekStart: LocalDate,
        periodEnd: LocalDate,
        startMillis: Long,
        endMillis: Long
    ): AdherenceMetric {
        val profile = profileDao.getTrainingProfile()
        val activePlan = trainingDao.getActivePlan()
        val planned = if (profile == null || activePlan == null) 0 else {
            val templates = trainingDao.getTemplates(activePlan.id)
            val days = trainingScheduleEngine.resolveScheduledDays(templates.size.coerceAtLeast(1), profile.preferredTrainingDaysCsv)
            generateSequence(weekStart) { it.plusDays(1) }
                .takeWhile { !it.isAfter(periodEnd) }
                .count { it.dayOfWeek in days }
        }
        val completed = trainingDao.getFinishedSessionsBetween(startMillis, endMillis).size
        return AdherenceMetric(completed = completed, planned = planned)
    }

    private suspend fun studyMetric(
        periodEnd: LocalDate,
        startMillis: Long,
        endMillis: Long
    ): Pair<AdherenceMetric, List<StudySessionEntity>> {
        val goals = studyDao.getStudyGoals()
        val day = periodEnd.dayOfWeek.value.coerceIn(1, 7)
        val planned = goals.sumOf { goal -> ceil(goal.sessionsPerWeek.coerceIn(1, 7) * day / 7.0).toInt() }
        val sessions = studyDao.getStudySessionsBetween(startMillis, endMillis)
        return AdherenceMetric(sessions.size, planned) to sessions
    }

    private suspend fun activityMetric(
        weekStart: LocalDate,
        periodEnd: LocalDate
    ): Pair<AdherenceMetric, List<String>> {
        val definitions = activityDao.getActiveDefinitions()
        val occurrences = activityDao.getAllOccurrencesInRange(weekStart.toEpochDay(), periodEnd.toEpochDay())
        val planned = definitions.sumOf { plannedOccurrences(it, weekStart, periodEnd) }
        val completed = occurrences.count { it.status == OccurrenceStatus.COMPLETED.name }
        val failureReasons = occurrences
            .filter { it.status == OccurrenceStatus.MISSED.name }
            .map { it.failureReason }
        return AdherenceMetric(completed, planned) to failureReasons
    }

    private fun plannedOccurrences(definition: ActivityDefinitionEntity, start: LocalDate, end: LocalDate): Int {
        if (!definition.active || end.isBefore(start)) return 0
        val type = runCatching { FrequencyType.valueOf(definition.frequencyType) }.getOrNull() ?: return 0
        val anchor = LocalDate.ofEpochDay(definition.anchorEpochDay)
        val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
        return when (type) {
            FrequencyType.DAILY -> dates.count { !it.isBefore(anchor) }
            FrequencyType.SPECIFIC_DAYS -> {
                val allowed = definition.specificDaysCsv.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
                dates.count { !it.isBefore(anchor) && it.dayOfWeek.value in allowed }
            }
            FrequencyType.EVERY_X_DAYS -> {
                val every = definition.everyXDays.coerceAtLeast(1)
                dates.count {
                    val delta = it.toEpochDay() - definition.anchorEpochDay
                    delta >= 0 && delta % every == 0L
                }
            }
            FrequencyType.ONCE -> if (!anchor.isBefore(start) && !anchor.isAfter(end)) 1 else 0
            FrequencyType.TIMES_PER_WEEK -> {
                val target = definition.timesPerPeriod.coerceIn(1, 7)
                dates.filter { !it.isBefore(anchor) }.sumOf { date ->
                    val beforeDay = date.dayOfWeek.value - 1
                    val expectedBefore = ceil(target * beforeDay / 7.0).toInt()
                    val expectedToday = ceil(target * date.dayOfWeek.value / 7.0).toInt()
                    (expectedToday - expectedBefore).coerceAtLeast(0)
                }
            }
            FrequencyType.TIMES_PER_MONTH -> {
                dates.filter { !it.isBefore(anchor) }.sumOf { date ->
                    val target = definition.timesPerPeriod.coerceAtLeast(1)
                    val beforeDay = date.dayOfMonth - 1
                    val expectedBefore = ceil(target * beforeDay / date.lengthOfMonth().toDouble()).toInt()
                    val expectedToday = ceil(target * date.dayOfMonth / date.lengthOfMonth().toDouble()).toInt()
                    (expectedToday - expectedBefore).coerceAtLeast(0)
                }
            }
        }
    }

    private suspend fun weightComparison(
        weekStart: LocalDate,
        periodEnd: LocalDate,
        zone: ZoneId
    ): WeightWeekComparison {
        val dayCount = java.time.temporal.ChronoUnit.DAYS.between(weekStart, periodEnd) + 1
        val currentStart = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val currentEnd = periodEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val previousStartDate = weekStart.minusWeeks(1)
        val previousEndDate = previousStartDate.plusDays(dayCount - 1)
        val previousStart = previousStartDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val previousEnd = previousEndDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val current = profileDao.getBodyMetricsBetween(currentStart, currentEnd).map { it.weightKg }
        val previous = profileDao.getBodyMetricsBetween(previousStart, previousEnd).map { it.weightKg }
        return WeightWeekComparison(engine.average(current), engine.average(previous))
    }

    private suspend fun exerciseProgress(periodEnd: LocalDate, zone: ZoneId): List<ExerciseProgressLine> {
        val startMillis = periodEnd.minusDays(59).atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = periodEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val sets = trainingDao.getExerciseSetsBetween(startMillis, endMillis)
        if (sets.isEmpty()) return emptyList()
        val exerciseMap = trainingDao.getExercises(sets.map { it.exerciseCode }.distinct()).associateBy { it.code }

        return sets.groupBy { it.exerciseCode }.mapNotNull { (exerciseCode, exerciseSets) ->
            val sessions = exerciseSets.groupBy { it.sessionId }
                .map { (_, sessionSets) ->
                    val best = sessionSets.maxWithOrNull(compareBy<ExerciseSetEntity> { it.loadKg }.thenBy { it.reps }) ?: return@map null
                    Triple(sessionSets.maxOf { it.completedAtMillis }, best, sessionSets)
                }
                .filterNotNull()
                .sortedByDescending { it.first }
            val latest = sessions.getOrNull(0) ?: return@mapNotNull null
            val previous = sessions.getOrNull(1)
            val exercise = exerciseMap[exerciseCode] ?: return@mapNotNull null
            ExerciseProgressLine(
                exerciseCode = exerciseCode,
                exerciseName = exercise.name,
                latestLoadKg = latest.second.loadKg,
                latestReps = latest.second.reps,
                previousLoadKg = previous?.second?.loadKg,
                previousReps = previous?.second?.reps,
                latestAtMillis = latest.first
            )
        }.sortedByDescending { it.latestAtMillis }.take(6)
    }
}
