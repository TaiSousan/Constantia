package br.com.taina.constantia.core.model

enum class WorkoutMode { NORMAL, QUICK }
enum class ProgressionAction { INCREASE_LOAD, MAINTAIN, CONSIDER_REDUCTION }
enum class SuggestionStatus { PENDING, ACCEPTED, REJECTED }

data class QuickExerciseSelection(
    val workoutExerciseId: Long,
    val selectedSets: Int
)

data class ProgressionDecision(
    val action: ProgressionAction,
    val currentLoadKg: Double,
    val suggestedLoadKg: Double,
    val rationale: String
)

data class StagnationAssessment(
    val state: StagnationState,
    val rationale: String
)
