import br.com.taina.constantia.engine.*

fun main() {
    val engine = TrainingCycleReviewEngine()

    val insufficient = engine.evaluate(
        TrainingCycleSignals(
            daysObserved = 7,
            completedSessions = 2,
            plannedSessions = 3,
            currentPlanSessionsPerWeek = 3,
            desiredSessionsPerWeek = 3,
            availableDaysPerWeek = 5,
            currentSessionMinutes = 70,
            planSessionMinutes = 70,
            progressingExerciseCount = 1,
            plateauExerciseCodes = emptySet(),
            watchExerciseCodes = emptySet(),
            unavailableExerciseCodes = emptySet(),
            recoveryLoads = emptyList()
        )
    )
    check(insufficient.status == TrainingCycleReviewStatus.INSUFFICIENT_DATA)
    check(!insufficient.canBuildProposal)

    val plateau = engine.evaluate(
        TrainingCycleSignals(
            daysObserved = 28,
            completedSessions = 11,
            plannedSessions = 12,
            currentPlanSessionsPerWeek = 3,
            desiredSessionsPerWeek = 4,
            availableDaysPerWeek = 7,
            currentSessionMinutes = 120,
            planSessionMinutes = 70,
            progressingExerciseCount = 5,
            plateauExerciseCodes = setOf("LEG_EXTENSION_MACHINE"),
            watchExerciseCodes = emptySet(),
            unavailableExerciseCodes = emptySet(),
            recoveryLoads = emptyList()
        )
    )
    check(plateau.status == TrainingCycleReviewStatus.REVIEW)
    check(plateau.recommendedSessionsPerWeek == 4)
    check("LEG_EXTENSION_MACHINE" in plateau.rotateExerciseCodes)

    val lowAdherence = engine.evaluate(
        TrainingCycleSignals(
            daysObserved = 28,
            completedSessions = 5,
            plannedSessions = 12,
            currentPlanSessionsPerWeek = 3,
            desiredSessionsPerWeek = 5,
            availableDaysPerWeek = 7,
            currentSessionMinutes = 120,
            planSessionMinutes = 60,
            progressingExerciseCount = 0,
            plateauExerciseCodes = emptySet(),
            watchExerciseCodes = emptySet(),
            unavailableExerciseCodes = emptySet(),
            recoveryLoads = emptyList()
        )
    )
    check(lowAdherence.recommendedSessionsPerWeek == 2)

    val recovery = MuscleRecoveryLoad(
        muscleCode = "QUADS",
        muscleName = "Quadríceps",
        recentEffectiveSets = 14.0,
        previousEffectiveSets = 8.0,
        hardEffectiveSetsLast72h = 5.0,
        elevated = true
    )
    val noForcedIncrease = engine.evaluate(
        TrainingCycleSignals(
            daysObserved = 28,
            completedSessions = 12,
            plannedSessions = 12,
            currentPlanSessionsPerWeek = 3,
            desiredSessionsPerWeek = 5,
            availableDaysPerWeek = 7,
            currentSessionMinutes = 120,
            planSessionMinutes = 60,
            progressingExerciseCount = 6,
            plateauExerciseCodes = emptySet(),
            watchExerciseCodes = emptySet(),
            unavailableExerciseCodes = emptySet(),
            recoveryLoads = listOf(recovery)
        )
    )
    check(noForcedIncrease.recommendedSessionsPerWeek == 3)

    println("Constantia RC3.2 adaptive training review tests: OK")
}
