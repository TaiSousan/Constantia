package br.com.taina.constantia.core.repository

import br.com.taina.constantia.core.database.*
import br.com.taina.constantia.engine.NutritionConfidence
import br.com.taina.constantia.engine.NutritionEngine
import br.com.taina.constantia.engine.NutritionTotals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId

class NutritionRepository(
    private val nutritionDao: NutritionDao,
    private val profileDao: ProfileDao
) {
    suspend fun ensureReady() {
        // ON CONFLICT IGNORE + índice único por nome permite adicionar novos itens
        // do catálogo sem duplicar os que a instalação já possuía.
        nutritionDao.insertFoods(NutritionCatalog.starterFoods)
    }

    fun observeFoods(): Flow<List<FoodEntity>> = nutritionDao.observeFoods()
    fun observeGoal(): Flow<NutritionGoalEntity?> = nutritionDao.observeGoal()

    fun observeDay(date: LocalDate): Flow<DayNutrition> {
        val epoch = date.toEpochDay()
        return combine(
            nutritionDao.observeMealsForDay(epoch),
            nutritionDao.observeEntriesForDay(epoch),
            nutritionDao.observeGoal()
        ) { meals, entries, goal ->
            val groups = meals.map { meal ->
                MealGroup(meal, entries.filter { it.mealId == meal.id })
            }.filter { it.entries.isNotEmpty() }
            DayNutrition(meals, entries, groups, NutritionEngine.totals(entries), goal)
        }
    }

    suspend fun setGoal(calories: Int?, protein: Int?, carbs: Int?, fat: Int?) {
        nutritionDao.upsertGoal(
            NutritionGoalEntity(
                dailyCalories = calories?.takeIf { it > 0 },
                dailyProteinGrams = protein?.takeIf { it > 0 },
                dailyCarbsGrams = carbs?.takeIf { it > 0 },
                dailyFatGrams = fat?.takeIf { it > 0 }
            )
        )
    }

    suspend fun addCustomFood(
        name: String,
        kcalPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        measureName: String,
        measureGrams: Double
    ) = nutritionDao.insertFood(
        FoodEntity(
            name = name.trim(),
            kcalPer100g = kcalPer100g.coerceAtLeast(0.0),
            proteinPer100g = proteinPer100g.coerceAtLeast(0.0),
            carbsPer100g = carbsPer100g.coerceAtLeast(0.0),
            fatPer100g = fatPer100g.coerceAtLeast(0.0),
            defaultMeasureName = measureName.ifBlank { "porção" },
            defaultMeasureGrams = measureGrams.coerceAtLeast(1.0),
            sourceCode = "CUSTOM",
            sourceLabel = "Personalizado"
        )
    )

    suspend fun addFoodToMeal(
        date: LocalDate,
        mealType: String,
        food: FoodEntity,
        amount: Double,
        unitMode: String,
        notes: String = ""
    ) {
        val epochDay = date.toEpochDay()
        val mealId = nutritionDao.findMeal(epochDay, mealType)?.id ?: nutritionDao.insertMeal(
            MealEntity(
                epochDay = epochDay,
                mealType = mealType,
                minuteOfDay = currentMinuteOfDay()
            )
        )
        val estimate = when (unitMode) {
            "GRAMS", "ML" -> NutritionEngine.estimateFromGrams(food, amount)
            "MEASURE" -> NutritionEngine.estimateFromMeasure(food, amount)
            else -> NutritionEngine.estimateFree(food, amount)
        }
        nutritionDao.insertFoodEntry(
            FoodEntryEntity(
                mealId = mealId,
                foodId = food.id.takeIf { it != 0L },
                foodNameSnapshot = food.name,
                amountValue = amount,
                amountUnit = when (unitMode) {
                    "GRAMS" -> "g"
                    "ML" -> "ml"
                    "MEASURE" -> food.defaultMeasureName
                    else -> "g estimados"
                },
                estimatedGrams = estimate.grams,
                kcal = estimate.kcal,
                proteinGrams = estimate.proteinGrams,
                carbsGrams = estimate.carbsGrams,
                fatGrams = estimate.fatGrams,
                confidence = estimate.confidence.name,
                notes = notes
            )
        )
    }

    suspend fun deleteMeal(mealId: Long) = nutritionDao.deleteMeal(mealId)

    suspend fun addWeight(weightKg: Double, notes: String = "") {
        if (weightKg <= 0.0) return
        profileDao.insertBodyMetric(BodyMetricEntity(weightKg = weightKg, notes = notes))
    }

    fun observeBodyMetrics(): Flow<List<BodyMetricEntity>> = profileDao.observeBodyMetrics()

    suspend fun getWeightTrend(): WeightTrend {
        val recent = profileDao.getRecentBodyMetrics(30)
        val newest = recent.firstOrNull()?.weightKg
        val avg7 = NutritionEngine.movingAverage7Days(recent.map { it.recordedAtMillis to it.weightKg })
        return WeightTrend(newest, avg7, recent)
    }

    private fun currentMinuteOfDay(): Int {
        val now = java.time.LocalTime.now(ZoneId.systemDefault())
        return now.hour * 60 + now.minute
    }
}

data class MealGroup(
    val meal: MealEntity,
    val entries: List<FoodEntryEntity>
)

data class DayNutrition(
    val meals: List<MealEntity>,
    val entries: List<FoodEntryEntity>,
    val mealGroups: List<MealGroup>,
    val totals: NutritionTotals,
    val goal: NutritionGoalEntity?
)

data class WeightTrend(
    val currentWeightKg: Double?,
    val sevenDayAverageKg: Double?,
    val recent: List<BodyMetricEntity>
)
