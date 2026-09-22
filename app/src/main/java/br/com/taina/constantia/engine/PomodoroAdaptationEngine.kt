package br.com.taina.constantia.engine

class PomodoroAdaptationEngine {
    enum class Action { INCREASE, MAINTAIN, DECREASE }
    data class Decision(val action: Action, val suggestedMinutes: Int, val reason: String)

    fun evaluate(currentMinutes: Int, recentCompletion: List<Boolean>): Decision {
        val current = currentMinutes.coerceIn(15, 60)
        if (recentCompletion.size < 8) {
            return Decision(Action.MAINTAIN, current, "Ainda há poucas sessões para sugerir mudança.")
        }
        val rate = recentCompletion.count { it }.toDouble() / recentCompletion.size
        return when {
            rate >= 0.85 && current < 45 -> Decision(
                Action.INCREASE,
                current + 5,
                "Alta taxa de conclusão nas sessões recentes."
            )
            rate < 0.60 && current > 20 -> Decision(
                Action.DECREASE,
                current - 5,
                "A taxa de conclusão caiu; um bloco menor pode melhorar a aderência."
            )
            else -> Decision(Action.MAINTAIN, current, "A duração atual está adequada aos seus dados recentes.")
        }
    }
}
