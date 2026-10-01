package br.com.taina.constantia.engine

import br.com.taina.constantia.core.database.RestrictionEntity

enum class CircuitEquipment { BODYWEIGHT, DUMBBELLS, KETTLEBELL, SANDBAG }
enum class CircuitPurpose { CARDIO_REPLACEMENT, QUICK_EXTRA }

data class CircuitMove(
    val name: String,
    val equipment: CircuitEquipment,
    val movementPattern: String,
    val cue: String,
    val exerciseCode: String? = null
)

data class CircuitSuggestion(
    val title: String,
    val purpose: CircuitPurpose,
    val minutes: Int,
    val rounds: Int,
    val workSeconds: Int,
    val transitionSeconds: Int,
    val moves: List<CircuitMove>,
    val rationale: String
)

class CircuitWorkoutEngine {
    private val catalog = listOf(
        CircuitMove("Agachamento com peso corporal", CircuitEquipment.BODYWEIGHT, "SQUAT", "Ritmo controlado; pare antes de perder a técnica.", "BODYWEIGHT_SQUAT"),
        CircuitMove("Flexão de braços", CircuitEquipment.BODYWEIGHT, "HORIZONTAL_PUSH", "Use apoio elevado se precisar reduzir a dificuldade.", "PUSH_UP"),
        CircuitMove("Ponte de glúteos", CircuitEquipment.BODYWEIGHT, "HIP_EXTENSION", "Contraia glúteos sem hiperestender a lombar.", "BODYWEIGHT_GLUTE_BRIDGE"),
        CircuitMove("Dead bug", CircuitEquipment.BODYWEIGHT, "CORE_STABILITY", "Mantenha lombar estável e respiração contínua.", "DEAD_BUG"),
        CircuitMove("Agachamento goblet com halter", CircuitEquipment.DUMBBELLS, "SQUAT", "Carga moderada e amplitude confortável.", "GOBLET_SQUAT"),
        CircuitMove("Levantamento romeno com halteres", CircuitEquipment.DUMBBELLS, "HIP_HINGE", "Quadril para trás e coluna neutra.", "DB_RDL"),
        CircuitMove("Remada unilateral com halter", CircuitEquipment.DUMBBELLS, "HORIZONTAL_PULL", "Evite girar o tronco.", "ONE_ARM_DB_ROW"),
        CircuitMove("Supino no chão com halteres", CircuitEquipment.DUMBBELLS, "HORIZONTAL_PUSH", "Controle a descida e mantenha ombros estáveis.", "DB_FLOOR_PRESS"),
        CircuitMove("Agachamento frontal com kettlebell", CircuitEquipment.KETTLEBELL, "SQUAT", "Segure a carga próxima ao corpo.", "DS_0533"),
        CircuitMove("Levantamento terra com kettlebell", CircuitEquipment.KETTLEBELL, "HIP_HINGE", "Empurre o chão e mantenha a carga próxima.", "KETTLEBELL_DEADLIFT"),
        CircuitMove("Remada bilateral com kettlebells", CircuitEquipment.KETTLEBELL, "HORIZONTAL_PULL", "Tronco firme e cotovelos controlados.", "DS_1345"),
        CircuitMove("Floor press unilateral com kettlebell", CircuitEquipment.KETTLEBELL, "HORIZONTAL_PUSH", "Punho neutro e controle do ombro.", "DS_1298"),
        CircuitMove("Agachamento frontal com bolsa de peso", CircuitEquipment.SANDBAG, "SQUAT", "Abrace a bolsa junto ao tronco e use carga confortável."),
        CircuitMove("Levantamento terra com bolsa de peso", CircuitEquipment.SANDBAG, "HIP_HINGE", "Quadril para trás; não arredonde a lombar."),
        CircuitMove("Carregada abraçada com bolsa de peso", CircuitEquipment.SANDBAG, "LOADED_CARRY", "Passos curtos, tronco alto e respiração contínua."),
        CircuitMove("Remada inclinada com bolsa de peso", CircuitEquipment.SANDBAG, "HORIZONTAL_PULL", "Segure a bolsa firme e evite balanço.")
    )

    fun build(
        minutes: Int,
        equipment: Set<CircuitEquipment>,
        purpose: CircuitPurpose,
        restrictions: List<RestrictionEntity>
    ): CircuitSuggestion {
        val safeMinutes = minutes.coerceIn(8, 30)
        val allowedEquipment = equipment.ifEmpty { setOf(CircuitEquipment.BODYWEIGHT) }
        val blockedPatterns = restrictions
            .filter { it.active && it.restrictionType == "MOVEMENT_AVOID" }
            .mapNotNull { it.movementPattern }
            .toSet()
        val exerciseRestrictions = restrictions.filter { it.active && it.restrictionType == "EXERCISE_AVOID" }
        val blockedExerciseCodes = exerciseRestrictions.mapNotNull { it.exerciseCode }.toSet()
        val blockedExerciseNames = exerciseRestrictions.map { it.description.lowercase() }

        val candidates = catalog.filter { move ->
            move.equipment in allowedEquipment &&
                move.movementPattern !in blockedPatterns &&
                (move.exerciseCode == null || move.exerciseCode !in blockedExerciseCodes) &&
                blockedExerciseNames.none { blocked -> blocked.isNotBlank() && move.name.lowercase().contains(blocked) }
        }
        val fallback = catalog.filter { it.equipment == CircuitEquipment.BODYWEIGHT && it.movementPattern !in blockedPatterns }
        val pool = (candidates.ifEmpty { fallback }).distinctBy { it.name }

        val selected = selectBalanced(pool, if (safeMinutes <= 12) 4 else 5)
        val work = if (purpose == CircuitPurpose.CARDIO_REPLACEMENT) 35 else 40
        val transition = if (purpose == CircuitPurpose.CARDIO_REPLACEMENT) 25 else 35
        val roundSeconds = selected.size.coerceAtLeast(1) * (work + transition)
        val rounds = ((safeMinutes * 60) / roundSeconds).coerceIn(2, 6)
        val title = if (purpose == CircuitPurpose.CARDIO_REPLACEMENT) "Circuito no lugar do cardio" else "Circuito rápido opcional"
        return CircuitSuggestion(
            title = title,
            purpose = purpose,
            minutes = safeMinutes,
            rounds = rounds,
            workSeconds = work,
            transitionSeconds = transition,
            moves = selected,
            rationale = if (purpose == CircuitPurpose.CARDIO_REPLACEMENT) {
                "Circuito moderado e separado da progressão da ficha. Use apenas quando preferir trocar o cardio opcional por trabalho geral."
            } else {
                "Sessão extra curta e voluntária. Não substitui automaticamente o treino de força programado nem altera a progressão."
            }
        )
    }

    private fun selectBalanced(pool: List<CircuitMove>, count: Int): List<CircuitMove> {
        val preferredOrder = listOf("SQUAT", "HIP_HINGE", "HORIZONTAL_PUSH", "HORIZONTAL_PULL", "CORE_STABILITY", "LOADED_CARRY", "HIP_EXTENSION")
        val result = mutableListOf<CircuitMove>()
        preferredOrder.forEach { pattern ->
            pool.firstOrNull { it.movementPattern == pattern && it !in result }?.let(result::add)
        }
        pool.filter { it !in result }.forEach { if (result.size < count) result += it }
        return result.take(count)
    }
}
