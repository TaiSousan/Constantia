package br.com.taina.constantia.engine

import kotlin.math.roundToInt

data class AdherenceMetric(
    val completed: Int,
    val planned: Int
) {
    val ratio: Double? get() = if (planned > 0) completed.coerceAtMost(planned).toDouble() / planned else null
    val percent: Int? get() = ratio?.times(100)?.roundToInt()
}

data class FocusMetric(
    val completed: Int,
    val started: Int
) {
    val ratio: Double? get() = if (started > 0) completed.toDouble() / started else null
    val percent: Int? get() = ratio?.times(100)?.roundToInt()
}

data class WeightWeekComparison(
    val currentAverageKg: Double?,
    val previousAverageKg: Double?
) {
    val deltaKg: Double? get() = if (currentAverageKg != null && previousAverageKg != null) currentAverageKg - previousAverageKg else null
}

data class FailureReasonCount(val reason: String, val count: Int)

data class ProgressSuggestion(
    val area: String,
    val title: String,
    val rationale: String
)

/**
 * Consolida dados já registrados; não atribui causas e não altera planos.
 * As sugestões são deliberadamente conservadoras e sempre precisam de decisão da usuária.
 */
class ProgressEngine {
    fun average(values: List<Double>): Double? = values.takeIf { it.isNotEmpty() }?.average()

    fun aggregateFailureReasons(reasons: List<String>, limit: Int = 3): List<FailureReasonCount> = reasons
        .map { it.trim().ifBlank { "Não informado" } }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(limit.coerceAtLeast(1))
        .map { FailureReasonCount(it.key, it.value) }

    fun suggestions(
        training: AdherenceMetric,
        study: AdherenceMetric,
        activities: AdherenceMetric,
        focus: FocusMetric,
        failureReasons: List<FailureReasonCount>
    ): List<ProgressSuggestion> {
        val out = mutableListOf<ProgressSuggestion>()

        if (training.planned >= 2 && (training.ratio ?: 1.0) < 0.65) {
            out += ProgressSuggestion(
                area = "Treino",
                title = "Revisar encaixe dos treinos",
                rationale = "Você concluiu ${training.completed} de ${training.planned} treinos previstos no período. Antes de aumentar volume ou trocar a ficha, vale revisar dias e duração das sessões."
            )
        }

        if (study.planned >= 3 && (study.ratio ?: 1.0) < 0.65) {
            out += ProgressSuggestion(
                area = "Estudo",
                title = "Reorganizar a meta de estudo",
                rationale = "Foram ${study.completed} de ${study.planned} sessões previstas. O primeiro ajuste sugerido é horário/carga semanal, não aumentar o tamanho dos blocos."
            )
        }

        if (focus.started >= 5 && (focus.ratio ?: 1.0) < 0.65) {
            out += ProgressSuggestion(
                area = "Foco",
                title = "Testar blocos de foco mais curtos",
                rationale = "Você concluiu ${focus.completed} de ${focus.started} blocos iniciados. Uma redução pequena na duração pode ser testada antes de tentar aumentar o Pomodoro."
            )
        }

        if (activities.planned >= 5 && (activities.ratio ?: 1.0) < 0.70) {
            val dominant = failureReasons.firstOrNull()
            out += ProgressSuggestion(
                area = "Rotina",
                title = "Revisar obrigações que estão ficando para trás",
                rationale = buildString {
                    append("Você concluiu ${activities.completed} de ${activities.planned} obrigações previstas.")
                    if (dominant != null) append(" O motivo mais registrado foi “${dominant.reason}” (${dominant.count}x).")
                }
            )
        }

        val observable = listOfNotNull(training.ratio, study.ratio, activities.ratio)
        if (out.isEmpty() && observable.size >= 2 && observable.all { it >= 0.85 }) {
            out += ProgressSuggestion(
                area = "Geral",
                title = "Manter a estrutura atual",
                rationale = "A aderência das áreas acompanhadas está consistente. Não há motivo, por estes dados, para aumentar a complexidade da rotina nesta semana."
            )
        }

        return out.take(4)
    }
}
