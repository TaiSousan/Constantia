package br.com.taina.constantia.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.taina.constantia.core.database.ActivityDefinitionEntity
import br.com.taina.constantia.core.database.RoutineProfileEntity
import br.com.taina.constantia.core.database.TrainingProfileEntity
import br.com.taina.constantia.core.database.UserProfileEntity
import br.com.taina.constantia.core.model.ActivityCategory
import br.com.taina.constantia.core.model.ActivityPriority
import br.com.taina.constantia.core.model.ExperienceLevel
import br.com.taina.constantia.core.model.FrequencyType
import br.com.taina.constantia.core.model.MissedPolicy
import br.com.taina.constantia.core.model.Sex
import br.com.taina.constantia.core.model.TrainingGoal
import br.com.taina.constantia.core.repository.ActivityRepository
import br.com.taina.constantia.core.repository.NutritionRepository
import br.com.taina.constantia.core.repository.ProfileRepository
import br.com.taina.constantia.core.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class OnboardingState(
    val age: String = "",
    val sex: Sex = Sex.FEMALE,
    val heightCm: String = "",
    val weightKg: String = "",
    val goal: TrainingGoal = TrainingGoal.MIXED,
    val secondaryGoalNotes: String = "",
    val experience: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val currentDays: String = "3",
    val availableDays: String = "4",
    val normalMinutes: String = "60",
    val minimumMinutes: String = "25",
    val preferredTrainingTime: String = "18:30",
    val currentPlanNotes: String = "",
    val wakeTime: String = "07:00",
    val sleepTime: String = "23:00",
    val workStart: String = "",
    val workEnd: String = "",
    val lunchTime: String = "12:30",
    val studyTime: String = "20:00",
    val restrictions: String = "",
    val dailyCalories: String = "",
    val dailyProtein: String = "",
    val initialSubject: String = "",
    val initialTopic: String = "",
    val studySessionsPerWeek: String = "3",
    val studyMinutes: String = "25",
    val initialActivity: String = "",
    val initialActivityTimesPerWeek: String = "7",
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

class OnboardingViewModel(
    private val profileRepository: ProfileRepository,
    private val studyRepository: StudyRepository,
    private val nutritionRepository: NutritionRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun update(transform: (OnboardingState) -> OnboardingState) { _state.value = transform(_state.value) }

    fun save() {
        val s = _state.value
        val age = s.age.toIntOrNull()
        val height = s.heightCm.replace(',', '.').toDoubleOrNull()
        val weight = s.weightKg.replace(',', '.').toDoubleOrNull()
        if (age == null || age !in 12..100 || height == null || height !in 100.0..250.0 || weight == null || weight !in 25.0..400.0) {
            _state.value = s.copy(error = "Revise idade, altura e peso.")
            return
        }
        _state.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            profileRepository.saveOnboarding(
                user = UserProfileEntity(
                    age = age,
                    sex = s.sex.name,
                    heightCm = height,
                    initialWeightKg = weight,
                    primaryGoal = s.goal.name,
                    secondaryGoalNotes = s.secondaryGoalNotes.trim()
                ),
                training = TrainingProfileEntity(
                    experienceLevel = s.experience.name,
                    currentTrainingDaysPerWeek = s.currentDays.toIntOrNull()?.coerceIn(0, 7) ?: 0,
                    availableDaysPerWeek = s.availableDays.toIntOrNull()?.coerceIn(1, 7) ?: 3,
                    normalSessionMinutes = s.normalMinutes.toIntOrNull()?.coerceIn(15, 180) ?: 60,
                    minimumSessionMinutes = s.minimumMinutes.toIntOrNull()?.coerceIn(10, 120) ?: 25,
                    preferredTrainingMinuteOfDay = parseTime(s.preferredTrainingTime),
                    currentPlanNotes = s.currentPlanNotes.trim()
                ),
                routine = RoutineProfileEntity(
                    wakeMinuteOfDay = parseTime(s.wakeTime),
                    sleepMinuteOfDay = parseTime(s.sleepTime),
                    workStartMinuteOfDay = parseTime(s.workStart),
                    workEndMinuteOfDay = parseTime(s.workEnd),
                    lunchMinuteOfDay = parseTime(s.lunchTime),
                    preferredStudyMinuteOfDay = parseTime(s.studyTime)
                ),
                restrictionNotes = s.restrictions
            )

            val calories = s.dailyCalories.toIntOrNull()?.takeIf { it > 0 }
            val protein = s.dailyProtein.toIntOrNull()?.takeIf { it > 0 }
            if (calories != null || protein != null) {
                nutritionRepository.setGoal(calories, protein, null, null)
            }

            if (s.initialSubject.isNotBlank()) {
                val subjectId = studyRepository.addSubject(s.initialSubject)
                val topicId = s.initialTopic.takeIf { it.isNotBlank() }?.let { studyRepository.addTopic(subjectId, it) }
                studyRepository.addGoal(
                    subjectId = subjectId,
                    topicId = topicId,
                    sessionsPerWeek = s.studySessionsPerWeek.toIntOrNull()?.coerceIn(1, 7) ?: 3,
                    targetMinutes = s.studyMinutes.toIntOrNull()?.coerceIn(10, 180) ?: 25
                )
            }

            if (s.initialActivity.isNotBlank()) {
                val times = s.initialActivityTimesPerWeek.toIntOrNull()?.coerceIn(1, 7) ?: 7
                activityRepository.addDefinition(
                    ActivityDefinitionEntity(
                        name = s.initialActivity.trim(),
                        category = ActivityCategory.OBLIGATION.name,
                        frequencyType = if (times >= 7) FrequencyType.DAILY.name else FrequencyType.TIMES_PER_WEEK.name,
                        timesPerPeriod = if (times >= 7) 1 else times,
                        anchorEpochDay = LocalDate.now().toEpochDay(),
                        priority = ActivityPriority.REQUIRED.name,
                        missedPolicy = MissedPolicy.ASK.name
                    )
                )
            }

            _state.value = _state.value.copy(saving = false, saved = true)
        }
    }

    private fun parseTime(value: String): Int? {
        val parts = value.trim().split(':')
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val studyRepository: StudyRepository,
        private val nutritionRepository: NutritionRepository,
        private val activityRepository: ActivityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = OnboardingViewModel(
            profileRepository, studyRepository, nutritionRepository, activityRepository
        ) as T
    }
}
