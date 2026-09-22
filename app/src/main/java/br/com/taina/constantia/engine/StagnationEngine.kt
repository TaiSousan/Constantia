package br.com.taina.constantia.engine

import br.com.taina.constantia.core.database.ExerciseSetEntity
import br.com.taina.constantia.core.model.StagnationAssessment
import br.com.taina.constantia.core.model.StagnationState
import kotlin.math.abs

/**
 * Detector deliberadamente conservador. Não declara platô com uma ou duas sessões ruins.
 * Só compara sessões do mesmo slot/exercício planejado e exige pelo menos três exposições.
 */
class StagnationEngine {
    fun evaluate(sets: List<ExerciseSetEntity>, plannedExerciseCode: String): StagnationAssessment {
        val bySession = sets
            .filter { it.exerciseCode == plannedExerciseCode }
            .groupBy { it.sessionId }
            .values
            .filter { it.isNotEmpty() }
            .take(3)

        if (bySession.size < 2) {
            return StagnationAssessment(StagnationState.NONE, "Histórico ainda insuficiente para avaliar tendência.")
        }
        if (bySession.size == 2) {
            return StagnationAssessment(StagnationState.WATCH, "Duas exposições registradas; o app ainda observa antes de sugerir mudança.")
        }

        val latest = summary(bySession[0])
        val middle = summary(bySession[1])
        val oldest = summary(bySession[2])
        val loadStable = listOf(latest.avgLoad, middle.avgLoad, oldest.avgLoad).maxOrNull()!! -
            listOf(latest.avgLoad, middle.avgLoad, oldest.avgLoad).minOrNull()!! <= maxOf(1.0, oldest.avgLoad * 0.025)
        val noRepGain = latest.totalReps <= oldest.totalReps && middle.totalReps <= oldest.totalReps + 1
        val hardEffort = latest.avgRir <= 1.5 && middle.avgRir <= 1.5

        return when {
            loadStable && noRepGain && hardEffort -> StagnationAssessment(
                StagnationState.PLATEAU,
                "Três exposições com carga semelhante, sem ganho claro de repetições e esforço alto. Vale revisar recuperação, técnica, volume ou exercício antes de aumentar demanda."
            )
            loadStable && noRepGain -> StagnationAssessment(
                StagnationState.WATCH,
                "O desempenho ficou estável por três exposições. Ainda não é motivo para trocar o exercício, mas merece observação."
            )
            else -> StagnationAssessment(StagnationState.NONE, "Há progressão ou variação suficiente entre as últimas exposições.")
        }
    }

    private data class Summary(val avgLoad: Double, val totalReps: Int, val avgRir: Double)

    private fun summary(sets: List<ExerciseSetEntity>): Summary = Summary(
        avgLoad = sets.map { it.loadKg }.average(),
        totalReps = sets.sumOf { it.reps },
        avgRir = sets.map { it.rir }.average()
    )
}
