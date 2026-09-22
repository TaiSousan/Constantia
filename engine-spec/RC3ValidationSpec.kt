import br.com.taina.constantia.core.database.ExerciseEntity
import br.com.taina.constantia.core.database.ExerciseMuscleEntity
import br.com.taina.constantia.core.database.FoodEntity
import br.com.taina.constantia.core.database.TrainingProfileEntity
import br.com.taina.constantia.core.database.UserProfileEntity
import br.com.taina.constantia.engine.ExerciseAdaptationEngine
import br.com.taina.constantia.engine.SmartMealParser
import br.com.taina.constantia.engine.TrainingScheduleEngine
import br.com.taina.constantia.engine.TrainingPrescriptionEngine
import java.time.DayOfWeek

fun main() {
    val foods = listOf(
        FoodEntity(name="Ovo de galinha cozido", kcalPer100g=125.0, proteinPer100g=10.4, carbsPer100g=1.38, fatPer100g=8.7, defaultMeasureName="unidade média", defaultMeasureGrams=50.0),
        FoodEntity(name="Tapioca sem manteiga e sem recheio", kcalPer100g=289.0, proteinPer100g=0.36, carbsPer100g=71.9, fatPer100g=0.0, defaultMeasureName="colher de sopa de goma", defaultMeasureGrams=15.0),
        FoodEntity(name="Queijo prato", kcalPer100g=346.0, proteinPer100g=24.2, carbsPer100g=0.2, fatPer100g=27.6, defaultMeasureName="fatia média", defaultMeasureGrams=20.0),
        FoodEntity(name="Banana prata", kcalPer100g=107.0, proteinPer100g=1.11, carbsPer100g=25.9, fatPer100g=0.28, defaultMeasureName="unidade/fatia média", defaultMeasureGrams=40.0),
        FoodEntity(name="Café preto, infusão 10%, sem açúcar", kcalPer100g=10.0, proteinPer100g=0.67, carbsPer100g=1.68, fatPer100g=0.07, defaultMeasureName="xícara de chá", defaultMeasureGrams=200.0),
        FoodEntity(name="Açúcar cristal", kcalPer100g=400.0, proteinPer100g=0.32, carbsPer100g=99.6, fatPer100g=0.0, defaultMeasureName="colher de chá cheia", defaultMeasureGrams=5.0)
    )
    val meal = SmartMealParser().parse(
        "crepioca com dois ovos, uma colher de sopa de goma de tapioca, 1 fatia de queijo prato, 1 banana prata, 1 xícara de 250ml de café preto com 10g de açúcar cristal",
        foods
    )
    check(meal.items.size == 6) { meal }
    check(meal.unmatchedFragments.isEmpty()) { meal.unmatchedFragments }
    check(meal.items.any { it.food.name == "Queijo prato" })
    check(meal.items.any { it.food.name.startsWith("Café preto") && it.unitMode == "ML" && it.amount == 250.0 })
    check(meal.items.any { it.food.name == "Açúcar cristal" && it.estimatedGrams == 10.0 })

    val plan = TrainingPrescriptionEngine().generate(
        UserProfileEntity(primaryGoal = "Perder gordura"),
        TrainingProfileEntity(availableDaysPerWeek = 6, experienceLevel = "INTERMEDIATE", normalSessionMinutes = 70),
        hasRestrictions = false
    )
    check(plan.workouts.size == 5) { plan.workouts }
    check(plan.workouts.all { it.estimatedMinutes <= 70 }) { plan.workouts.map { it.estimatedMinutes } }
    check(plan.workouts.take(4).all { it.estimatedMinutes >= 45 }) { plan.workouts.map { it.estimatedMinutes } }

    val scheduled = TrainingScheduleEngine().resolveScheduledDays(5, "1,2,3,4,5,7")
    check(scheduled.size == 5) { scheduled }
    check(DayOfWeek.SUNDAY in scheduled) { scheduled }
    // Com 5 sessões em 7 dias, uma sequência de 3 pode ser inevitável;
    // o seletor não deve criar um bloco de 4 quando há alternativa melhor.
    val selected = scheduled.map { it.value }.toSet()
    val longestRun = (1..7).filter { it in selected }.maxOf { start ->
        var day = start
        var run = 1
        while (run < selected.size) {
            day = if (day == 7) 1 else day + 1
            if (day !in selected) break
            run += 1
        }
        run
    }
    check(longestRun <= 3) { "Sequência longa demais: $scheduled" }

    val source = ExerciseEntity(code="CHEST_PRESS_MACHINE", name="Supino máquina", equipmentCode="CHEST_PRESS", movementPattern="HORIZONTAL_PUSH")
    val candidates = listOf(
        source,
        ExerciseEntity(code="DB_FLAT_PRESS", name="Supino reto com halteres", equipmentCode="DUMBBELLS", movementPattern="HORIZONTAL_PUSH"),
        ExerciseEntity(code="PUSH_UP", name="Flexão", equipmentCode="BODYWEIGHT", movementPattern="HORIZONTAL_PUSH"),
        ExerciseEntity(code="PECK_DECK", name="Voador", equipmentCode="PECK_DECK", movementPattern="HORIZONTAL_ADDUCTION")
    )
    val links = listOf(
        ExerciseMuscleEntity("CHEST_PRESS_MACHINE", "CHEST", 1.0, "PRIMARY"),
        ExerciseMuscleEntity("DB_FLAT_PRESS", "CHEST", 1.0, "PRIMARY"),
        ExerciseMuscleEntity("PUSH_UP", "CHEST", 1.0, "PRIMARY"),
        ExerciseMuscleEntity("PECK_DECK", "CHEST", 1.0, "PRIMARY")
    )
    val subs = ExerciseAdaptationEngine().findSubstitutes(
        source = source,
        exercises = candidates,
        muscleLinks = links,
        availableEquipmentCodes = setOf("CHEST_PRESS", "DUMBBELLS", "BODYWEIGHT", "PECK_DECK"),
        restrictions = emptyList(),
        excludedEquipmentCodes = setOf("CHEST_PRESS")
    )
    check(subs.none { it.equipmentCode == "CHEST_PRESS" }) { subs }
    check(subs.take(2).map { it.code }.toSet() == setOf("DB_FLAT_PRESS", "PUSH_UP")) { subs }

    println("Constantia RC3 validation tests: OK")
    println("Meal items: ${meal.items.map { it.food.name }}")
    println("Workout estimates: ${plan.workouts.map { it.estimatedMinutes }}")
    println("Scheduled days: $scheduled")
    println("Substitutes: ${subs.map { it.name }}")
}
