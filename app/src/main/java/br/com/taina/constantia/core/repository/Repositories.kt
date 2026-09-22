package br.com.taina.constantia.core.repository

import br.com.taina.constantia.core.database.*
import br.com.taina.constantia.core.model.FrequencyType
import br.com.taina.constantia.core.model.OccurrenceStatus
import br.com.taina.constantia.engine.ActivityScheduleEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class ProfileRepository(private val dao: ProfileDao) {
    val profile: Flow<UserProfileEntity?> = dao.observeUserProfile()

    suspend fun routineProfile(): RoutineProfileEntity? = dao.getRoutineProfile()

    suspend fun saveOnboarding(
        user: UserProfileEntity,
        training: TrainingProfileEntity,
        routine: RoutineProfileEntity,
        restrictionNotes: String
    ) {
        dao.upsertUserProfile(user.copy(onboardingCompleted = true, updatedAtMillis = System.currentTimeMillis()))
        dao.upsertTrainingProfile(training)
        dao.upsertRoutineProfile(routine)
        dao.insertBodyMetric(BodyMetricEntity(weightKg = user.initialWeightKg))
        if (restrictionNotes.isNotBlank()) {
            dao.insertRestriction(RestrictionEntity(description = restrictionNotes.trim()))
        }
    }
}

data class TodayActivity(
    val definition: ActivityDefinitionEntity,
    val occurrence: ActivityOccurrenceEntity?,
    val completed: Boolean,
    val missed: Boolean = false,
    val failureReason: String = ""
)

class ActivityRepository(
    private val dao: ActivityDao,
    private val scheduleEngine: ActivityScheduleEngine = ActivityScheduleEngine()
) {
    fun observeToday(date: LocalDate): Flow<List<TodayActivity>> {
        val epochDay = date.toEpochDay()
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val sunday = monday.plusDays(6)
        val monthStart = date.withDayOfMonth(1)
        val monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth())
        return combine(
            dao.observeActiveDefinitions(),
            dao.observeOccurrencesForDay(epochDay),
            dao.observeOccurrencesInRange(monday.toEpochDay(), sunday.toEpochDay()),
            dao.observeOccurrencesInRange(monthStart.toEpochDay(), monthEnd.toEpochDay())
        ) { definitions, todayOccurrences, weekOccurrences, monthOccurrences ->
            definitions.mapNotNull { definition ->
                val type = runCatching { FrequencyType.valueOf(definition.frequencyType) }.getOrNull()
                    ?: return@mapNotNull null
                val periodOccurrences = when (type) {
                    FrequencyType.TIMES_PER_WEEK -> weekOccurrences.filter { it.activityId == definition.id }
                    FrequencyType.TIMES_PER_MONTH -> monthOccurrences.filter { it.activityId == definition.id }
                    else -> todayOccurrences.filter { it.activityId == definition.id }
                }
                val due = scheduleEngine.isDueToday(definition, date, periodOccurrences)
                if (!due) null else {
                    val occurrence = todayOccurrences.firstOrNull { it.activityId == definition.id }
                    TodayActivity(
                        definition = definition,
                        occurrence = occurrence,
                        completed = occurrence?.status == OccurrenceStatus.COMPLETED.name,
                        missed = occurrence?.status == OccurrenceStatus.MISSED.name,
                        failureReason = occurrence?.failureReason.orEmpty()
                    )
                }
            }
        }
    }

    suspend fun addDefinition(definition: ActivityDefinitionEntity): Long = dao.insertDefinition(definition)

    suspend fun setCompleted(activityId: Long, date: LocalDate, completed: Boolean) {
        val epoch = date.toEpochDay()
        val current = dao.findOccurrence(activityId, epoch)
        val status = if (completed) OccurrenceStatus.COMPLETED.name else OccurrenceStatus.PENDING.name
        val completedAt = if (completed) System.currentTimeMillis() else null
        if (current == null) {
            dao.insertOccurrence(
                ActivityOccurrenceEntity(
                    activityId = activityId,
                    scheduledEpochDay = epoch,
                    status = status,
                    completedAtMillis = completedAt
                )
            )
        } else {
            dao.updateStatus(activityId, epoch, status, completedAt)
        }
    }

    suspend fun markMissed(activityId: Long, date: LocalDate, reason: String) {
        val epoch = date.toEpochDay()
        val normalizedReason = reason.trim().ifBlank { "Não informado" }
        val current = dao.findOccurrence(activityId, epoch)
        if (current == null) {
            dao.insertOccurrence(
                ActivityOccurrenceEntity(
                    activityId = activityId,
                    scheduledEpochDay = epoch,
                    status = OccurrenceStatus.MISSED.name,
                    failureReason = normalizedReason
                )
            )
        } else {
            dao.updateMissed(activityId, epoch, OccurrenceStatus.MISSED.name, normalizedReason)
        }
    }
}
