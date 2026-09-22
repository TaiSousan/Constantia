package br.com.taina.constantia.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import br.com.taina.constantia.core.database.NotificationEventEntity

class ContextualNotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(event: NotificationEventEntity) {
        if (event.plannedAtMillis <= System.currentTimeMillis()) return
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            putExtra(NotificationAlarmReceiver.EXTRA_EVENT_ID, event.id)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            event.plannedAtMillis,
            15 * 60_000L,
            pending
        )
    }
}
