package br.com.taina.constantia.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.taina.constantia.core.database.*
import br.com.taina.constantia.core.focusgate.FocusGateController
import br.com.taina.constantia.core.repository.DueQuestion
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
    private val questionGenerator: StudyQuestionGenerator = StudyQuestionGenerator()
) : ViewModel() {
    val subjects = repository.subjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val topics = repository.topics.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goals = repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _dueQuestions = MutableStateFlow<List<DueQuestion>>(emptyList())
    val dueQuestions: StateFlow<List<DueQuestion>> = _dueQuestions.asStateFlow()

    private val _reviewMessage = MutableStateFlow<String?>(null)
    val reviewMessage: StateFlow<String?> = _reviewMessage.asStateFlow()

    private val _pomodoroSuggestion = MutableStateFlow<PomodoroAdaptationEngine.Decision?>(null)
    val pomodoroSuggestion: StateFlow<PomodoroAdaptationEngine.Decision?> = _pomodoroSuggestion.asStateFlow()

    private val _generatedQuestions = MutableStateFlow<List<GeneratedStudyQuestion>>(emptyList())
    val generatedQuestions: StateFlow<List<GeneratedStudyQuestion>> = _generatedQuestions.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _dueQuestions.value = repository.getDailyQuestions(LocalDate.now(), 2)
            _pomodoroSuggestion.value = repository.pomodoroSuggestion(25)
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

    fun addQuestion(topicId: Long, prompt: String, answer: String) {
        if (topicId <= 0 || prompt.isBlank() || answer.isBlank()) return
        viewModelScope.launch {
            repository.addQuestion(topicId, prompt, answer)
            _dueQuestions.value = repository.getDailyQuestions(LocalDate.now(), 2)
        }
    }

    fun generateQuestionsFromText(text: String) {
        _generatedQuestions.value = questionGenerator.generate(text, maxQuestions = 5)
    }

    fun clearGeneratedQuestions() { _generatedQuestions.value = emptyList() }

    fun saveGeneratedQuestions(topicId: Long) {
        if (topicId <= 0) return
        val drafts = _generatedQuestions.value
        if (drafts.isEmpty()) return
        viewModelScope.launch {
            drafts.forEach { draft ->
                repository.addQuestion(topicId, draft.prompt, draft.answer, sourceLabel = "LOCAL_GENERATED")
            }
            _generatedQuestions.value = emptyList()
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
        private val focusGateController: FocusGateController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FocusViewModel(repository, focusGateController) as T
    }
}
