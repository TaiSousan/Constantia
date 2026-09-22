package br.com.taina.constantia.engine

import br.com.taina.constantia.core.database.FoodEntity
import br.com.taina.constantia.core.database.FoodEntryEntity
import kotlin.math.round

enum class NutritionConfidence { HIGH, MEDIUM, LOW }

data class NutritionEstimate(
    val grams: Double,
    val kcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val confidence: NutritionConfidence
)

data class NutritionTotals(
    val kcal: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0
)

object NutritionEngine {
    fun estimateFromGrams(food: FoodEntity, grams: Double): NutritionEstimate = estimate(
        food = food,
        grams = grams,
        confidence = NutritionConfidence.HIGH
    )

    fun estimateFromMeasure(food: FoodEntity, measureCount: Double): NutritionEstimate = estimate(
        food = food,
        grams = measureCount * food.defaultMeasureGrams,
        confidence = NutritionConfidence.MEDIUM
    )

    fun estimateFree(food: FoodEntity, estimatedGrams: Double): NutritionEstimate = estimate(
        food = food,
        grams = estimatedGrams,
        confidence = NutritionConfidence.LOW
    )

    private fun estimate(food: FoodEntity, grams: Double, confidence: NutritionConfidence): NutritionEstimate {
        val safeGrams = grams.coerceIn(0.0, 5000.0)
        val factor = safeGrams / 100.0
        return NutritionEstimate(
            grams = round1(safeGrams),
            kcal = round1(food.kcalPer100g * factor),
            proteinGrams = round1(food.proteinPer100g * factor),
            carbsGrams = round1(food.carbsPer100g * factor),
            fatGrams = round1(food.fatPer100g * factor),
            confidence = confidence
        )
    }

    fun totals(entries: List<FoodEntryEntity>): NutritionTotals = NutritionTotals(
        kcal = round1(entries.sumOf { it.kcal }),
        proteinGrams = round1(entries.sumOf { it.proteinGrams }),
        carbsGrams = round1(entries.sumOf { it.carbsGrams }),
        fatGrams = round1(entries.sumOf { it.fatGrams })
    )

    fun movingAverage7Days(weightPoints: List<Pair<Long, Double>>, nowMillis: Long = System.currentTimeMillis()): Double? {
        val cutoff = nowMillis - 7L * 24L * 60L * 60L * 1000L
        val values = weightPoints
            .filter { (recordedAt, weight) -> recordedAt >= cutoff && recordedAt <= nowMillis && weight > 0.0 }
            .map { it.second }
        if (values.isEmpty()) return null
        return round1(values.average())
    }

    private fun round1(value: Double): Double = round(value * 10.0) / 10.0
}
