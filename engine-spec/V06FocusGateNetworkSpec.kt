import br.com.taina.constantia.engine.FocusGateNetworkPlanEngine
import br.com.taina.constantia.engine.FocusGateNetworkRule

fun main() {
    val engine = FocusGateNetworkPlanEngine()

    val bothBlocked = engine.build(listOf(
        FocusGateNetworkRule("Instagram", "com.instagram.android", active = true, unlocked = false, installed = true),
        FocusGateNetworkRule("Discord", "com.discord", active = true, unlocked = false, installed = true)
    ))
    check(bothBlocked.shouldRunVpn)
    check(bothBlocked.blockedPackages.size == 2)

    val workoutReleased = engine.build(listOf(
        FocusGateNetworkRule("Instagram", "com.instagram.android", active = true, unlocked = true, installed = true),
        FocusGateNetworkRule("Discord", "com.discord", active = true, unlocked = false, installed = true)
    ))
    check(workoutReleased.blockedPackages == listOf("com.discord"))

    val missing = engine.build(listOf(
        FocusGateNetworkRule("Instagram", "com.instagram.android", active = true, unlocked = false, installed = false)
    ))
    check(!missing.shouldRunVpn)
    check(missing.missingLabels == listOf("Instagram"))

    val off = engine.build(listOf(
        FocusGateNetworkRule("Instagram", "com.instagram.android", active = false, unlocked = false, installed = true)
    ))
    check(!off.anyRuleActive)
    check(!off.shouldRunVpn)

    println("Constantia v0.6 focus gate network engine tests: OK")
}
