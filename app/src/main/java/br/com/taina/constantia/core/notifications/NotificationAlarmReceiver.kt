package br.com.taina.constantia.core.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.taina.constantia.ConstantiaApplication
import br.com.taina.constantia.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class NotificationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        if (eventId <= 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as ConstantiaApplication
                val repository = app.container.contextualRepository
                val event = repository.getNotification(eventId) ?: return@launch
                val prefs = app.container.preferences.state.first()
                val enabledForType = prefs.contextualNotifications && when (event.type) {
                    "MOTIVATION" -> prefs.motivationalNotifications
                    "PRE_WORKOUT" -> prefs.workoutReminders
                    "STUDY_REMINDER" -> prefs.studyReminders
                    "STUDY_QUESTION" -> prefs.studyQuestionNotifications
                    else -> true
                }
                if (!enabledForType) {
                    repository.markCancelled(event.id)
                    return@launch
                }
                NotificationChannels.ensure(context)

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_NOTIFICATION_EVENT_ID, event.id)
                }
                val openPending = PendingIntent.getActivity(
                    context,
                    event.id.hashCode(),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val dismissIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
                    putExtra(EXTRA_EVENT_ID, event.id)
                }
                val dismissPending = PendingIntent.getBroadcast(
                    context,
                    event.id.hashCode() xor 0x51,
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val notification = Notification.Builder(context, NotificationChannels.channelFor(event.type))
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(event.title)
                    .setContentText(event.body)
                    .setStyle(Notification.BigTextStyle().bigText(event.body))
                    .setContentIntent(openPending)
                    .setDeleteIntent(dismissPending)
                    .setAutoCancel(true)
                    .build()
                context.getSystemService(NotificationManager::class.java).notify(event.id.hashCode(), notification)
                repository.markDelivered(event.id)
            } finally {
                pending.finish()
            }
        }
    }

    companion object { const val EXTRA_EVENT_ID = "constantia_event_id" }
}
