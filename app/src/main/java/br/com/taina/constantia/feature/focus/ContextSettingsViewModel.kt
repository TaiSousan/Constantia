package br.com.taina.constantia.feature.focus

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.taina.constantia.core.focusgate.FocusGateController
import br.com.taina.constantia.core.notifications.NotificationCoordinator
import br.com.taina.constantia.core.preferences.AppPreferences
import br.com.taina.constantia.core.preferences.PreferencesState
import br.com.taina.constantia.core.repository.ContextualRepository
import br.com.taina.constantia.core.repository.GateRuleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContextSettingsViewModel(
    private val preferences: AppPreferences,
    private val repository: ContextualRepository,
    private val coordinator: NotificationCoordinator,
    private val focusGateController: FocusGateController
) : ViewModel() {
    val preferencesState = preferences.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PreferencesState()
    )
    val gateRules = repository.gateRules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val vpnStatus = focusGateController.status

    private val _gateStatuses = MutableStateFlow<List<GateRuleStatus>>(emptyList())
    val gateStatuses: StateFlow<List<GateRuleStatus>> = _gateStatuses.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaults()
            refreshGateStatus()
            focusGateController.reconcile()
        }
    }

    fun refreshGateStatus() {
        viewModelScope.launch { _gateStatuses.value = repository.gateStatuses() }
    }

    fun vpnPermissionIntent(): Intent? = focusGateController.prepareIntent()

    fun onVpnPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            if (granted) focusGateController.reconcile() else refreshGateStatus()
        }
    }

    fun setMaster(enabled: Boolean) = update { preferences.setNotificationMaster(enabled) }
    fun setMotivation(enabled: Boolean) = update { preferences.setMotivationalNotifications(enabled) }
    fun setWorkoutReminders(enabled: Boolean) = update { preferences.setWorkoutReminders(enabled) }
    fun setStudyReminders(enabled: Boolean) = update { preferences.setStudyReminders(enabled) }
    fun setQuestionNotifications(enabled: Boolean) = update { preferences.setStudyQuestionNotifications(enabled) }
    fun setQuestionCount(value: Int) = update { preferences.setStudyQuestionsPerDay(value) }
    fun setMaxNotifications(value: Int) = update { preferences.setMaxNonEssentialNotifications(value) }

    fun setGateActive(id: Long, active: Boolean) {
        viewModelScope.launch {
            repository.setGateRuleActive(id, active)
            refreshGateStatus()
            focusGateController.reconcile()
        }
    }

    fun setGateCondition(id: Long, type: String, threshold: Int) {
        viewModelScope.launch {
            repository.setGateCondition(id, type, threshold)
            refreshGateStatus()
            focusGateController.reconcile()
        }
    }

    fun temporaryOverride(id: Long, minutes: Int) {
        viewModelScope.launch {
            repository.temporaryOverride(id, minutes, "Exceção manual")
            refreshGateStatus()
            focusGateController.reconcile()
        }
    }

    fun stopFocusGate() {
        viewModelScope.launch {
            focusGateController.stopNow()
            _gateStatuses.value = repository.gateStatuses()
        }
    }

    fun refreshFocusGate() {
        viewModelScope.launch {
            refreshGateStatus()
            focusGateController.reconcile()
        }
    }

    fun replanNow() {
        viewModelScope.launch { coordinator.planNext36Hours() }
    }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            coordinator.planNext36Hours()
        }
    }

    class Factory(
        private val preferences: AppPreferences,
        private val repository: ContextualRepository,
        private val coordinator: NotificationCoordinator,
        private val focusGateController: FocusGateController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ContextSettingsViewModel(preferences, repository, coordinator, focusGateController) as T
    }
}
