@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.taina.constantia.feature.focus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.taina.constantia.core.database.SubjectEntity
import br.com.taina.constantia.core.database.StudyTopicEntity
import br.com.taina.constantia.engine.StudyReviewEngine
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun FocusScreen(viewModel: FocusViewModel, contextSettingsViewModel: ContextSettingsViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val dueQuestions by viewModel.dueQuestions.collectAsState()
    val reviewMessage by viewModel.reviewMessage.collectAsState()
    val suggestion by viewModel.pomodoroSuggestion.collectAsState()
    val generatedQuestions by viewModel.generatedQuestions.collectAsState()

    var showSubjectDialog by remember { mutableStateOf(false) }
    var showTopicDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var showGenerateDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Foco e estudo") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PomodoroCard(viewModel, subjects, topics, suggestion?.suggestedMinutes) }
            item { ContextualSettingsSection(contextSettingsViewModel) }

            item {
                Text("Plano de estudo", style = MaterialTheme.typography.titleLarge)
                Text("Cadastre disciplinas, tópicos e a frequência semanal que pretende cumprir.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    AssistChip(onClick = { showSubjectDialog = true }, label = { Text("+ Disciplina") })
                    AssistChip(onClick = { showTopicDialog = true }, enabled = subjects.isNotEmpty(), label = { Text("+ Tópico") })
                    AssistChip(onClick = { showGoalDialog = true }, enabled = subjects.isNotEmpty(), label = { Text("+ Meta") })
                }
            }

            if (goals.isEmpty()) {
                item { Text("Nenhuma meta de estudo cadastrada.") }
            } else {
                items(goals, key = { it.id }) { goal ->
                    val subject = subjects.firstOrNull { it.id == goal.subjectId }
                    val topic = goal.topicId?.let { id -> topics.firstOrNull { it.id == id } }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(subject?.name ?: "Disciplina", style = MaterialTheme.typography.titleMedium)
                            topic?.let { Text(it.name) }
                            Text("${goal.sessionsPerWeek}x/semana · ${goal.targetMinutesPerSession} min por sessão", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Questões de revisão", style = MaterialTheme.typography.titleLarge)
                        Text("Até 2 questões vencidas por dia.", style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        IconButton(onClick = { showGenerateDialog = true }, enabled = topics.isNotEmpty()) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Gerar questões de texto")
                        }
                        IconButton(onClick = { showQuestionDialog = true }, enabled = topics.isNotEmpty()) {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar questão")
                        }
                    }
                }
            }

            if (dueQuestions.isEmpty()) {
                item { Text("Nenhuma questão para revisar agora.") }
            } else {
                items(dueQuestions, key = { it.question.id }) { item ->
                    ReviewQuestionCard(
                        title = listOfNotNull(item.subject?.name, item.topic?.name).joinToString(" · "),
                        prompt = item.question.prompt,
                        answer = item.question.answer,
                        onRate = { rating -> viewModel.rate(item.question.id, rating) }
                    )
                }
            }
        }
    }

    reviewMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearReviewMessage,
            confirmButton = { TextButton(onClick = viewModel::clearReviewMessage) { Text("OK") } },
            title = { Text("Revisão programada") },
            text = { Text(message) }
        )
    }

    if (showSubjectDialog) AddSubjectDialog({ showSubjectDialog = false }) { viewModel.addSubject(it); showSubjectDialog = false }
    if (showTopicDialog) AddTopicDialog(subjects, { showTopicDialog = false }) { subjectId, name -> viewModel.addTopic(subjectId, name); showTopicDialog = false }
    if (showGoalDialog) AddGoalDialog(subjects, topics, { showGoalDialog = false }) { subjectId, topicId, sessions, minutes ->
        viewModel.addGoal(subjectId, topicId, sessions, minutes); showGoalDialog = false
    }

    if (showGenerateDialog) GenerateQuestionsDialog(
        subjects = subjects,
        topics = topics,
        generated = generatedQuestions,
        onDismiss = { viewModel.clearGeneratedQuestions(); showGenerateDialog = false },
        onGenerate = viewModel::generateQuestionsFromText,
        onSave = { topicId -> viewModel.saveGeneratedQuestions(topicId); showGenerateDialog = false }
    )

    if (showQuestionDialog) AddQuestionDialog(subjects, topics, { showQuestionDialog = false }) { topicId, prompt, answer ->
        viewModel.addQuestion(topicId, prompt, answer); showQuestionDialog = false
    }
}

@Composable
private fun PomodoroCard(
    viewModel: FocusViewModel,
    subjects: List<SubjectEntity>,
    topics: List<StudyTopicEntity>,
    suggestedMinutes: Int?
) {
    var minutesText by remember { mutableStateOf("25") }
    val plannedMinutes = minutesText.toIntOrNull()?.coerceIn(10, 90) ?: 25
    var remainingSeconds by remember { mutableIntStateOf(plannedMinutes * 60) }
    var running by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var interruptions by remember { mutableIntStateOf(0) }
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var selectedTopicId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(minutesText) {
        if (!running && startedAt == 0L) remainingSeconds = plannedMinutes * 60
    }

    LaunchedEffect(running) {
        while (running && remainingSeconds > 0) {
            delay(1_000)
            remainingSeconds = max(0, remainingSeconds - 1)
        }
        if (running && remainingSeconds == 0) {
            running = false
            viewModel.saveFocusSession(
                label = "Pomodoro",
                subjectId = selectedSubjectId,
                topicId = selectedTopicId,
                plannedMinutes = plannedMinutes,
                actualMinutes = plannedMinutes,
                interruptions = interruptions,
                startedAtMillis = startedAt,
                completed = true
            )
            startedAt = 0L
            interruptions = 0
            remainingSeconds = plannedMinutes * 60
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Pomodoro", style = MaterialTheme.typography.titleLarge)
            Text("%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = minutesText,
                onValueChange = { if (!running) minutesText = it.filter(Char::isDigit).take(2) },
                enabled = !running,
                label = { Text("Minutos") },
                singleLine = true
            )
            SubjectDropdown(subjects, selectedSubjectId, enabled = !running) { id ->
                selectedSubjectId = id
                selectedTopicId = null
            }
            TopicDropdown(topics.filter { selectedSubjectId == null || it.subjectId == selectedSubjectId }, selectedTopicId, enabled = !running) { selectedTopicId = it }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!running) {
                    Button(onClick = {
                        if (remainingSeconds <= 0) remainingSeconds = plannedMinutes * 60
                        if (startedAt == 0L) {
                            startedAt = System.currentTimeMillis(); interruptions = 0
                        }
                        running = true
                    }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Iniciar") }
                } else {
                    Button(onClick = { running = false; interruptions += 1 }) { Icon(Icons.Default.Pause, null); Spacer(Modifier.width(4.dp)); Text("Pausar") }
                }
                OutlinedButton(
                    onClick = {
                        if (startedAt != 0L) {
                            val elapsedSeconds = plannedMinutes * 60 - remainingSeconds
                            viewModel.saveFocusSession(
                                label = "Pomodoro",
                                subjectId = selectedSubjectId,
                                topicId = selectedTopicId,
                                plannedMinutes = plannedMinutes,
                                actualMinutes = max(1, (elapsedSeconds / 60.0).roundToInt()),
                                interruptions = interruptions,
                                startedAtMillis = startedAt,
                                completed = remainingSeconds == 0
                            )
                        }
                        running = false; startedAt = 0L; interruptions = 0; remainingSeconds = plannedMinutes * 60
                    },
                    enabled = startedAt != 0L
                ) { Icon(Icons.Default.Stop, null); Spacer(Modifier.width(4.dp)); Text("Encerrar") }
            }
            suggestedMinutes?.takeIf { it != plannedMinutes }?.let {
                Text("Sugestão com base nas sessões recentes: testar $it min. A mudança não é automática.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ReviewQuestionCard(title: String, prompt: String, answer: String, onRate: (StudyReviewEngine.Rating) -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.labelMedium)
            Text(prompt, style = MaterialTheme.typography.titleMedium)
            if (!revealed) {
                Button(onClick = { revealed = true }) { Text("Mostrar resposta") }
            } else {
                Text(answer)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { onRate(StudyReviewEngine.Rating.AGAIN) }) { Text("Errei") }
                    TextButton(onClick = { onRate(StudyReviewEngine.Rating.HARD) }) { Text("Difícil") }
                    TextButton(onClick = { onRate(StudyReviewEngine.Rating.GOOD) }) { Text("Acertei") }
                    TextButton(onClick = { onRate(StudyReviewEngine.Rating.EASY) }) { Text("Fácil") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectDropdown(subjects: List<SubjectEntity>, selectedId: Long?, enabled: Boolean = true, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val name = subjects.firstOrNull { it.id == selectedId }?.name ?: "Sem disciplina"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = !expanded }) {
        OutlinedTextField(name, {}, readOnly = true, enabled = enabled, label = { Text("Disciplina (opcional)") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
        ExposedDropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem({ Text("Sem disciplina") }, onClick = { onSelect(null); expanded = false })
            subjects.forEach { s -> DropdownMenuItem({ Text(s.name) }, onClick = { onSelect(s.id); expanded = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicDropdown(topics: List<StudyTopicEntity>, selectedId: Long?, enabled: Boolean = true, onSelect: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val name = topics.firstOrNull { it.id == selectedId }?.name ?: "Sem tópico"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = !expanded }) {
        OutlinedTextField(name, {}, readOnly = true, enabled = enabled, label = { Text("Tópico (opcional)") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
        ExposedDropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem({ Text("Sem tópico") }, onClick = { onSelect(null); expanded = false })
            topics.forEach { t -> DropdownMenuItem({ Text(t.name) }, onClick = { onSelect(t.id); expanded = false }) }
        }
    }
}

@Composable
private fun AddSubjectDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nova disciplina") }, text = { OutlinedTextField(name, { name = it }, label = { Text("Nome") }) }, confirmButton = { Button(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Salvar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTopicDialog(subjects: List<SubjectEntity>, onDismiss: () -> Unit, onSave: (Long, String) -> Unit) {
    var subjectId by remember { mutableStateOf(subjects.firstOrNull()?.id) }
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Novo tópico") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { SubjectDropdown(subjects, subjectId) { subjectId = it }; OutlinedTextField(name, { name = it }, label = { Text("Tópico / matéria") }) } }, confirmButton = { Button(onClick = { subjectId?.let { onSave(it, name) } }, enabled = subjectId != null && name.isNotBlank()) { Text("Salvar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun AddGoalDialog(subjects: List<SubjectEntity>, topics: List<StudyTopicEntity>, onDismiss: () -> Unit, onSave: (Long, Long?, Int, Int) -> Unit) {
    var subjectId by remember { mutableStateOf(subjects.firstOrNull()?.id) }
    var topicId by remember { mutableStateOf<Long?>(null) }
    var sessions by remember { mutableStateOf("3") }
    var minutes by remember { mutableStateOf("25") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Meta de estudo") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubjectDropdown(subjects, subjectId) { subjectId = it; topicId = null }
        TopicDropdown(topics.filter { it.subjectId == subjectId }, topicId) { topicId = it }
        OutlinedTextField(sessions, { sessions = it.filter(Char::isDigit).take(1) }, label = { Text("Sessões por semana") })
        OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, label = { Text("Minutos por sessão") })
    } }, confirmButton = { Button(onClick = { subjectId?.let { onSave(it, topicId, sessions.toIntOrNull() ?: 3, minutes.toIntOrNull() ?: 25) } }, enabled = subjectId != null) { Text("Salvar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun GenerateQuestionsDialog(
    subjects: List<SubjectEntity>,
    topics: List<StudyTopicEntity>,
    generated: List<br.com.taina.constantia.engine.GeneratedStudyQuestion>,
    onDismiss: () -> Unit,
    onGenerate: (String) -> Unit,
    onSave: (Long) -> Unit
) {
    var subjectId by remember { mutableStateOf(subjects.firstOrNull()?.id) }
    var topicId by remember { mutableStateOf(topics.firstOrNull { it.subjectId == subjectId }?.id) }
    var material by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gerar questões do material") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SubjectDropdown(subjects, subjectId) { subjectId = it; topicId = topics.firstOrNull { t -> t.subjectId == it }?.id }
                TopicDropdown(topics.filter { it.subjectId == subjectId }, topicId) { topicId = it }
                OutlinedTextField(
                    value = material,
                    onValueChange = { material = it },
                    label = { Text("Cole um trecho das suas anotações") },
                    minLines = 5,
                    maxLines = 9,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("O gerador local usa somente frases do texto colado; ele não acrescenta fatos externos.", style = MaterialTheme.typography.labelSmall)
                generated.take(5).forEachIndexed { index, q ->
                    Text("${index + 1}. ${q.prompt}", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (generated.isEmpty()) {
                Button(onClick = { onGenerate(material) }, enabled = material.length >= 30 && topicId != null) { Text("Gerar") }
            } else {
                Button(onClick = { topicId?.let(onSave) }, enabled = topicId != null) { Text("Salvar ${generated.size} questões") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AddQuestionDialog(subjects: List<SubjectEntity>, topics: List<StudyTopicEntity>, onDismiss: () -> Unit, onSave: (Long, String, String) -> Unit) {
    var subjectId by remember { mutableStateOf(subjects.firstOrNull()?.id) }
    var topicId by remember { mutableStateOf(topics.firstOrNull { it.subjectId == subjectId }?.id) }
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nova questão") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubjectDropdown(subjects, subjectId) { subjectId = it; topicId = topics.firstOrNull { t -> t.subjectId == it }?.id }
        TopicDropdown(topics.filter { it.subjectId == subjectId }, topicId) { topicId = it }
        OutlinedTextField(prompt, { prompt = it }, label = { Text("Pergunta") })
        OutlinedTextField(answer, { answer = it }, label = { Text("Resposta") })
    } }, confirmButton = { Button(onClick = { topicId?.let { onSave(it, prompt, answer) } }, enabled = topicId != null && prompt.isNotBlank() && answer.isNotBlank()) { Text("Salvar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
