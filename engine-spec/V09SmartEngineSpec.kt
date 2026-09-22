import br.com.taina.constantia.core.database.FoodEntity
import br.com.taina.constantia.engine.*

fun main() {
    val foods = listOf(
        FoodEntity(name="Arroz branco cozido, sem óleo", kcalPer100g=131.0, proteinPer100g=2.38, carbsPer100g=30.0, fatPer100g=0.41, defaultMeasureName="colher de sopa cheia", defaultMeasureGrams=20.0),
        FoodEntity(name="Peito de frango grelhado, sem pele e sem óleo", kcalPer100g=149.0, proteinPer100g=31.8, carbsPer100g=0.0, fatPer100g=2.46, defaultMeasureName="filé médio", defaultMeasureGrams=110.0),
        FoodEntity(name="Ovo de galinha cozido", kcalPer100g=125.0, proteinPer100g=10.4, carbsPer100g=1.38, fatPer100g=8.70, defaultMeasureName="unidade média", defaultMeasureGrams=50.0)
    )
    val parsed = SmartMealParser().parse("4 colheres de arroz, 1 filé de frango e 1 ovo", foods)
    check(parsed.items.size == 3) { parsed }
    check(parsed.unmatchedFragments.isEmpty()) { parsed.unmatchedFragments }
    check(parsed.range.minKcal < parsed.range.maxKcal)
    check(parsed.range.minKcal == 281.5) { parsed.range }
    check(parsed.range.maxKcal == 380.9) { parsed.range }

    val notes = "FTIP é a falha na transferência de imunidade passiva em bezerros. O colostro fornece imunoglobulinas importantes ao recém-nascido."
    val questions = StudyQuestionGenerator().generate(notes, 5)
    check(questions.size == 2) { questions }
    check(questions.all { it.answer in notes })

    check(RomanQuoteLibrary.forSeed(0).author.isNotBlank())
    check(RomanQuoteLibrary.forSeed(3).source.isNotBlank())

    val plan = NotificationPlannerEngine().plan(
        NotificationPlanInput(
            lunchMinuteOfDay = 12 * 60 + 30,
            workoutMinuteOfDay = 18 * 60 + 30,
            workoutPending = true,
            preferredStudyMinuteOfDay = 20 * 60,
            studyDue = true,
            dueQuestionPrompts = listOf(1L to "O que significa FTIP?"),
            maxNonEssential = 3
        )
    )
    check(plan.any { it.type == "PRE_WORKOUT" })

    println("Constantia v0.9 smart engine tests: OK")
    println("Meal range: ${parsed.range.minKcal}-${parsed.range.maxKcal} kcal")
    println("Generated questions: ${questions.size}")
}
