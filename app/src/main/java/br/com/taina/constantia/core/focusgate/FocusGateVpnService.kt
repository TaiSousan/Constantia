package br.com.taina.constantia.core.focusgate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.net.VpnService
import android.os.ParcelFileDescriptor
import br.com.taina.constantia.MainActivity
import java.io.FileInputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class FocusGateVpnService : VpnService() {
    private var tunnel: ParcelFileDescriptor? = null
    private var discardExecutor: ExecutorService? = null
    private val running = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                clearPersistedPlan(this)
                stopGate()
            }
            ACTION_APPLY -> {
                val packages = intent.getStringArrayListExtra(EXTRA_PACKAGES).orEmpty().distinct()
                val labels = intent.getStringArrayListExtra(EXTRA_LABELS).orEmpty()
                if (packages.isEmpty()) {
                    clearPersistedPlan(this)
                    stopGate()
                } else {
                    FocusGatePlanStore.save(this, packages, labels)
                    applyGate(packages, labels)
                }
            }
            else -> {
                // START_STICKY may recreate the service with a null Intent after process death.
                // Restore the last active local blocking plan instead of silently leaving the gate open.
                val restored = FocusGatePlanStore.load(this)
                if (restored.packages.isEmpty()) stopGate() else applyGate(restored.packages, restored.labels)
            }
        }
        return START_STICKY
    }


    private fun applyGate(packages: List<String>, labels: List<String>) {
        ensureChannel()
        val notification = foregroundNotification(labels.ifEmpty { packages })
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        closeTunnel()

        val installed = packages.filter(::isInstalled)
        if (installed.isEmpty()) {
            FocusGateRuntime.publish(
                FocusGateVpnStatus(
                    phase = FocusGateVpnPhase.ERROR,
                    missingPackages = packages,
                    message = "Nenhum dos aplicativos configurados está instalado."
                )
            )
            stopGate()
            return
        }

        val builder = Builder()
            .setSession("Constantia · Portão de Foco")
            .setMtu(1500)
            .addAddress("10.254.0.1", 32)
            .addRoute("0.0.0.0", 0)
            .addAddress("fd00:1:fd00:1::1", 128)
            .addRoute("::", 0)
            .setBlocking(true)

        installed.forEach { builder.addAllowedApplication(it) }
        val established = runCatching { builder.establish() }.getOrNull()
        if (established == null) {
            FocusGateRuntime.publish(
                FocusGateVpnStatus(
                    phase = FocusGateVpnPhase.ERROR,
                    blockedPackages = installed,
                    message = "O Android não conseguiu iniciar o túnel local do Portão de Foco."
                )
            )
            stopGate()
            return
        }

        tunnel = established
        running.set(true)
        FocusGateRuntime.publish(
            FocusGateVpnStatus(
                phase = FocusGateVpnPhase.ACTIVE,
                blockedLabels = labels,
                blockedPackages = installed,
                discardedBytes = 0L,
                message = "Portão de Foco ativo. A rede dos aplicativos bloqueados está sendo descartada localmente."
            )
        )
        discardExecutor = Executors.newSingleThreadExecutor().also { executor ->
            executor.execute {
                val buffer = ByteArray(32 * 1024)
                var discardedBytes = 0L
                var lastPublished = 0L
                runCatching {
                    FileInputStream(established.fileDescriptor).use { input ->
                        while (running.get()) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (count > 0) {
                                discardedBytes += count
                                if (discardedBytes - lastPublished >= 64 * 1024) {
                                    lastPublished = discardedBytes
                                    val current = FocusGateRuntime.status.value
                                    if (current.phase == FocusGateVpnPhase.ACTIVE) {
                                        FocusGateRuntime.publish(current.copy(discardedBytes = discardedBytes))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun stopGate() {
        closeTunnel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun closeTunnel() {
        running.set(false)
        runCatching { tunnel?.close() }
        tunnel = null
        discardExecutor?.shutdownNow()
        discardExecutor = null
    }

    override fun onRevoke() {
        closeTunnel()
        FocusGateRuntime.publish(
            FocusGateVpnStatus(
                phase = FocusGateVpnPhase.PERMISSION_REQUIRED,
                message = "A autorização de VPN do Constantia foi revogada pelo Android."
            )
        )
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        closeTunnel()
        if (FocusGateRuntime.status.value.phase == FocusGateVpnPhase.ACTIVE) {
            FocusGateRuntime.publish(FocusGateVpnStatus(phase = FocusGateVpnPhase.READY, message = "Portão de Foco pronto, sem bloqueio ativo agora."))
        }
        super.onDestroy()
    }

    private fun foregroundNotification(labels: List<String>): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val summary = labels.joinToString().ifBlank { "apps selecionados" }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Constantia · Portão de Foco")
            .setContentText("Bloqueando rede de: $summary")
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }


    private fun isInstalled(packageName: String): Boolean = runCatching {
        @Suppress("DEPRECATION")
        packageManager.getApplicationInfo(packageName, 0)
    }.isSuccess

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Portão de Foco", NotificationManager.IMPORTANCE_LOW)
        )
    }

    companion object {
        const val ACTION_APPLY = "br.com.taina.constantia.focusgate.APPLY"
        const val ACTION_STOP = "br.com.taina.constantia.focusgate.STOP"
        const val EXTRA_PACKAGES = "focus_gate_packages"
        const val EXTRA_LABELS = "focus_gate_labels"
        private const val CHANNEL_ID = "constantia_focus_gate"
        private const val NOTIFICATION_ID = 8405
        fun clearPersistedPlan(context: Context) {
            FocusGatePlanStore.clear(context)
        }
    }
}
