package br.com.taina.constantia.engine

import br.com.taina.constantia.core.database.ExerciseEntity
import br.com.taina.constantia.core.database.ExerciseMuscleEntity
import br.com.taina.constantia.core.database.RestrictionEntity

class ExerciseAdaptationEngine {
    fun isBlocked(exercise: ExerciseEntity, restrictions: List<RestrictionEntity>): Boolean =
        restrictions.any { restriction ->
            if (!restriction.active) return@any false
            when (restriction.restrictionType) {
                "EXERCISE_AVOID" -> restriction.exerciseCode == exercise.code
                "MOVEMENT_AVOID" -> restriction.movementPattern == exercise.movementPattern
                else -> false
            }
        }

    fun findSubstitutes(
        source: ExerciseEntity,
        exercises: List<ExerciseEntity>,
        muscleLinks: List<ExerciseMuscleEntity>,
        availableEquipmentCodes: Set<String>,
        restrictions: List<RestrictionEntity>,
        excludedExerciseCodes: Set<String> = emptySet(),
        limit: Int = 4
    ): List<ExerciseEntity> {
        val linksByExercise = muscleLinks.groupBy { it.exerciseCode }
        val sourceMuscles = linksByExercise[source.code].orEmpty().associate { it.muscleCode to it.contribution }
        return exercises.asSequence()
            .filter { it.active && it.code != source.code }
            .filter { it.code !in excludedExerciseCodes }
            .filter { it.equipmentCode in availableEquipmentCodes }
            .filterNot { isBlocked(it, restrictions) }
            .map { candidate -> candidate to score(source, candidate, sourceMuscles, linksByExercise[candidate.code].orEmpty()) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<ExerciseEntity, Int>> { it.second }.thenBy { it.first.name })
            .take(limit)
            .map { it.first }
            .toList()
    }

    private fun score(
        source: ExerciseEntity,
        candidate: ExerciseEntity,
        sourceMuscles: Map<String, Double>,
        candidateLinks: List<ExerciseMuscleEntity>
    ): Int {
        var score = 0
        if (source.movementPattern == candidate.movementPattern) score += 100
        val candidateMuscles = candidateLinks.associate { it.muscleCode to it.contribution }
        sourceMuscles.forEach { (muscle, contribution) ->
            val match = candidateMuscles[muscle] ?: 0.0
            score += (40.0 * minOf(contribution, match)).toInt()
        }
        if (source.equipmentCode == candidate.equipmentCode) score += 10
        return score
    }
}
