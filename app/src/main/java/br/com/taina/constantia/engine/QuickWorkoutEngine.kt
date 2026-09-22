package br.com.taina.constantia.engine

import br.com.taina.constantia.core.database.ExerciseEntity
import br.com.taina.constantia.core.database.WorkoutExerciseEntity
import br.com.taina.constantia.core.model.QuickExerciseSelection

class QuickWorkoutEngine {
    fun build(
        planned: List<WorkoutExerciseEntity>,
        exercises: Map<String, ExerciseEntity>,
        availableMinutes: Int
    ): List<QuickExerciseSelection> {
        if (planned.isEmpty()) return emptyList()
        val budget = availableMinutes.coerceAtLeast(10) * 60
        var used = 0
        val selected = linkedMapOf<Long, Int>()

        // Primeiro garante as séries essenciais dos exercícios de maior prioridade.
        planned.sortedByDescending { it.priorityScore }.forEach { item ->
            val minimumSets = minOf(2, item.plannedSets)
            val cost = costSeconds(item, exercises[item.exerciseCode], minimumSets)
            if (used + cost <= budget || selected.isEmpty()) {
                selected[item.id] = minimumSets
                used += cost
            }
        }

        // Com o tempo restante, devolve séries removidas começando pelas maiores prioridades.
        planned.sortedByDescending { it.priorityScore }.forEach { item ->
            var current = selected[item.id] ?: return@forEach
            while (current < item.plannedSets) {
                val extraCost = incrementalSetCost(item, exercises[item.exerciseCode], current)
                if (used + extraCost > budget) break
                current += 1
                selected[item.id] = current
                used += extraCost
            }
        }

        return planned.mapNotNull { item -> selected[item.id]?.let { QuickExerciseSelection(item.id, it) } }
    }

    private fun costSeconds(item: WorkoutExerciseEntity, exercise: ExerciseEntity?, sets: Int): Int {
        val work = (exercise?.estimatedSetSeconds ?: 45) * sets
        val rest = (sets - 1).coerceAtLeast(0) * item.restSeconds
        return work + rest + 45 // transição/preparação mínima
    }

    private fun incrementalSetCost(item: WorkoutExerciseEntity, exercise: ExerciseEntity?, currentSets: Int): Int =
        (exercise?.estimatedSetSeconds ?: 45) + if (currentSets >= 1) item.restSeconds else 0
}
