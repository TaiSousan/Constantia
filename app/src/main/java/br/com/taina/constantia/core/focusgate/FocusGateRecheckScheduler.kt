package br.com.taina.constantia.core.focusgate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class FocusGateRecheckScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun schedule(atMillis: Long?) {
        cancel()
        if (atMillis == null) return
        val safeAt = atMillis.coerceAtLeast(System.currentTimeMillis() + 5_000L)
        alarms.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            safeAt,
            pendingIntent()
        )
    }

    fun cancel() {
        alarms.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, FocusGateRecheckReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private companion object { const val REQUEST_CODE = 8406 }
}
