package br.com.taina.constantia.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.taina.constantia.core.database.ActivityDefinitionEntity
import br.com.taina.constantia.core.model.*
import br.com.taina.constantia.core.repository.ActivityRepository
import br.com.taina.constantia.core.repository.TodayActivity
import br.com.taina.constantia.core.repository.TodayWorkoutSummary
import br.com.taina.constantia.core.repository.TrainingRepository
import br.com.taina.constantia.core.repository.StudyRepository
import br.com.taina.constantia.core.repository.TodayStudyGoal
import br.com.taina.constantia.core.repository.NutritionRepository
import br.com.taina.constantia.core.repository.DayNutrition
import br.com.taina.constantia.engine.NutritionTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class TodayViewModel(
    private val repository: ActivityRepository,
    private val trainingRepository: TrainingRepository,
    private val studyRepository: StudyRepository,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {
    private val today = LocalDate.now()
    val activities: StateFlow<List<TodayActivity>> = repository.observeToday(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _workout = MutableStateFlow<TodayWorkoutSummary?>(null)
    val workout: StateFlow<TodayWorkoutSummary?> = _workout.asStateFlow()

    private val _studyGoals = MutableStateFlow<List<TodayStudyGoal>>(emptyList())
    val studyGoals: StateFlow<List<TodayStudyGoal>> = _studyGoals.asStateFlow()

    private val _dueQuestionCount = MutableStateFlow(0)
    val dueQuestionCount: StateFlow<Int> = _dueQuestionCount.asStateFlow()

    val nutrition: StateFlow<DayNutrition> = nutritionRepository.observeDay(today)
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            DayNutrition(emptyList(), emptyList(), emptyList(), NutritionTotals(), null)
        )

    init {
        refreshWorkout(); refreshStudy()
        viewModelScope.launch { runCatching { nutritionRepository.ensureReady() } }
    }

    fun refreshWorkout() {
        viewModelScope.launch {
            runCatching { trainingRepository.ensureReady(); trainingRepository.getTodayWorkout(today) }
                .onSuccess { _workout.value = it }
        }
    }

    fun refreshStudy() {
        viewModelScope.launch {
            runCatching { studyRepository.getTodayGoals(today) }.onSuccess { _studyGoals.value = it }
            runCatching { studyRepository.getDailyQuestions(today, 2) }.onSuccess { _dueQuestionCount.value = it.size }
        }
    }

    fun toggle(activity: TodayActivity, completed: Boolean) {
        viewModelScope.launch { repository.setCompleted(activity.definition.id, today, completed) }
    }

    fun markMissed(activity: TodayActivity, reason: String) {
        viewModelScope.launch { repository.markMissed(activity.definition.id, today, reason) }
    }

    fun addActivity(name: String, frequency: FrequencyType, times: Int, days: Set<DayOfWeek>) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addDefinition(
                ActivityDefinitionEntity(
                    name = name.trim(),
                    category = ActivityCategory.OBLIGATION.name,
                    frequencyType = frequency.name,
                    timesPerPeriod = times.coerceAtLeast(1),
                    specificDaysCsv = days.joinToString(",") { it.value.toString() },
                    anchorEpochDay = today.toEpochDay(),
                    priority = ActivityPriority.REQUIRED.name,
                    missedPolicy = MissedPolicy.ASK.name
                )
            )
        }
    }

    class Factory(
        private val repository: ActivityRepository,
        private val trainingRepository: TrainingRepository,
        private val studyRepository: StudyRepository,
        private val nutritionRepository: NutritionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TodayViewModel(repository, trainingRepository, studyRepository, nutritionRepository) as T
    }
}
