package br.com.taina.constantia.core.focusgate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.taina.constantia.ConstantiaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FocusGateRecheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as ConstantiaApplication
                app.container.focusGateController.reconcile()
            } finally {
                pending.finish()
            }
        }
    }
}
