package br.com.taina.constantia.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "study_topics",
    foreignKeys = [ForeignKey(
        entity = SubjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("subjectId")]
)
data class StudyTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val name: String,
    val notes: String = "",
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "study_goals",
    foreignKeys = [ForeignKey(
        entity = SubjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("subjectId")]
)
data class StudyGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val topicId: Long? = null,
    val sessionsPerWeek: Int = 3,
    val targetMinutesPerSession: Int = 25,
    val preferredMinuteOfDay: Int? = null,
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudyTopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("subjectId"), Index("topicId"), Index("startedAtMillis")]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val topicId: Long? = null,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val startedAtMillis: Long,
    val finishedAtMillis: Long,
    val difficulty: String = "NORMAL",
    val notes: String = ""
)

@Entity(
    tableName = "review_questions",
    foreignKeys = [ForeignKey(
        entity = StudyTopicEntity::class,
        parentColumns = ["id"],
        childColumns = ["topicId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("topicId"), Index("nextReviewEpochDay")]
)
data class ReviewQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val prompt: String,
    val answer: String,
    val sourceLabel: String = "MANUAL",
    val active: Boolean = true,
    val nextReviewEpochDay: Long,
    val intervalDays: Int = 0,
    val reviewCount: Int = 0,
    val lastRating: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "review_attempts",
    foreignKeys = [ForeignKey(
        entity = ReviewQuestionEntity::class,
        parentColumns = ["id"],
        childColumns = ["questionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("questionId"), Index("reviewedAtMillis")]
)
data class ReviewAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: Long,
    val rating: String,
    val previousIntervalDays: Int,
    val nextIntervalDays: Int,
    val reviewedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "focus_sessions",
    indices = [Index("startedAtMillis"), Index("topicId")]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long? = null,
    val topicId: Long? = null,
    val label: String = "Foco",
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val interruptions: Int = 0,
    val completed: Boolean,
    val startedAtMillis: Long,
    val finishedAtMillis: Long
)
