package br.com.taina.constantia.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {
    @Query("SELECT COUNT(*) FROM foods")
    suspend fun foodCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoods(items: List<FoodEntity>)

    @Insert
    suspend fun insertFood(item: FoodEntity): Long

    @Query("SELECT * FROM foods WHERE active = 1 ORDER BY name")
    fun observeFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE active = 1 AND name LIKE '%' || :query || '%' ORDER BY name LIMIT :limit")
    suspend fun searchFoods(query: String, limit: Int = 30): List<FoodEntity>

    @Query("SELECT * FROM foods WHERE id = :id LIMIT 1")
    suspend fun getFood(id: Long): FoodEntity?

    @Upsert
    suspend fun upsertGoal(goal: NutritionGoalEntity)

    @Query("SELECT * FROM nutrition_goals WHERE id = 1")
    fun observeGoal(): Flow<NutritionGoalEntity?>

    @Query("SELECT * FROM nutrition_goals WHERE id = 1")
    suspend fun getGoal(): NutritionGoalEntity?

    @Insert
    suspend fun insertMeal(item: MealEntity): Long

    @Query("SELECT * FROM meals WHERE epochDay = :epochDay AND mealType = :mealType ORDER BY createdAtMillis DESC LIMIT 1")
    suspend fun findMeal(epochDay: Long, mealType: String): MealEntity?

    @Query("SELECT * FROM meals WHERE epochDay = :epochDay ORDER BY COALESCE(minuteOfDay, 9999), createdAtMillis")
    fun observeMealsForDay(epochDay: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY epochDay, createdAtMillis")
    suspend fun getMealsBetween(startEpochDay: Long, endEpochDay: Long): List<MealEntity>

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMeal(mealId: Long)

    @Insert
    suspend fun insertFoodEntry(item: FoodEntryEntity): Long

    @Query("SELECT * FROM food_entries WHERE mealId = :mealId ORDER BY createdAtMillis")
    suspend fun getEntriesForMeal(mealId: Long): List<FoodEntryEntity>

    @Query("SELECT fe.* FROM food_entries fe JOIN meals m ON m.id = fe.mealId WHERE m.epochDay = :epochDay ORDER BY m.createdAtMillis, fe.createdAtMillis")
    fun observeEntriesForDay(epochDay: Long): Flow<List<FoodEntryEntity>>

    @Query("SELECT fe.* FROM food_entries fe JOIN meals m ON m.id = fe.mealId WHERE m.epochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY m.epochDay, fe.createdAtMillis")
    suspend fun getEntriesBetween(startEpochDay: Long, endEpochDay: Long): List<FoodEntryEntity>
}
