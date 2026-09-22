package br.com.taina.constantia.engine

data class FocusGateState(
    val workoutCompletedToday: Boolean,
    val completedFocusSessionsToday: Int
)

data class FocusGateDecision(
    val unlocked: Boolean,
    val reason: String
)

class FocusGateEngine {
    fun evaluate(conditionType: String, threshold: Int, state: FocusGateState, overrideActive: Boolean): FocusGateDecision {
        if (overrideActive) return FocusGateDecision(true, "Exceção temporária ativa.")
        return when (conditionType) {
            "WORKOUT_TODAY" -> if (state.workoutCompletedToday) {
                FocusGateDecision(true, "Treino do dia concluído.")
            } else FocusGateDecision(false, "Conclua o treino do dia.")
            "FOCUS_SESSIONS_TODAY" -> if (state.completedFocusSessionsToday >= threshold.coerceAtLeast(1)) {
                FocusGateDecision(true, "Meta de foco concluída.")
            } else FocusGateDecision(false, "Conclua ${threshold.coerceAtLeast(1)} bloco(s) de foco.")
            else -> FocusGateDecision(true, "Sem condição ativa.")
        }
    }
}
