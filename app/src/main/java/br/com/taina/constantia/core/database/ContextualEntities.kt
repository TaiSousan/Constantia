package br.com.taina.constantia.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_events",
    indices = [
        Index(value = ["dedupeKey"], unique = true),
        Index("type"),
        Index("plannedAtMillis")
    ]
)
data class NotificationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dedupeKey: String,
    val type: String,
    val title: String,
    val body: String,
    val sourceRef: String = "",
    val plannedAtMillis: Long,
    val plannedMinuteOfDay: Int,
    val status: String = "SCHEDULED",
    val deliveredAtMillis: Long? = null,
    val openedAtMillis: Long? = null,
    val dismissedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "focus_gate_rules",
    indices = [Index(value = ["packageName"], unique = true)]
)
data class FocusGateRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appLabel: String,
    val packageName: String,
    val conditionType: String = "WORKOUT_TODAY",
    val threshold: Int = 1,
    val emergencyMinutes: Int = 15,
    val active: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "focus_gate_overrides",
    foreignKeys = [ForeignKey(
        entity = FocusGateRuleEntity::class,
        parentColumns = ["id"],
        childColumns = ["ruleId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("ruleId"), Index("untilMillis")]
)
data class FocusGateOverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: Long,
    val untilMillis: Long,
    val reason: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)
