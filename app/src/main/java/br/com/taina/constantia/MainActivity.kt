package br.com.taina.constantia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import br.com.taina.constantia.ui.ConstantiaApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container: AppContainer by lazy { (application as ConstantiaApplication).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordNotificationOpen(intent)
        setContent { ConstantiaApp(container) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recordNotificationOpen(intent)
    }

    private fun recordNotificationOpen(intent: Intent?) {
        val eventId = intent?.getLongExtra(EXTRA_NOTIFICATION_EVENT_ID, -1L) ?: -1L
        if (eventId > 0) lifecycleScope.launch { container.contextualRepository.markOpened(eventId) }
    }

    companion object { const val EXTRA_NOTIFICATION_EVENT_ID = "constantia_notification_event_id" }
}
