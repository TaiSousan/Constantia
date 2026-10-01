package br.com.taina.constantia.feature.focus

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.taina.constantia.core.database.*
import br.com.taina.constantia.core.focusgate.FocusGateController
import br.com.taina.constantia.core.preferences.AppPreferences
import br.com.taina.constantia.core.preferences.PreferencesState
import br.com.taina.constantia.core.repository.DueQuestion
import br.com.taina.constantia.core.repository.StudyMaterialHit
import br.com.taina.constantia.core.repository.StudyMaterialStore
import br.com.taina.constantia.core.repository.StudyMaterialSummary
import br.com.taina.constantia.core.repository.StudyRepository
import br.com.taina.constantia.engine.PomodoroAdaptationEngine
import br.com.taina.constantia.engine.StudyReviewEngine
import br.com.taina.constantia.engine.StudyQuestionGenerator
import br.com.taina.constantia.engine.GeneratedStudyQuestion
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class FocusViewModel(
    private val repository: StudyRepository,
    private val focusGateController: FocusGateController,
    private val preferences: AppPreferences,
    private val materialStore: StudyMaterialStore,
    private val questionGenerator: StudyQuestionGenerator = StudyQuestionGenerator()
) : ViewModel() {
    val subjects = repository.subjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val topics = repository.topics.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goals = repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val preferencesState = preferences.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PreferencesState())

    private val _dueQuestions = MutableStateFlow<List<DueQuestion>>(emptyList())
    val dueQuestions: StateFlow<List<DueQuestion>> = _dueQuestions.asStateFlow()

    private val _reviewMessage = MutableStateFlow<String?>(null)
    val reviewMessage: StateFlow<String?> = _reviewMessage.asStateFlow()

    private val _pomodoroSuggestion = MutableStateFlow<PomodoroAdaptationEngine.Decision?>(null)
    val pomodoroSuggestion: StateFlow<PomodoroAdaptationEngine.Decision?> = _pomodoroSuggestion.asStateFlow()

    private val _generatedQuestions = MutableStateFlow<List<GeneratedStudyQuestion>>(emptyList())
    val generatedQuestions: StateFlow<List<GeneratedStudyQuestion>> = _generatedQuestions.asStateFlow()
    private var generatedSourceLabel: String = "LOCAL_GENERATED"

    private val _studyMaterials = MutableStateFlow<List<StudyMaterialSummary>>(emptyList())
    val studyMaterials: StateFlow<List<StudyMaterialSummary>> = _studyMaterials.asStateFlow()

    private val _materialHits = MutableStateFlow<List<StudyMaterialHit>>(emptyList())
    val materialHits: StateFlow<List<StudyMaterialHit>> = _materialHits.asStateFlow()

    private val _materialBusy = MutableStateFlow(false)
    val materialBusy: StateFlow<Boolean> = _materialBusy.asStateFlow()

    private val _materialMessage = MutableStateFlow<String?>(null)
    val materialMessage: StateFlow<String?> = _materialMessage.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _dueQuestions.value = repository.getDailyQuestions(LocalDate.now(), 2)
            _pomodoroSuggestion.value = repository.pomodoroSuggestion(preferencesState.value.defaultPomodoroMinutes)
            _studyMaterials.value = materialStore.list()
        }
    }

    fun addSubject(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addSubject(name) }
    }

    fun addTopic(subjectId: Long, name: String, notes: String = "") {
        if (subjectId <= 0 || name.isBlank()) return
        viewModelScope.launch { repository.addTopic(subjectId, name, notes) }
    }

    fun addGoal(subjectId: Long, topicId: Long?, sessionsPerWeek: Int, minutes: Int) {
        if (subjectId <= 0) return
        viewModelScope.launch { repository.addGoal(subjectId, topicId, sessionsPerWeek, minutes) }
    }

    fun seedCurrentSemesterCatalog() {
        viewModelScope.launch {
            val created = repository.seedCurrentSemesterCatalog()
            _materialMessage.value = if (created > 0) {
                "$created item(ns) do semestre adicionados sem alterar suas metas existentes."
            } else {
                "As matérias e temas deste semestre já estavam cadastrados."
            }
        }
    }

    fun importStudyPdf(uri: Uri, subjectId: Long?) {
        viewModelScope.launch {
            _materialBusy.value = true
            _materialMessage.value = "Extraindo texto localmente… o PDF original não será guardado pelo Constantia."
            runCatching { materialStore.importPdf(uri, subjectId) }
                .onSuccess { item ->
                    _studyMaterials.value = materialStore.list()
                    _materialMessage.value = "${item.title}: ${item.pageCount} página(s) indexadas localmente. O arquivo PDF temporário foi descartado."
                }
                .onFailure { error ->
                    _materialMessage.value = "Não foi possível importar o PDF: ${error.message ?: "erro desconhecido"}."
                }
            _materialBusy.value = false
        }
    }

    fun deleteStudyMaterial(id: String) {
        viewModelScope.launch {
            materialStore.delete(id)
            _studyMaterials.value = materialStore.list()
            _materialHits.value = emptyList()
            _materialMessage.value = "Material removido da memória interna do Constantia. Suas sessões, tópicos e questões não foram apagados."
        }
    }

    fun searchStudyMaterial(id: String, query: String) {
        viewModelScope.launch {
            _materialHits.value = materialStore.search(id, query)
        }
    }

    fun clearMaterialSearch() { _materialHits.value = emptyList() }
    fun clearMaterialMessage() { _materialMessage.value = null }

    fun setPomodoroSettings(studyMinutes: Int, breakMinutes: Int, sound: Boolean, vibration: Boolean, autoStartBreak: Boolean) {
        viewModelScope.launch {
            preferences.setPomodoro(studyMinutes, breakMinutes)
            preferences.setPomodoroAlerts(sound, vibration, autoStartBreak)
            _pomodoroSuggestion.value = repository.pomodoroSuggestion(studyMinutes)
        }
    }

    fun addQuestion(topicId: Long, prompt: String, answer: String) {
        if (topicId <= 0 || prompt.isBlank() || answer.isBlank()) return
        viewModelScope.launch {
            repository.addQuestion(topicId, prompt, answer)
            _dueQuestions.value = repository.getDailyQuestions(LocalDate.now(), 2)
        }
    }

    fun generateQuestionsFromText(text: String) {
        generatedSourceLabel = "LOCAL_GENERATED"
        _generatedQuestions.value = questionGenerator.generate(text, maxQuestions = 5)
    }

    fun generateQuestionsFromMaterial(text: String, title: String, pageNumber: Int) {
        generatedSourceLabel = "PDF: $title · p. $pageNumber"
        _generatedQuestions.value = questionGenerator.generate(text, maxQuestions = 5)
    }

    fun clearGeneratedQuestions() {
        _generatedQuestions.value = emptyList()
        generatedSourceLabel = "LOCAL_GENERATED"
    }

    fun saveGeneratedQuestions(topicId: Long) {
        if (topicId <= 0) return
        val drafts = _generatedQuestions.value
        if (drafts.isEmpty()) return
        viewModelScope.launch {
            drafts.forEach { draft ->
                repository.addQuestion(topicId, draft.prompt, draft.answer, sourceLabel = generatedSourceLabel)
            }
            _generatedQuestions.value = emptyList()
            generatedSourceLabel = "LOCAL_GENERATED"
            _dueQuestions.value = repository.getDailyQuestions(LocalDate.now(), 2)
        }
    }

    fun rate(questionId: Long, rating: StudyReviewEngine.Rating) {
        viewModelScope.launch {
            repository.rateQuestion(questionId, rating)?.let { decision ->
                _reviewMessage.value = "Próxima revisão em ${decision.nextIntervalDays} dia(s). ${decision.reason}"
            }
            _dueQuestions.value = repository.getDailyQuestions(LocalDate.now(), 2)
        }
    }

    fun clearReviewMessage() { _reviewMessage.value = null }

    fun saveFocusSession(
        label: String,
        subjectId: Long?,
        topicId: Long?,
        plannedMinutes: Int,
        actualMinutes: Int,
        interruptions: Int,
        startedAtMillis: Long,
        completed: Boolean
    ) {
        viewModelScope.launch {
            repository.completeFocusSession(
                label = label,
                subjectId = subjectId,
                topicId = topicId,
                plannedMinutes = plannedMinutes,
                actualMinutes = actualMinutes,
                interruptions = interruptions,
                startedAtMillis = startedAtMillis,
                finishedAtMillis = System.currentTimeMillis(),
                completed = completed
            )
            _pomodoroSuggestion.value = repository.pomodoroSuggestion(plannedMinutes)
            focusGateController.reconcile()
        }
    }

    class Factory(
        private val repository: StudyRepository,
        private val focusGateController: FocusGateController,
        private val preferences: AppPreferences,
        private val materialStore: StudyMaterialStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FocusViewModel(repository, focusGateController, preferences, materialStore) as T
    }
}
