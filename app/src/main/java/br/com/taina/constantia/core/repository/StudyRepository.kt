package br.com.taina.constantia.core.repository

import br.com.taina.constantia.core.database.*
import br.com.taina.constantia.engine.PomodoroAdaptationEngine
import br.com.taina.constantia.engine.StudyReviewEngine
import br.com.taina.constantia.engine.StudyScheduleEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId


data class TodayStudyGoal(
    val goal: StudyGoalEntity,
    val subject: SubjectEntity,
    val topic: StudyTopicEntity?,
    val completedThisWeek: Int,
    val dueToday: Boolean
)

data class DueQuestion(
    val question: ReviewQuestionEntity,
    val topic: StudyTopicEntity?,
    val subject: SubjectEntity?
)

class StudyRepository(
    private val dao: StudyDao,
    private val scheduleEngine: StudyScheduleEngine = StudyScheduleEngine(),
    private val reviewEngine: StudyReviewEngine = StudyReviewEngine(),
    private val pomodoroEngine: PomodoroAdaptationEngine = PomodoroAdaptationEngine()
) {
    val subjects: Flow<List<SubjectEntity>> = dao.observeSubjects()
    val topics: Flow<List<StudyTopicEntity>> = dao.observeTopics()
    val goals: Flow<List<StudyGoalEntity>> = dao.observeStudyGoals()
    val questions: Flow<List<ReviewQuestionEntity>> = dao.observeQuestions()

    suspend fun addSubject(name: String): Long {
        require(name.isNotBlank())
        return dao.insertSubject(SubjectEntity(name = name.trim()))
    }

    suspend fun addTopic(subjectId: Long, name: String, notes: String = ""): Long {
        require(name.isNotBlank())
        return dao.insertTopic(StudyTopicEntity(subjectId = subjectId, name = name.trim(), notes = notes.trim()))
    }

    suspend fun addGoal(subjectId: Long, topicId: Long?, sessionsPerWeek: Int, targetMinutes: Int): Long {
        return dao.insertStudyGoal(
            StudyGoalEntity(
                subjectId = subjectId,
                topicId = topicId,
                sessionsPerWeek = sessionsPerWeek.coerceIn(1, 7),
                targetMinutesPerSession = targetMinutes.coerceIn(10, 180)
            )
        )
    }

    suspend fun addQuestion(topicId: Long, prompt: String, answer: String, date: LocalDate = LocalDate.now(), sourceLabel: String = "MANUAL"): Long {
        require(prompt.isNotBlank() && answer.isNotBlank())
        return dao.insertQuestion(
            ReviewQuestionEntity(
                topicId = topicId,
                prompt = prompt.trim(),
                answer = answer.trim(),
                sourceLabel = sourceLabel,
                nextReviewEpochDay = date.toEpochDay()
            )
        )
    }

    suspend fun getTodayGoals(date: LocalDate = LocalDate.now()): List<TodayStudyGoal> {
        val zone = ZoneId.systemDefault()
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val startMillis = monday.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = monday.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val subjects = dao.observeSubjects().firstValue()
        val topics = dao.observeTopics().firstValue()
        val goals = dao.observeStudyGoals().firstValue()
        return goals.mapNotNull { goal ->
            val subject = subjects.firstOrNull { it.id == goal.subjectId } ?: return@mapNotNull null
            val topic = goal.topicId?.let { id -> topics.firstOrNull { it.id == id } }
            val completed = dao.getStudySessionsForSubjectBetween(goal.subjectId, startMillis, endMillis)
                .count { session -> goal.topicId == null || session.topicId == goal.topicId }
            TodayStudyGoal(
                goal = goal,
                subject = subject,
                topic = topic,
                completedThisWeek = completed,
                dueToday = scheduleEngine.isDueToday(goal.sessionsPerWeek, completed, date.dayOfWeek.value)
            )
        }.filter { it.dueToday }
    }

    suspend fun getDailyQuestions(date: LocalDate = LocalDate.now(), limit: Int = 2): List<DueQuestion> {
        val duePool = dao.getDueQuestions(date.toEpochDay(), 20)
        val plannedTopicIds = getTodayGoals(date).mapNotNull { it.goal.topicId }.toSet()
        val questions = duePool.sortedWith(
            compareByDescending<ReviewQuestionEntity> { it.topicId in plannedTopicIds }
                .thenBy { it.nextReviewEpochDay }
                .thenBy { it.reviewCount }
        ).take(limit.coerceIn(1, 2))
        val topics = dao.observeTopics().firstValue()
        val subjects = dao.observeSubjects().firstValue()
        return questions.map { q ->
            val topic = topics.firstOrNull { it.id == q.topicId }
            val subject = topic?.let { t -> subjects.firstOrNull { it.id == t.subjectId } }
            DueQuestion(q, topic, subject)
        }
    }

    suspend fun rateQuestion(questionId: Long, rating: StudyReviewEngine.Rating, date: LocalDate = LocalDate.now()): StudyReviewEngine.ReviewDecision? {
        val question = dao.getQuestion(questionId) ?: return null
        val decision = reviewEngine.nextInterval(question.intervalDays, question.reviewCount, rating)
        dao.insertReviewAttempt(
            ReviewAttemptEntity(
                questionId = questionId,
                rating = rating.name,
                previousIntervalDays = question.intervalDays,
                nextIntervalDays = decision.nextIntervalDays
            )
        )
        dao.updateQuestionSchedule(
            id = questionId,
            nextEpochDay = date.plusDays(decision.nextIntervalDays.toLong()).toEpochDay(),
            intervalDays = decision.nextIntervalDays,
            rating = rating.name
        )
        return decision
    }

    suspend fun completeFocusSession(
        label: String,
        subjectId: Long?,
        topicId: Long?,
        plannedMinutes: Int,
        actualMinutes: Int,
        interruptions: Int,
        startedAtMillis: Long,
        finishedAtMillis: Long,
        completed: Boolean
    ) {
        dao.insertFocusSession(
            FocusSessionEntity(
                subjectId = subjectId,
                topicId = topicId,
                label = label.ifBlank { "Foco" },
                plannedMinutes = plannedMinutes,
                actualMinutes = actualMinutes.coerceAtLeast(0),
                interruptions = interruptions.coerceAtLeast(0),
                completed = completed,
                startedAtMillis = startedAtMillis,
                finishedAtMillis = finishedAtMillis
            )
        )
        if (completed && subjectId != null) {
            dao.insertStudySession(
                StudySessionEntity(
                    subjectId = subjectId,
                    topicId = topicId,
                    plannedMinutes = plannedMinutes,
                    actualMinutes = actualMinutes.coerceAtLeast(1),
                    startedAtMillis = startedAtMillis,
                    finishedAtMillis = finishedAtMillis
                )
            )
        }
    }

    suspend fun pomodoroSuggestion(currentMinutes: Int): PomodoroAdaptationEngine.Decision {
        val recent = dao.getRecentFocusSessions(12)
        return pomodoroEngine.evaluate(currentMinutes, recent.map { it.completed })
    }
}

/** Small helper so Room Flow can be sampled in repository-only calculations. */
private suspend fun <T> Flow<T>.firstValue(): T = this.first()
