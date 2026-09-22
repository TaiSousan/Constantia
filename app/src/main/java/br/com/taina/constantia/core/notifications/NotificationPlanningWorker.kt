package br.com.taina.constantia.core.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.taina.constantia.ConstantiaApplication
import java.util.concurrent.TimeUnit

class NotificationPlanningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val app = applicationContext as ConstantiaApplication
        app.container.notificationCoordinator.planNext36Hours()
    }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })

    companion object {
        private const val PERIODIC_NAME = "constantia_contextual_notification_planner"

        fun ensureScheduled(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.enqueue(OneTimeWorkRequestBuilder<NotificationPlanningWorker>().build())
            val periodic = PeriodicWorkRequestBuilder<NotificationPlanningWorker>(12, TimeUnit.HOURS).build()
            wm.enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic)
        }
    }
}
