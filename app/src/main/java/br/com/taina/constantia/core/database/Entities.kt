package br.com.taina.constantia.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val age: Int,
    val sex: String,
    val heightCm: Double,
    val initialWeightKg: Double,
    val primaryGoal: String,
    val secondaryGoalNotes: String = "",
    val onboardingCompleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "training_profile")
data class TrainingProfileEntity(
    @PrimaryKey val id: Int = 1,
    val experienceLevel: String,
    val currentTrainingDaysPerWeek: Int,
    val availableDaysPerWeek: Int,
    val normalSessionMinutes: Int,
    val minimumSessionMinutes: Int,
    val preferredTrainingMinuteOfDay: Int?,
    val preferredTrainingDaysCsv: String = "",
    val gymType: String = "SMART_FIT",
    val currentPlanNotes: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "routine_profile")
data class RoutineProfileEntity(
    @PrimaryKey val id: Int = 1,
    val wakeMinuteOfDay: Int?,
    val sleepMinuteOfDay: Int?,
    val workStartMinuteOfDay: Int?,
    val workEndMinuteOfDay: Int?,
    val lunchMinuteOfDay: Int?,
    val preferredStudyMinuteOfDay: Int?,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

/**
 * Restrições estruturadas. Apenas EXERCISE_AVOID e MOVEMENT_AVOID alteram a
 * prescrição automaticamente. GENERAL permanece como informação para revisão,
 * evitando que texto livre seja interpretado como diagnóstico.
 */
@Entity(tableName = "restrictions", indices = [Index("exerciseCode"), Index("movementPattern")])
data class RestrictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val restrictionType: String = "GENERAL",
    val bodyRegion: String = "",
    val description: String,
    val professionalGuidance: String = "",
    val exerciseCode: String? = null,
    val movementPattern: String? = null,
    val temporary: Boolean = false,
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordedAtMillis: Long = System.currentTimeMillis(),
    val weightKg: Double,
    val waistCm: Double? = null,
    val notes: String = ""
)

@Entity(tableName = "activity_definitions")
data class ActivityDefinitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val frequencyType: String,
    val timesPerPeriod: Int = 1,
    val specificDaysCsv: String = "",
    val anchorEpochDay: Long,
    val everyXDays: Int = 1,
    val windowStartMinute: Int? = null,
    val windowEndMinute: Int? = null,
    val expectedDurationMinutes: Int? = null,
    val priority: String,
    val missedPolicy: String,
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "activity_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = ActivityDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("activityId"), Index(value = ["activityId", "scheduledEpochDay"], unique = true)]
)
data class ActivityOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: Long,
    val scheduledEpochDay: Long,
    val status: String,
    val completedAtMillis: Long? = null,
    val failureReason: String = "",
    val notes: String = ""
)
