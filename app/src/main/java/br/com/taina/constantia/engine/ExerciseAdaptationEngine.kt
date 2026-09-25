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
        excludedEquipmentCodes: Set<String> = emptySet(),
        limit: Int = 8
    ): List<ExerciseEntity> {
        val linksByExercise = muscleLinks.groupBy { it.exerciseCode }
        val sourceLinks = linksByExercise[source.code].orEmpty()
        val ranked = exercises.asSequence()
            .filter { it.active && it.code != source.code }
            .filter { it.code !in excludedExerciseCodes }
            .filter { it.equipmentCode in availableEquipmentCodes }
            .filter { it.equipmentCode !in excludedEquipmentCodes }
            .filterNot { isBlocked(it, restrictions) }
            .filter { candidate ->
                primaryMusclesOverlap(sourceLinks, linksByExercise[candidate.code].orEmpty())
            }
            .map { candidate -> candidate to score(source, candidate, sourceLinks, linksByExercise[candidate.code].orEmpty()) }
            .filter { it.second >= 35 }
            .sortedWith(compareByDescending<Pair<ExerciseEntity, Int>> { it.second }.thenBy { it.first.name })
            .toList()

        // Primeira passada: traz ao menos uma opção de famílias diferentes
        // (máquina, peso livre, peso corporal, polia/faixa) quando existirem.
        val diversified = mutableListOf<Pair<ExerciseEntity, Int>>()
        val seenFamilies = mutableSetOf<String>()
        ranked.forEach { item ->
            val family = equipmentFamily(item.first.equipmentCode)
            if (family !in seenFamilies && diversified.size < limit) {
                diversified += item
                seenFamilies += family
            }
        }
        ranked.forEach { item ->
            if (diversified.size >= limit) return@forEach
            if (item !in diversified) diversified += item
        }
        return diversified.take(limit).map { it.first }
    }

    private fun primaryMusclesOverlap(
        sourceLinks: List<ExerciseMuscleEntity>,
        candidateLinks: List<ExerciseMuscleEntity>
    ): Boolean {
        val sourcePrimary = sourceLinks.filter { it.role == "PRIMARY" }.map { it.muscleCode }.toSet()
        val candidatePrimary = candidateLinks.filter { it.role == "PRIMARY" }.map { it.muscleCode }.toSet()
        return sourcePrimary.isNotEmpty() &&
            candidatePrimary.isNotEmpty() &&
            sourcePrimary.any { it in candidatePrimary }
    }

    private fun score(
        source: ExerciseEntity,
        candidate: ExerciseEntity,
        sourceLinks: List<ExerciseMuscleEntity>,
        candidateLinks: List<ExerciseMuscleEntity>
    ): Int {
        var score = 0
        score += movementScore(source.movementPattern, candidate.movementPattern)

        val candidateByMuscle = candidateLinks.associateBy { it.muscleCode }
        sourceLinks.forEach { sourceLink ->
            val match = candidateByMuscle[sourceLink.muscleCode] ?: return@forEach
            val weight = if (sourceLink.role == "PRIMARY") 60.0 else 30.0
            score += (weight * minOf(sourceLink.contribution, match.contribution)).toInt()
            if (sourceLink.role == "PRIMARY" && match.role == "PRIMARY") score += 15
        }

        // Em substituição por aparelho ocupado, o chamador exclui o equipamento fonte.
        // Fora desse caso, manter o mesmo equipamento recebe somente um bônus pequeno.
        if (source.equipmentCode == candidate.equipmentCode) score += 5
        return score
    }

    private fun equipmentFamily(code: String): String = when (code) {
        "DUMBBELLS", "BARBELL", "EZ_BAR", "KETTLEBELL", "PLATE", "LANDMINE" -> "FREE_WEIGHT"
        "BODYWEIGHT", "PULLUP_BAR", "TRX", "BACK_EXTENSION" -> "BODYWEIGHT"
        "CABLE", "BAND" -> "CABLE_BAND"
        else -> "MACHINE"
    }

    private fun movementScore(a: String, b: String): Int {
        if (a == b) return 120
        val groups = listOf(
            setOf("SQUAT", "KNEE_HIP_EXTENSION", "LUNGE", "ISOMETRIC_LOWER"),
            setOf("HIP_HINGE", "HIP_EXTENSION"),
            setOf("HORIZONTAL_PUSH", "INCLINE_PUSH", "HORIZONTAL_ADDUCTION"),
            setOf("VERTICAL_PULL", "SHOULDER_EXTENSION", "SCAPULAR_DEPRESSION"),
            setOf("HORIZONTAL_PULL", "SCAPULAR_CONTROL", "SHOULDER_HORIZONTAL_ABDUCTION"),
            setOf("TRUNK_FLEXION", "CORE_STABILITY", "CORE_DYNAMIC", "ANTI_ROTATION", "TRUNK_ROTATION", "LATERAL_FLEXION", "HIP_FLEXION_CORE"),
            setOf("SHOULDER_ABDUCTION", "SHOULDER_FLEXION", "SHOULDER_HORIZONTAL_ABDUCTION"),
            setOf("WRIST_FLEXION", "WRIST_EXTENSION", "GRIP_ISOMETRIC"),
            setOf("HIP_ADDUCTION", "ISOMETRIC_ADDUCTION")
        )
        if (groups.any { a in it && b in it }) return 70
        if (setOf(a, b) == setOf("VERTICAL_PULL", "HORIZONTAL_PULL")) return 35
        if (setOf(a, b).all { it in setOf("SQUAT", "KNEE_EXTENSION", "KNEE_HIP_EXTENSION", "LUNGE") }) return 30
        return 0
    }
}
