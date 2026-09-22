package br.com.taina.constantia

import android.app.Application
import br.com.taina.constantia.core.notifications.NotificationChannels
import br.com.taina.constantia.core.notifications.NotificationPlanningWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ConstantiaApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.ensure(this)
        NotificationPlanningWorker.ensureScheduled(this)
        appScope.launch {
            container.contextualRepository.seedDefaults()
            container.focusGateController.reconcile()
        }
    }
}
