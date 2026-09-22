package br.com.taina.constantia.core.repository

import br.com.taina.constantia.core.database.*
import br.com.taina.constantia.engine.FocusGateDecision
import br.com.taina.constantia.engine.FocusGateEngine
import br.com.taina.constantia.engine.FocusGateState
import br.com.taina.constantia.engine.NotificationInteractionStats
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

data class GateRuleStatus(
    val rule: FocusGateRuleEntity,
    val decision: FocusGateDecision
)

class ContextualRepository(
    private val dao: ContextualDao,
    private val trainingDao: TrainingDao,
    private val studyDao: StudyDao,
    private val gateEngine: FocusGateEngine = FocusGateEngine()
) {
    val recentNotifications: Flow<List<NotificationEventEntity>> = dao.observeRecentNotifications()
    val gateRules: Flow<List<FocusGateRuleEntity>> = dao.observeGateRules()

    suspend fun seedDefaults() {
        if (dao.gateRuleCount() > 0) return
        dao.insertGateRule(FocusGateRuleEntity(appLabel = "Instagram", packageName = "com.instagram.android"))
        dao.insertGateRule(FocusGateRuleEntity(appLabel = "Discord", packageName = "com.discord"))
    }

    suspend fun insertScheduledNotification(item: NotificationEventEntity): Long = dao.insertNotification(item)
    suspend fun getNotification(id: Long): NotificationEventEntity? = dao.getNotification(id)
    suspend fun markDelivered(id: Long) = dao.markDelivered(id, System.currentTimeMillis())
    suspend fun markOpened(id: Long) = dao.markOpened(id, System.currentTimeMillis())
    suspend fun markDismissed(id: Long) = dao.markDismissed(id, System.currentTimeMillis())
    suspend fun markCancelled(id: Long) = dao.markCancelled(id)

    suspend fun interactionStats(type: String): NotificationInteractionStats {
        val items = dao.getRecentNotificationsByType(type, 20).filter { it.deliveredAtMillis != null }
        return NotificationInteractionStats(
            samples = items.size,
            opened = items.count { it.openedAtMillis != null },
            dismissed = items.count { it.dismissedAtMillis != null }
        )
    }

    suspend fun setGateRuleActive(id: Long, active: Boolean) = dao.setGateRuleActive(id, active)
    suspend fun setGateCondition(id: Long, conditionType: String, threshold: Int) = dao.setGateCondition(id, conditionType, threshold.coerceAtLeast(1))

    suspend fun gateStatuses(date: LocalDate = LocalDate.now()): List<GateRuleStatus> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val state = FocusGateState(
            workoutCompletedToday = trainingDao.countFinishedSessionsBetween(start, end) > 0,
            completedFocusSessionsToday = studyDao.countCompletedFocusSessionsBetween(start, end)
        )
        val now = System.currentTimeMillis()
        return dao.getGateRules().map { rule ->
            val overrideActive = dao.getActiveOverride(rule.id, now) != null
            GateRuleStatus(rule, gateEngine.evaluate(rule.conditionType, rule.threshold, state, overrideActive))
        }
    }

    suspend fun nextOverrideExpiry(nowMillis: Long = System.currentTimeMillis()): Long? = dao.getNextOverrideExpiry(nowMillis)

    suspend fun temporaryOverride(ruleId: Long, minutes: Int, reason: String = "") {
        dao.insertOverride(
            FocusGateOverrideEntity(
                ruleId = ruleId,
                untilMillis = System.currentTimeMillis() + minutes.coerceIn(1, 60) * 60_000L,
                reason = reason.trim()
            )
        )
    }
}
