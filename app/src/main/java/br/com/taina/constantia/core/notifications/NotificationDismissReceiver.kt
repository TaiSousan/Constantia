package br.com.taina.constantia.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.taina.constantia.ConstantiaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(NotificationAlarmReceiver.EXTRA_EVENT_ID, -1L)
        if (eventId <= 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as ConstantiaApplication
                app.container.contextualRepository.markDismissed(eventId)
            } finally { pending.finish() }
        }
    }
}
