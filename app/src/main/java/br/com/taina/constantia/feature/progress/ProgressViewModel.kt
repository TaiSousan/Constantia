package br.com.taina.constantia.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.taina.constantia.core.repository.ProgressRepository
import br.com.taina.constantia.core.repository.WeeklyProgressSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ProgressViewModel(private val repository: ProgressRepository) : ViewModel() {
    private val _summary = MutableStateFlow<WeeklyProgressSummary?>(null)
    val summary: StateFlow<WeeklyProgressSummary?> = _summary.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()

    init { refresh() }

    fun previousWeek() {
        _weekOffset.value -= 1
        refresh()
    }

    fun nextWeek() {
        if (_weekOffset.value < 0) {
            _weekOffset.value += 1
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            runCatching { repository.weeklySummary(anchorForOffset(_weekOffset.value)) }
                .onSuccess { _summary.value = it }
            _loading.value = false
        }
    }

    private fun anchorForOffset(offset: Int): LocalDate {
        val today = LocalDate.now()
        if (offset == 0) return today
        val currentWeekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weeksBack = -offset
        return currentWeekStart.minusWeeks((weeksBack - 1).toLong()).minusDays(1)
    }

    class Factory(private val repository: ProgressRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProgressViewModel(repository) as T
    }
}
