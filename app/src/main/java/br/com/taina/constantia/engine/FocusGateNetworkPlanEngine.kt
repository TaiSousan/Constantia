package br.com.taina.constantia.engine

data class FocusGateNetworkRule(
    val appLabel: String,
    val packageName: String,
    val active: Boolean,
    val unlocked: Boolean,
    val installed: Boolean
)

data class FocusGateNetworkPlan(
    val blockedLabels: List<String>,
    val blockedPackages: List<String>,
    val missingLabels: List<String>,
    val anyRuleActive: Boolean
) {
    val shouldRunVpn: Boolean get() = blockedPackages.isNotEmpty()
}

class FocusGateNetworkPlanEngine {
    fun build(rules: List<FocusGateNetworkRule>): FocusGateNetworkPlan {
        val blocked = rules.filter { it.active && !it.unlocked }
        return FocusGateNetworkPlan(
            blockedLabels = blocked.filter { it.installed }.map { it.appLabel },
            blockedPackages = blocked.filter { it.installed }.map { it.packageName },
            missingLabels = blocked.filterNot { it.installed }.map { it.appLabel },
            anyRuleActive = rules.any { it.active }
        )
    }
}
