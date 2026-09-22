package br.com.taina.constantia.core.focusgate

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import androidx.core.content.ContextCompat
import br.com.taina.constantia.core.repository.ContextualRepository
import br.com.taina.constantia.engine.FocusGateNetworkPlanEngine
import br.com.taina.constantia.engine.FocusGateNetworkRule
import java.time.LocalDate
import java.time.ZoneId

class FocusGateController(
    private val context: Context,
    private val repository: ContextualRepository,
    private val recheckScheduler: FocusGateRecheckScheduler = FocusGateRecheckScheduler(context),
    private val planEngine: FocusGateNetworkPlanEngine = FocusGateNetworkPlanEngine()
) {
    val status = FocusGateRuntime.status

    fun prepareIntent(): Intent? = VpnService.prepare(context)

    fun hasVpnPermission(): Boolean = prepareIntent() == null

    fun otherVpnIsActive(): Boolean {
        if (FocusGateRuntime.status.value.phase == FocusGateVpnPhase.ACTIVE) return false
        val manager = context.getSystemService(ConnectivityManager::class.java)
        return manager.allNetworks.any { network ->
            manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    }

    suspend fun reconcile() {
        repository.seedDefaults()
        val statuses = repository.gateStatuses()
        val plan = planEngine.build(statuses.map { status ->
            FocusGateNetworkRule(
                appLabel = status.rule.appLabel,
                packageName = status.rule.packageName,
                active = status.rule.active,
                unlocked = status.decision.unlocked,
                installed = isInstalled(status.rule.packageName)
            )
        })

        if (plan.anyRuleActive) scheduleNextRecheck() else recheckScheduler.cancel()

        if (plan.blockedPackages.isEmpty()) {
            stopService()
            FocusGateRuntime.publish(
                FocusGateVpnStatus(
                    phase = if (plan.anyRuleActive) FocusGateVpnPhase.READY else FocusGateVpnPhase.OFF,
                    missingPackages = plan.missingLabels,
                    message = when {
                        plan.missingLabels.isNotEmpty() -> "Há regras bloqueadas, mas os aplicativos configurados não estão instalados neste aparelho."
                        plan.anyRuleActive -> "Condições cumpridas. Nenhum aplicativo precisa ser bloqueado agora."
                        else -> "Nenhuma regra do Portão de Foco está ativa."
                    }
                )
            )
            return
        }

        if (!hasVpnPermission()) {
            stopService()
            val conflict = otherVpnIsActive()
            FocusGateRuntime.publish(
                FocusGateVpnStatus(
                    phase = if (conflict) FocusGateVpnPhase.VPN_CONFLICT else FocusGateVpnPhase.PERMISSION_REQUIRED,
                    blockedLabels = plan.blockedLabels,
                    blockedPackages = plan.blockedPackages,
                    missingPackages = plan.missingLabels,
                    otherVpnActive = conflict,
                    message = if (conflict) {
                        "Outra VPN está ativa. Autorizar o Constantia como VPN pode substituir a VPN atual do Android."
                    } else {
                        "Autorize a VPN local do Constantia para efetivar o bloqueio de rede."
                    }
                )
            )
            return
        }

        startService(plan.blockedPackages, plan.blockedLabels)
    }

    fun stopNow() {
        recheckScheduler.cancel()
        stopService()
        FocusGateRuntime.publish(FocusGateVpnStatus(phase = FocusGateVpnPhase.READY, message = "Portão de Foco parado manualmente."))
    }

    private fun startService(packages: List<String>, labels: List<String>) {
        val intent = Intent(context, FocusGateVpnService::class.java).apply {
            action = FocusGateVpnService.ACTION_APPLY
            putStringArrayListExtra(FocusGateVpnService.EXTRA_PACKAGES, ArrayList(packages))
            putStringArrayListExtra(FocusGateVpnService.EXTRA_LABELS, ArrayList(labels))
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopService() {
        FocusGateVpnService.clearPersistedPlan(context)
        context.stopService(Intent(context, FocusGateVpnService::class.java))
    }

    private fun isInstalled(packageName: String): Boolean = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getApplicationInfo(packageName, 0)
    }.isSuccess

    private suspend fun scheduleNextRecheck() {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val midnight = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 5_000L
        val overrideExpiry = repository.nextOverrideExpiry(now)
        recheckScheduler.schedule(listOfNotNull(midnight, overrideExpiry).minOrNull())
    }
}
