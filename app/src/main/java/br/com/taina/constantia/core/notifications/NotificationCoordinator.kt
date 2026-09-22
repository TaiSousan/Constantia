package br.com.taina.constantia.core.notifications

import br.com.taina.constantia.core.database.NotificationEventEntity
import br.com.taina.constantia.core.preferences.AppPreferences
import br.com.taina.constantia.core.repository.ContextualRepository
import br.com.taina.constantia.core.repository.ProfileRepository
import br.com.taina.constantia.core.repository.StudyRepository
import br.com.taina.constantia.core.repository.TrainingRepository
import br.com.taina.constantia.engine.NotificationPlanInput
import br.com.taina.constantia.engine.NotificationPlannerEngine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

class NotificationCoordinator(
    private val profileRepository: ProfileRepository,
    private val trainingRepository: TrainingRepository,
    private val studyRepository: StudyRepository,
    private val contextualRepository: ContextualRepository,
    private val preferences: AppPreferences,
    private val scheduler: ContextualNotificationScheduler,
    private val engine: NotificationPlannerEngine = NotificationPlannerEngine()
) {
    suspend fun planNext36Hours(nowMillis: Long = System.currentTimeMillis()) {
        val prefs = preferences.state.first()
        if (!prefs.contextualNotifications) return
        val routine = profileRepository.routineProfile()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        for (date in listOf(today, today.plusDays(1))) {
            val workout = runCatching { trainingRepository.ensureReady(); trainingRepository.getTodayWorkout(date) }.getOrNull()
            val goals = runCatching { studyRepository.getTodayGoals(date) }.getOrDefault(emptyList())
            val questions = runCatching { studyRepository.getDailyQuestions(date, prefs.studyQuestionsPerDay.coerceAtLeast(1)) }.getOrDefault(emptyList())
            val stats = listOf("MOTIVATION", "STUDY_QUESTION", "STUDY_REMINDER", "PRE_WORKOUT")
                .associateWith { contextualRepository.interactionStats(it) }
            val planned = engine.plan(
                NotificationPlanInput(
                    lunchMinuteOfDay = routine?.lunchMinuteOfDay,
                    workoutMinuteOfDay = workout?.scheduledMinuteOfDay,
                    workoutPending = workout != null && !workout.completedToday,
                    preferredStudyMinuteOfDay = routine?.preferredStudyMinuteOfDay,
                    studyDue = goals.isNotEmpty(),
                    dueQuestionPrompts = questions.map { it.question.id to it.question.prompt },
                    motivationalEnabled = prefs.motivationalNotifications,
                    workoutReminderEnabled = prefs.workoutReminders,
                    studyReminderEnabled = prefs.studyReminders,
                    studyQuestionsEnabled = prefs.studyQuestionNotifications,
                    questionsPerDay = prefs.studyQuestionsPerDay,
                    maxNonEssential = prefs.maxNonEssentialNotificationsPerDay,
                    messageSeed = date.toEpochDay().toInt(),
                    stats = stats
                )
            )
            for (candidate in planned) {
                val hour = candidate.minuteOfDay / 60
                val minute = candidate.minuteOfDay % 60
                val atMillis = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
                if (atMillis <= nowMillis || atMillis > nowMillis + 36 * 60 * 60_000L) continue
                val dedupe = "${date}:${candidate.type}:${candidate.sourceRef}:${candidate.minuteOfDay}"
                val id = contextualRepository.insertScheduledNotification(
                    NotificationEventEntity(
                        dedupeKey = dedupe,
                        type = candidate.type,
                        title = candidate.title,
                        body = candidate.body,
                        sourceRef = candidate.sourceRef,
                        plannedAtMillis = atMillis,
                        plannedMinuteOfDay = candidate.minuteOfDay
                    )
                )
                if (id > 0) {
                    scheduler.schedule(
                        NotificationEventEntity(
                            id = id,
                            dedupeKey = dedupe,
                            type = candidate.type,
                            title = candidate.title,
                            body = candidate.body,
                            sourceRef = candidate.sourceRef,
                            plannedAtMillis = atMillis,
                            plannedMinuteOfDay = candidate.minuteOfDay
                        )
                    )
                }
            }
        }
    }
}
