package br.com.taina.constantia.engine

import br.com.taina.constantia.core.database.TrainingProfileEntity
import br.com.taina.constantia.core.database.UserProfileEntity
import br.com.taina.constantia.core.model.ExperienceLevel

data class WorkoutExerciseSpec(
    val exerciseCode: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val rirMin: Int,
    val rirMax: Int,
    val restSeconds: Int,
    val priorityScore: Int
)

data class WorkoutTemplateSpec(
    val name: String,
    val estimatedMinutes: Int,
    val exercises: List<WorkoutExerciseSpec>
)

data class TrainingPlanSpec(
    val name: String,
    val goal: String,
    val rationale: String,
    val workouts: List<WorkoutTemplateSpec>
)

/**
 * Motor inicial deliberadamente conservador e determinístico.
 * A IA não escolhe exercícios aqui. A progressão posterior usa os dados reais da usuária.
 */
class TrainingPrescriptionEngine {
    fun generate(user: UserProfileEntity, training: TrainingProfileEntity, hasRestrictions: Boolean): TrainingPlanSpec {
        val days = training.availableDaysPerWeek.coerceIn(2, 5)
        val experience = runCatching { ExperienceLevel.valueOf(training.experienceLevel) }
            .getOrDefault(ExperienceLevel.INTERMEDIATE)
        val compoundSets = when (experience) {
            ExperienceLevel.BEGINNER, ExperienceLevel.RETURNING -> 2
            ExperienceLevel.INTERMEDIATE -> 3
            ExperienceLevel.ADVANCED -> 3
        }
        val accessorySets = when (experience) {
            ExperienceLevel.BEGINNER, ExperienceLevel.RETURNING -> 2
            else -> 2
        }
        val timeCap = training.normalSessionMinutes.coerceIn(30, 120)
        val workouts = when (days) {
            2 -> twoDay(compoundSets, accessorySets, timeCap)
            3 -> threeDay(compoundSets, accessorySets, timeCap)
            4 -> fourDay(compoundSets, accessorySets, timeCap)
            else -> fiveDay(compoundSets, accessorySets, timeCap)
        }
        val restrictionText = if (hasRestrictions) {
            " Há restrições registradas; os exercícios devem ser revisados pela usuária e, quando aplicável, por profissional antes da execução."
        } else ""
        return TrainingPlanSpec(
            name = "Plano inicial ${days}x/semana",
            goal = user.primaryGoal,
            rationale = "Plano inicial baseado em ${days} dias realmente disponíveis, experiência ${training.experienceLevel.lowercase()} e sessões de até ${timeCap} min. O volume começa conservador e só progride após dados reais de execução.$restrictionText",
            workouts = workouts
        )
    }

    private fun twoDay(c: Int, a: Int, cap: Int) = listOf(
        template("Corpo inteiro A", cap, listOf(
            e("LEG_PRESS_45", c, 8, 12, 1, 3, 120, 100),
            e("CHEST_PRESS_MACHINE", c, 8, 12, 1, 3, 120, 95),
            e("LAT_PULLDOWN", c, 8, 12, 1, 3, 120, 95),
            e("SEATED_LEG_CURL", a, 10, 15, 1, 3, 90, 85),
            e("DB_LATERAL_RAISE", a, 10, 15, 1, 3, 75, 65),
            e("CABLE_CRUNCH", a, 10, 15, 2, 3, 60, 50)
        )),
        template("Corpo inteiro B", cap, listOf(
            e("SMITH_SQUAT", c, 8, 12, 1, 3, 120, 100),
            e("INCLINE_DB_PRESS", c, 8, 12, 1, 3, 120, 95),
            e("SEATED_ROW", c, 8, 12, 1, 3, 120, 95),
            e("DB_RDL", a, 8, 12, 1, 3, 120, 85),
            e("CABLE_TRICEPS_PRESSDOWN", a, 10, 15, 1, 3, 75, 60),
            e("SCOTT_CURL", a, 10, 15, 1, 3, 75, 60)
        ))
    )

    private fun threeDay(c: Int, a: Int, cap: Int) = listOf(
        template("Corpo inteiro A — quadríceps", cap, listOf(
            e("LEG_PRESS_45", c, 8, 12, 1, 3, 120, 100),
            e("CHEST_PRESS_MACHINE", c, 8, 12, 1, 3, 120, 90),
            e("LAT_PULLDOWN", c, 8, 12, 1, 3, 120, 90),
            e("LEG_EXTENSION_MACHINE", a, 10, 15, 1, 3, 75, 80),
            e("DB_LATERAL_RAISE", a, 10, 15, 1, 3, 75, 60)
        )),
        template("Corpo inteiro B — posteriores", cap, listOf(
            e("DB_RDL", c, 8, 12, 1, 3, 120, 100),
            e("INCLINE_DB_PRESS", c, 8, 12, 1, 3, 120, 90),
            e("SEATED_ROW", c, 8, 12, 1, 3, 120, 90),
            e("SEATED_LEG_CURL", a, 10, 15, 1, 3, 90, 80),
            e("CABLE_TRICEPS_PRESSDOWN", a, 10, 15, 1, 3, 75, 55),
            e("SCOTT_CURL", a, 10, 15, 1, 3, 75, 55)
        )),
        template("Corpo inteiro C — misto", cap, listOf(
            e("SMITH_SQUAT", c, 8, 12, 1, 3, 120, 100),
            e("PECK_DECK", a, 10, 15, 1, 3, 75, 75),
            e("LAT_PULLDOWN", c, 8, 12, 1, 3, 120, 90),
            e("HIP_ABDUCTOR", a, 12, 20, 1, 3, 75, 65),
            e("SHOULDER_PRESS_MACHINE", a, 8, 12, 1, 3, 90, 65),
            e("CALF_MACHINE", a, 10, 15, 1, 3, 60, 55)
        ))
    )

    private fun fourDay(c: Int, a: Int, cap: Int) = listOf(
        template("Inferiores A", cap, listOf(
            e("LEG_PRESS_45", c, 8, 12, 1, 3, 120, 100),
            e("SEATED_LEG_CURL", c, 8, 12, 1, 3, 90, 90),
            e("LEG_EXTENSION_MACHINE", a, 10, 15, 1, 3, 75, 78),
            e("DB_RDL", a, 8, 12, 1, 3, 120, 82),
            e("CALF_MACHINE", a, 10, 15, 1, 3, 60, 60)
        )),
        template("Superiores A", cap, listOf(
            e("CHEST_PRESS_MACHINE", c, 8, 12, 1, 3, 120, 100),
            e("LAT_PULLDOWN", c, 8, 12, 1, 3, 120, 100),
            e("SEATED_ROW", c, 8, 12, 1, 3, 120, 90),
            e("SHOULDER_PRESS_MACHINE", a, 8, 12, 1, 3, 90, 75),
            e("DB_LATERAL_RAISE", a, 10, 15, 1, 3, 75, 65),
            e("CABLE_TRICEPS_PRESSDOWN", a, 10, 15, 1, 3, 75, 55),
            e("SCOTT_CURL", a, 10, 15, 1, 3, 75, 55)
        )),
        template("Inferiores B", cap, listOf(
            e("SMITH_SQUAT", c, 8, 12, 1, 3, 120, 100),
            e("DB_RDL", c, 8, 12, 1, 3, 120, 92),
            e("LEG_EXTENSION_MACHINE", a, 10, 15, 1, 3, 75, 75),
            e("SEATED_LEG_CURL", a, 10, 15, 1, 3, 90, 80),
            e("HIP_ABDUCTOR", a, 12, 20, 1, 3, 75, 65),
            e("CALF_MACHINE", a, 10, 15, 1, 3, 60, 60)
        )),
        template("Superiores B", cap, listOf(
            e("INCLINE_DB_PRESS", c, 8, 12, 1, 3, 120, 100),
            e("SEATED_ROW", c, 8, 12, 1, 3, 120, 100),
            e("LAT_PULLDOWN", c, 8, 12, 1, 3, 120, 90),
            e("PECK_DECK", a, 10, 15, 1, 3, 75, 70),
            e("DB_LATERAL_RAISE", a, 10, 15, 1, 3, 75, 65),
            e("CABLE_TRICEPS_PRESSDOWN", a, 10, 15, 1, 3, 75, 55),
            e("DB_BICEPS_CURL", a, 10, 15, 1, 3, 75, 55)
        ))
    )

    private fun fiveDay(c: Int, a: Int, cap: Int): List<WorkoutTemplateSpec> =
        fourDay(c, a, cap) + template("Corpo inteiro C — curto", cap, listOf(
            e("LEG_PRESS_45", a, 10, 15, 2, 3, 90, 95),
            e("CHEST_PRESS_MACHINE", a, 10, 15, 2, 3, 90, 85),
            e("LAT_PULLDOWN", a, 10, 15, 2, 3, 90, 85),
            e("SEATED_LEG_CURL", a, 10, 15, 2, 3, 75, 75),
            e("DB_LATERAL_RAISE", a, 12, 20, 2, 3, 60, 55)
        ))

    private fun template(name: String, cap: Int, exercises: List<WorkoutExerciseSpec>): WorkoutTemplateSpec {
        val compact = if (cap < 50 && exercises.size > 5) exercises.take(5) else exercises
        return WorkoutTemplateSpec(name, estimateMinutes(compact).coerceAtMost(cap + 10), compact)
    }

    private fun estimateMinutes(exercises: List<WorkoutExerciseSpec>): Int {
        val seconds = exercises.sumOf { ex ->
            ex.sets * 45 + (ex.sets - 1).coerceAtLeast(0) * ex.restSeconds + 60
        }
        return (seconds / 60.0).toInt().coerceAtLeast(1)
    }

    private fun e(code: String, sets: Int, min: Int, max: Int, rirMin: Int, rirMax: Int, rest: Int, priority: Int) =
        WorkoutExerciseSpec(code, sets, min, max, rirMin, rirMax, rest, priority)
}
