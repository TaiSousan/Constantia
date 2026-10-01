package br.com.taina.constantia.core.focusgate

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import br.com.taina.constantia.feature.focus.FocusBlockerActivity

/**
 * Camada opcional de bloqueio estrito.
 * Observa somente o packageName do app que ganhou a janela; não solicita conteúdo da tela.
 */
class FocusGateAccessibilityService : AccessibilityService() {
    private var lastBlockedPackage: String? = null
    private var lastBlockedAtElapsed: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val foregroundPackage = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
        if (foregroundPackage == packageName) return

        val plan = FocusGatePlanStore.load(this)
        if (foregroundPackage !in plan.packages) return

        val now = SystemClock.elapsedRealtime()
        if (foregroundPackage == lastBlockedPackage && now - lastBlockedAtElapsed < 900L) return
        lastBlockedPackage = foregroundPackage
        lastBlockedAtElapsed = now

        // Sai do app bloqueado primeiro; depois mostra uma tela do próprio Constantia.
        performGlobalAction(GLOBAL_ACTION_HOME)
        runCatching {
            startActivity(
                Intent(this, FocusBlockerActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(FocusBlockerActivity.EXTRA_APP_LABEL, plan.labelFor(foregroundPackage))
                }
            )
        }
    }

    override fun onInterrupt() = Unit
}
