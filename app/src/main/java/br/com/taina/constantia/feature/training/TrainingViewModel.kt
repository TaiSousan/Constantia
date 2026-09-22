package br.com.taina.constantia.feature.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.taina.constantia.core.database.*
import br.com.taina.constantia.core.focusgate.FocusGateController
import br.com.taina.constantia.core.model.WorkoutMode
import br.com.taina.constantia.core.repository.*
import br.com.taina.constantia.engine.TrainingScheduleEngine
import br.com.taina.constantia.engine.CompletionFeedbackLibrary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


data class TrainingUiState(
    val loading: Boolean = true,
    val plan: TrainingPlanEntity? = null,
    val templates: List<WorkoutTemplateDetail> = emptyList(),
    val weeklyVolume: List<MuscleVolumeSummary> = emptyList(),
    val activeWorkout: ActiveWorkout? = null,
    val sessionSets: List<ExerciseSetEntity> = emptyList(),
    val suggestions: List<ProgressionSuggestionEntity> = emptyList(),
    val equipment: List<EquipmentEntity> = emptyList(),
    val unavailableEquipmentCodes: Set<String> = emptySet(),
    val restrictions: List<RestrictionEntity> = emptyList(),
    val exerciseLibrary: List<ExerciseEntity> = emptyList(),
    val trainingDays: Set<Int> = emptySet(),
    val normalSessionMinutes: Int? = null,
    val experienceLevel: String = "INTERMEDIATE",
    val substitutionForId: Long? = null,
    val substitutionCandidates: List<ExerciseEntity> = emptyList(),
    val message: String? = null,
    val celebrationText: String? = null,
    val error: String? = null
)

class TrainingViewModel(
    private val repository: TrainingRepository,
    private val focusGateController: FocusGateController,
    private val scheduleEngine: TrainingScheduleEngine = TrainingScheduleEngine()
) : ViewModel() {
    private val _state = MutableStateFlow(TrainingUiState())
    val state: StateFlow<TrainingUiState> = _state.asStateFlow()
    private var setsJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching { repository.ensureReady() }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Falha ao preparar o treino.") }
            repository.activePlan.collect { plan ->
                if (plan == null) {
                    _state.value = _state.value.copy(loading = false, plan = null, templates = emptyList())
                } else {
                    refreshPlan(plan)
                }
            }
        }
        viewModelScope.launch {
            repository.pendingSuggestions.collect { suggestions -> _state.value = _state.value.copy(suggestions = suggestions) }
        }
        viewModelScope.launch {
            repository.equipment.collect { equipment -> _state.value = _state.value.copy(equipment = equipment) }
        }
        viewModelScope.launch {
            repository.equipmentAvailability.collect { rows ->
                _state.value = _state.value.copy(unavailableEquipmentCodes = rows.filterNot { it.available }.map { it.equipmentCode }.toSet())
            }
        }
        viewModelScope.launch {
            repository.restrictions.collect { restrictions -> _state.value = _state.value.copy(restrictions = restrictions) }
        }
        viewModelScope.launch {
            repository.exerciseLibrary.collect { library -> _state.value = _state.value.copy(exerciseLibrary = library) }
        }
        viewModelScope.launch {
            repository.trainingProfile.collect { profile ->
                if (profile != null) {
                    val days = scheduleEngine.resolveDays(profile.availableDaysPerWeek, profile.preferredTrainingDaysCsv).map { it.value }.toSet()
                    _state.value = _state.value.copy(trainingDays = days, normalSessionMinutes = profile.normalSessionMinutes, experienceLevel = profile.experienceLevel)
                }
            }
        }
    }

    private suspend fun refreshPlan(plan: TrainingPlanEntity) {
        val templates = runCatching { repository.loadPlan(plan.id) }.getOrElse { emptyList() }
        val volume = runCatching { repository.calculateWeeklyVolume(templates) }.getOrElse { emptyList() }
        _state.value = _state.value.copy(loading = false, plan = plan, templates = templates, weeklyVolume = volume)
    }

    fun regeneratePlan() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, message = null)
            runCatching { repository.generateNewPlan() }
                .onSuccess { _state.value = _state.value.copy(message = "Plano recalculado considerando anamnese, equipamentos e restrições estruturadas.") }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun startNormal(template: WorkoutTemplateDetail) = start(template, WorkoutMode.NORMAL, null)
    fun startQuick(template: WorkoutTemplateDetail, minutes: Int) = start(template, WorkoutMode.QUICK, minutes)

    private fun start(template: WorkoutTemplateDetail, mode: WorkoutMode, minutes: Int?) {
        viewModelScope.launch {
            runCatching { repository.startWorkout(template.template, mode, minutes) }
                .onSuccess { active ->
                    _state.value = _state.value.copy(activeWorkout = active, sessionSets = emptyList(), message = null, error = null)
                    observeSets(active)
                }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Não foi possível iniciar o treino.") }
        }
    }

    private fun observeSets(active: ActiveWorkout) {
        setsJob?.cancel()
        setsJob = viewModelScope.launch {
            repository.observeSessionSets(active.session.id).collect { sets -> _state.value = _state.value.copy(sessionSets = sets) }
        }
    }

    fun requestSubstitutes(workoutExerciseId: Long) {
        val active = _state.value.activeWorkout ?: return
        viewModelScope.launch {
            runCatching { repository.findSubstitutes(active, workoutExerciseId) }
                .onSuccess { _state.value = _state.value.copy(substitutionForId = workoutExerciseId, substitutionCandidates = it) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Não foi possível encontrar substituições.") }
        }
    }

    fun applySubstitute(exerciseCode: String) {
        val active = _state.value.activeWorkout ?: return
        val id = _state.value.substitutionForId ?: return
        viewModelScope.launch {
            runCatching { repository.substituteExercise(active, id, exerciseCode) }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(activeWorkout = updated, substitutionForId = null, substitutionCandidates = emptyList(), message = "Substituição aplicada apenas nesta sessão.")
                }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Falha ao substituir exercício.") }
        }
    }

    fun dismissSubstitutes() { _state.value = _state.value.copy(substitutionForId = null, substitutionCandidates = emptyList()) }

    fun saveSet(workoutExerciseId: Long, setIndex: Int, loadText: String, repsText: String, rir: Int) {
        val active = _state.value.activeWorkout ?: return
        val load = loadText.replace(',', '.').toDoubleOrNull()
        val reps = repsText.toIntOrNull()
        if (load == null || load < 0 || reps == null || reps <= 0) {
            _state.value = _state.value.copy(error = "Informe carga e repetições válidas.")
            return
        }
        viewModelScope.launch {
            runCatching { repository.saveSet(active, workoutExerciseId, setIndex, load, reps, rir) }
                .onSuccess { _state.value = _state.value.copy(error = null) }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Não foi possível salvar a série.") }
        }
    }

    fun finishWorkout() {
        val active = _state.value.activeWorkout ?: return
        viewModelScope.launch {
            runCatching { repository.finishWorkout(active) }
                .onSuccess {
                    setsJob?.cancel()
                    val plan = _state.value.plan
                    if (plan != null) refreshPlan(plan)
                    _state.value = _state.value.copy(
                        activeWorkout = null,
                        sessionSets = emptyList(),
                        message = "Treino concluído. Histórico e progressão foram atualizados.",
                        celebrationText = CompletionFeedbackLibrary.workout(active.session.id)
                    )
                    focusGateController.reconcile()
                }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Não foi possível concluir o treino.") }
        }
    }

    fun toggleEquipment(code: String, available: Boolean) {
        viewModelScope.launch { repository.setEquipmentAvailable(code, available) }
    }

    fun addExerciseRestriction(exerciseCode: String, bodyRegion: String, description: String, professional: Boolean) {
        viewModelScope.launch {
            runCatching { repository.addExerciseRestriction(exerciseCode, bodyRegion, description, professional) }
                .onSuccess { _state.value = _state.value.copy(message = "Restrição registrada. Recalcule o plano para aplicá-la estruturalmente.") }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun removeRestriction(id: Long) { viewModelScope.launch { repository.removeRestriction(id) } }

    fun setTrainingDays(days: Set<Int>) {
        if (days.isEmpty()) return
        viewModelScope.launch { repository.setPreferredTrainingDays(days) }
    }

    fun cancelExecution() {
        setsJob?.cancel()
        _state.value = _state.value.copy(activeWorkout = null, sessionSets = emptyList())
    }

    fun acceptSuggestion(item: ProgressionSuggestionEntity) {
        viewModelScope.launch {
            repository.acceptSuggestion(item)
            _state.value.plan?.let { refreshPlan(it) }
        }
    }

    fun rejectSuggestion(item: ProgressionSuggestionEntity) { viewModelScope.launch { repository.rejectSuggestion(item) } }
    fun clearMessage() { _state.value = _state.value.copy(message = null, error = null) }
    fun clearCelebration() { _state.value = _state.value.copy(celebrationText = null) }

    class Factory(
        private val repository: TrainingRepository,
        private val focusGateController: FocusGateController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TrainingViewModel(repository, focusGateController) as T
    }
}
