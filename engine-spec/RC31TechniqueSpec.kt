import br.com.taina.constantia.engine.TechniqueExerciseInput
import br.com.taina.constantia.engine.TrainingTechniqueEngine
import br.com.taina.constantia.engine.TrainingTechniqueType

fun main() {
    val engine = TrainingTechniqueEngine()
    val upper = listOf(
        TechniqueExerciseInput("CHEST_PRESS_MACHINE", "Supino máquina", "HORIZONTAL_PUSH", "CHEST_PRESS", 100),
        TechniqueExerciseInput("LAT_PULLDOWN", "Puxada frontal", "VERTICAL_PULL", "LAT_PULLDOWN", 100),
        TechniqueExerciseInput("DB_LATERAL_RAISE", "Elevação lateral", "SHOULDER_ABDUCTION", "DUMBBELLS", 65),
        TechniqueExerciseInput("FACE_PULL", "Face pull", "SHOULDER_HORIZONTAL_ABDUCTION", "CABLE", 68),
        TechniqueExerciseInput("CABLE_TRICEPS_PRESSDOWN", "Tríceps pulley", "ELBOW_EXTENSION", "CABLE", 55),
        TechniqueExerciseInput("DB_BICEPS_CURL", "Rosca halteres", "ELBOW_FLEXION", "DUMBBELLS", 55)
    )
    val intermediate = engine.suggest(upper, "INTERMEDIATE")
    check(intermediate.any { it.type == TrainingTechniqueType.BI_SET }) { intermediate }
    check(intermediate.any { it.type == TrainingTechniqueType.TRI_SET }) { intermediate }
    check(intermediate.any { it.type == TrainingTechniqueType.DROP_SET }) { intermediate }
    check(intermediate.none { it.exerciseCodes.contains("CHEST_PRESS_MACHINE") && it.type == TrainingTechniqueType.DROP_SET }) { intermediate }

    val beginner = engine.suggest(upper, "BEGINNER")
    check(beginner.none { it.type == TrainingTechniqueType.DROP_SET || it.type == TrainingTechniqueType.REST_PAUSE }) { beginner }

    val timed = engine.suggest(
        listOf(TechniqueExerciseInput("SIDE_PLANK", "Prancha lateral", "CORE_STABILITY", "BODYWEIGHT", 50, timed = true)),
        "INTERMEDIATE"
    )
    check(timed.any { it.type == TrainingTechniqueType.ISOMETRY }) { timed }

    println("Constantia RC3.1 technique engine tests: OK")
}
