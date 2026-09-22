package br.com.taina.constantia.core.database

data class FoodEntity(
    val id: Long = 0,
    val name: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val defaultMeasureName: String,
    val defaultMeasureGrams: Double,
    val sourceCode: String = "TEST",
    val sourceLabel: String = "TEST",
    val active: Boolean = true,
    val createdAtMillis: Long = 0
)

data class FoodEntryEntity(
    val kcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double
)
