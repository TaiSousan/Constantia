package br.com.taina.constantia.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        TrainingProfileEntity::class,
        RoutineProfileEntity::class,
        RestrictionEntity::class,
        BodyMetricEntity::class,
        ActivityDefinitionEntity::class,
        ActivityOccurrenceEntity::class,
        MuscleEntity::class,
        EquipmentEntity::class,
        ExerciseEntity::class,
        ExerciseMuscleEntity::class,
        TrainingPlanEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSessionEntity::class,
        ExerciseSetEntity::class,
        ProgressionSuggestionEntity::class,
        EquipmentAvailabilityEntity::class,
        WorkoutSubstitutionEntity::class,
        SubjectEntity::class,
        StudyTopicEntity::class,
        StudyGoalEntity::class,
        StudySessionEntity::class,
        ReviewQuestionEntity::class,
        ReviewAttemptEntity::class,
        FocusSessionEntity::class,
        NotificationEventEntity::class,
        FocusGateRuleEntity::class,
        FocusGateOverrideEntity::class,
        FoodEntity::class,
        NutritionGoalEntity::class,
        MealEntity::class,
        FoodEntryEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun activityDao(): ActivityDao
    abstract fun trainingDao(): TrainingDao
    abstract fun studyDao(): StudyDao
    abstract fun contextualDao(): ContextualDao
    abstract fun nutritionDao(): NutritionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "constantia.db"
            ).addMigrations(*DatabaseMigrations.ALL).build().also { INSTANCE = it }
        }
    }
}
