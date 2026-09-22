package br.com.taina.constantia.core.focusgate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FocusGateVpnPhase {
    OFF,
    PERMISSION_REQUIRED,
    READY,
    ACTIVE,
    VPN_CONFLICT,
    ERROR
}

data class FocusGateVpnStatus(
    val phase: FocusGateVpnPhase = FocusGateVpnPhase.OFF,
    val blockedLabels: List<String> = emptyList(),
    val blockedPackages: List<String> = emptyList(),
    val missingPackages: List<String> = emptyList(),
    val otherVpnActive: Boolean = false,
    val message: String = "Portão de foco inativo."
)

object FocusGateRuntime {
    private val _status = MutableStateFlow(FocusGateVpnStatus())
    val status: StateFlow<FocusGateVpnStatus> = _status.asStateFlow()

    fun publish(value: FocusGateVpnStatus) {
        _status.value = value
    }
}
