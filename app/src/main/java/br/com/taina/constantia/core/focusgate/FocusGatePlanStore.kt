package br.com.taina.constantia.core.focusgate

import android.content.Context

data class FocusGatePersistedPlan(
    val packages: List<String>,
    val labels: List<String>
) {
    fun labelFor(packageName: String): String =
        packages.indexOf(packageName)
            .takeIf { it >= 0 }
            ?.let { labels.getOrNull(it) }
            ?.takeIf { it.isNotBlank() }
            ?: packageName
}

/**
 * Plano mínimo compartilhado pelo bloqueio de rede e pelo bloqueio estrito de abertura.
 * Guarda somente nomes de pacote/rótulos; não guarda conteúdo, tela ou tráfego.
 */
object FocusGatePlanStore {
    private const val PREFS_NAME = "constantia_focus_gate_runtime"
    private const val PREF_PACKAGES = "blocked_packages"
    private const val PREF_LABELS = "blocked_labels"

    fun save(context: Context, packages: List<String>, labels: List<String>) {
        val normalizedPackages = packages.map(String::trim).filter(String::isNotEmpty).distinct()
        val normalizedLabels = normalizedPackages.mapIndexed { index, packageName ->
            labels.getOrNull(index)?.trim().takeUnless { it.isNullOrBlank() } ?: packageName
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREF_PACKAGES, normalizedPackages.joinToString("\n"))
            .putString(PREF_LABELS, normalizedLabels.joinToString("\n"))
            .apply()
    }

    fun load(context: Context): FocusGatePersistedPlan {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        fun decode(key: String): List<String> = prefs.getString(key, null)
            ?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.toList()
            .orEmpty()
        return FocusGatePersistedPlan(decode(PREF_PACKAGES), decode(PREF_LABELS))
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(PREF_PACKAGES)
            .remove(PREF_LABELS)
            .apply()
    }
}
