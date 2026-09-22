package br.com.taina.constantia.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Alimento de referência local. Os valores nutricionais ficam normalizados por 100 g.
 * sourceCode/sourceLabel permitem rastrear a origem (ex.: TBCA) sem depender de rede
 * durante o uso cotidiano do app.
 */
@Entity(tableName = "foods", indices = [Index(value = ["name"], unique = true)])
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val defaultMeasureName: String = "g",
    val defaultMeasureGrams: Double = 1.0,
    val sourceCode: String = "CUSTOM",
    val sourceLabel: String = "Personalizado",
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "nutrition_goals")
data class NutritionGoalEntity(
    @PrimaryKey val id: Int = 1,
    val dailyCalories: Int? = null,
    val dailyProteinGrams: Int? = null,
    val dailyCarbsGrams: Int? = null,
    val dailyFatGrams: Int? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "meals", indices = [Index("epochDay")])
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val mealType: String,
    val minuteOfDay: Int? = null,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * Snapshot nutricional: mesmo que um alimento seja editado depois, refeições antigas
 * preservam o valor usado no momento do registro.
 *
 * confidence: HIGH para peso em gramas/porção conhecida, MEDIUM para medida caseira,
 * LOW para estimativa livre.
 */
@Entity(
    tableName = "food_entries",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealId"), Index("foodId")]
)
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val foodId: Long? = null,
    val foodNameSnapshot: String,
    val amountValue: Double,
    val amountUnit: String,
    val estimatedGrams: Double,
    val kcal: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val confidence: String,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)
