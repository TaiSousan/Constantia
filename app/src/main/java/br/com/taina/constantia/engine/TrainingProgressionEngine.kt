package br.com.taina.constantia.engine

import br.com.taina.constantia.core.database.EquipmentEntity
import br.com.taina.constantia.core.database.ExerciseSetEntity
import br.com.taina.constantia.core.database.WorkoutExerciseEntity
import br.com.taina.constantia.core.model.ProgressionAction
import br.com.taina.constantia.core.model.ProgressionDecision
import kotlin.math.max

class TrainingProgressionEngine {
    fun evaluate(
        prescription: WorkoutExerciseEntity,
        sets: List<ExerciseSetEntity>,
        equipment: EquipmentEntity?
    ): ProgressionDecision? {
        // Séries extras são histórico real, mas não substituem nem alteram
        // as séries prescritas na decisão automática de progressão.
        val considered = sets
            .filter { it.setIndex in 1..prescription.plannedSets }
            .sortedBy { it.setIndex }
        if (considered.size < prescription.plannedSets) return null
        val currentLoad = considered.last().loadKg
        val sameLoad = considered.all { kotlin.math.abs(it.loadKg - currentLoad) < 0.01 }
        if (!sameLoad) {
            return ProgressionDecision(
                action = ProgressionAction.MAINTAIN,
                currentLoadKg = currentLoad,
                suggestedLoadKg = roundLoad(currentLoad),
                rationale = "As séries usaram cargas diferentes. Mantive a referência atual e evitei sugerir progressão automática a partir de uma sessão não padronizada."
            )
        }
        val allAtTop = considered.all { it.reps >= prescription.repMax }
        val effortAppropriate = considered.all { it.rir in prescription.targetRirMin..prescription.targetRirMax }
        val allBelowRange = considered.all { it.reps < prescription.repMin }
        val allAtLimit = considered.all { it.rir <= 0 }
        val increment = equipment?.defaultLoadIncrementKg ?: max(1.0, currentLoad * 0.025)

        return when {
            allAtTop && effortAppropriate -> ProgressionDecision(
                action = ProgressionAction.INCREASE_LOAD,
                currentLoadKg = currentLoad,
                suggestedLoadKg = roundLoad(currentLoad + increment),
                rationale = "Topo da faixa de repetições atingido em todas as séries mantendo o RIR planejado."
            )
            allBelowRange && allAtLimit -> ProgressionDecision(
                action = ProgressionAction.CONSIDER_REDUCTION,
                currentLoadKg = currentLoad,
                suggestedLoadKg = roundLoad(max(0.0, currentLoad - increment)),
                rationale = "Todas as séries ficaram abaixo da faixa e no limite de esforço. Vale considerar pequena redução de carga antes de insistir."
            )
            else -> ProgressionDecision(
                action = ProgressionAction.MAINTAIN,
                currentLoadKg = currentLoad,
                suggestedLoadKg = roundLoad(currentLoad),
                rationale = "Ainda há espaço para progredir dentro da faixa de repetições antes de alterar a carga."
            )
        }
    }

    private fun roundLoad(value: Double): Double = kotlin.math.round(value * 2.0) / 2.0
}
