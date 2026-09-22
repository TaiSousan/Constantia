import br.com.taina.constantia.engine.*

fun main() {
    val planner = NotificationPlannerEngine()
    val plan = planner.plan(
        NotificationPlanInput(
            lunchMinuteOfDay = 12 * 60 + 30,
            workoutMinuteOfDay = 18 * 60 + 30,
            workoutPending = true,
            preferredStudyMinuteOfDay = 20 * 60,
            studyDue = true,
            dueQuestionPrompts = listOf(1L to "O que significa FTIP?", 2L to "Quando avaliar a transferência de imunidade passiva?"),
            maxNonEssential = 3
        )
    )
    check(plan.any { it.type == "PRE_WORKOUT" && it.minuteOfDay == 17 * 60 + 50 })
    check(plan.count { it.type == "STUDY_QUESTION" } <= 2)
    check(plan.count { !it.essential } <= 3)

    val suppressed = planner.plan(
        NotificationPlanInput(
            motivationalEnabled = true,
            workoutReminderEnabled = false,
            studyReminderEnabled = false,
            studyQuestionsEnabled = false,
            maxNonEssential = 3,
            stats = mapOf("MOTIVATION" to NotificationInteractionStats(samples = 8, opened = 0, dismissed = 6))
        )
    )
    check(suppressed.none { it.type == "MOTIVATION" })

    val gate = FocusGateEngine()
    val locked = gate.evaluate(
        "WORKOUT_TODAY", 1,
        FocusGateState(workoutCompletedToday = false, completedFocusSessionsToday = 0),
        overrideActive = false
    )
    check(!locked.unlocked)
    val focusUnlocked = gate.evaluate(
        "FOCUS_SESSIONS_TODAY", 2,
        FocusGateState(workoutCompletedToday = false, completedFocusSessionsToday = 2),
        overrideActive = false
    )
    check(focusUnlocked.unlocked)
    val override = gate.evaluate(
        "WORKOUT_TODAY", 1,
        FocusGateState(workoutCompletedToday = false, completedFocusSessionsToday = 0),
        overrideActive = true
    )
    check(override.unlocked)

    println("Constantia v0.5 contextual engine tests: OK")
}
