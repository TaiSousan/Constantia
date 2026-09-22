package br.com.taina.constantia.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "muscles")
data class MuscleEntity(
    @PrimaryKey val code: String,
    val name: String,
    val region: String
)

@Entity(tableName = "equipment")
data class EquipmentEntity(
    @PrimaryKey val code: String,
    val name: String,
    val category: String,
    val commonAtSmartFit: Boolean = true,
    val defaultLoadIncrementKg: Double? = null
)

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["code"],
            childColumns = ["equipmentCode"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("equipmentCode"), Index(value = ["slug"], unique = true)]
)
data class ExerciseEntity(
    @PrimaryKey val code: String,
    val slug: String,
    val name: String,
    val equipmentCode: String,
    val movementPattern: String,
    val instructions: String,
    val commonErrors: String,
    val demoUrl: String? = null,
    val estimatedSetSeconds: Int = 45,
    val active: Boolean = true
)

@Entity(
    tableName = "exercise_muscles",
    primaryKeys = ["exerciseCode", "muscleCode"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["code"],
            childColumns = ["exerciseCode"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MuscleEntity::class,
            parentColumns = ["code"],
            childColumns = ["muscleCode"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("exerciseCode"), Index("muscleCode")]
)
data class ExerciseMuscleEntity(
    val exerciseCode: String,
    val muscleCode: String,
    val contribution: Double,
    val role: String
)

@Entity(tableName = "training_plans")
data class TrainingPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val goal: String,
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val rationale: String = ""
)

@Entity(
    tableName = "workout_templates",
    foreignKeys = [
        ForeignKey(
            entity = TrainingPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class WorkoutTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val name: String,
    val orderIndex: Int,
    val estimatedMinutes: Int
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["code"],
            childColumns = ["exerciseCode"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("templateId"), Index("exerciseCode")]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseCode: String,
    val orderIndex: Int,
    val plannedSets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRirMin: Int,
    val targetRirMax: Int,
    val restSeconds: Int,
    val priorityScore: Int,
    val targetLoadKg: Double? = null
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("templateId"), Index("startedAtMillis")]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val mode: String,
    val availableMinutes: Int?,
    val startedAtMillis: Long = System.currentTimeMillis(),
    val finishedAtMillis: Long? = null,
    val notes: String = ""
)

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["code"],
            childColumns = ["exerciseCode"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("sessionId"), Index("workoutExerciseId"), Index("exerciseCode"), Index(value = ["sessionId", "workoutExerciseId", "setIndex"], unique = true)]
)
data class ExerciseSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val workoutExerciseId: Long,
    val exerciseCode: String,
    val setIndex: Int,
    val loadKg: Double,
    val reps: Int,
    val rir: Int,
    val completedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "progression_suggestions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutExerciseId"), Index("createdAtMillis")]
)
data class ProgressionSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val exerciseCode: String,
    val action: String,
    val currentLoadKg: Double,
    val suggestedLoadKg: Double,
    val rationale: String,
    val status: String = "PENDING",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "equipment_availability",
    foreignKeys = [
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["code"],
            childColumns = ["equipmentCode"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("equipmentCode")]
)
data class EquipmentAvailabilityEntity(
    @PrimaryKey val equipmentCode: String,
    val available: Boolean,
    val note: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "workout_substitutions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("sessionId"), Index("workoutExerciseId")]
)
data class WorkoutSubstitutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val workoutExerciseId: Long,
    val plannedExerciseCode: String,
    val executedExerciseCode: String,
    val reason: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
