package br.com.taina.constantia.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextualDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotification(item: NotificationEventEntity): Long

    @Query("SELECT * FROM notification_events WHERE id = :id LIMIT 1")
    suspend fun getNotification(id: Long): NotificationEventEntity?

    @Query("SELECT * FROM notification_events ORDER BY plannedAtMillis DESC LIMIT :limit")
    fun observeRecentNotifications(limit: Int = 30): Flow<List<NotificationEventEntity>>

    @Query("SELECT * FROM notification_events WHERE type = :type ORDER BY plannedAtMillis DESC LIMIT :limit")
    suspend fun getRecentNotificationsByType(type: String, limit: Int = 20): List<NotificationEventEntity>

    @Query("UPDATE notification_events SET status = 'DELIVERED', deliveredAtMillis = :atMillis WHERE id = :id AND status = 'SCHEDULED'")
    suspend fun markDelivered(id: Long, atMillis: Long)

    @Query("UPDATE notification_events SET status = 'OPENED', openedAtMillis = :atMillis WHERE id = :id")
    suspend fun markOpened(id: Long, atMillis: Long)

    @Query("UPDATE notification_events SET status = 'DISMISSED', dismissedAtMillis = :atMillis WHERE id = :id AND status != 'OPENED'")
    suspend fun markDismissed(id: Long, atMillis: Long)

    @Query("UPDATE notification_events SET status = 'CANCELLED' WHERE id = :id AND status = 'SCHEDULED'")
    suspend fun markCancelled(id: Long)

    @Query("SELECT COUNT(*) FROM focus_gate_rules")
    suspend fun gateRuleCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGateRule(item: FocusGateRuleEntity): Long

    @Query("SELECT * FROM focus_gate_rules ORDER BY appLabel")
    fun observeGateRules(): Flow<List<FocusGateRuleEntity>>

    @Query("SELECT * FROM focus_gate_rules ORDER BY appLabel")
    suspend fun getGateRules(): List<FocusGateRuleEntity>

    @Query("SELECT * FROM focus_gate_rules WHERE active = 1 ORDER BY appLabel")
    suspend fun getActiveGateRules(): List<FocusGateRuleEntity>

    @Query("UPDATE focus_gate_rules SET active = :active WHERE id = :id")
    suspend fun setGateRuleActive(id: Long, active: Boolean)

    @Query("UPDATE focus_gate_rules SET conditionType = :conditionType, threshold = :threshold WHERE id = :id")
    suspend fun setGateCondition(id: Long, conditionType: String, threshold: Int)

    @Insert
    suspend fun insertOverride(item: FocusGateOverrideEntity): Long

    @Query("SELECT * FROM focus_gate_overrides WHERE ruleId = :ruleId AND untilMillis > :nowMillis ORDER BY untilMillis DESC LIMIT 1")
    suspend fun getActiveOverride(ruleId: Long, nowMillis: Long): FocusGateOverrideEntity?

    @Query("SELECT MIN(untilMillis) FROM focus_gate_overrides WHERE untilMillis > :nowMillis")
    suspend fun getNextOverrideExpiry(nowMillis: Long): Long?
}
