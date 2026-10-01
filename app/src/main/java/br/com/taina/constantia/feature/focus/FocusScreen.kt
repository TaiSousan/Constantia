@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.taina.constantia.feature.focus

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import br.com.taina.constantia.core.database.SubjectEntity
import br.com.taina.constantia.core.database.StudyTopicEntity
import br.com.taina.constantia.core.notifications.PomodoroAlertPlayer
import br.com.taina.constantia.core.preferences.PreferencesState
import br.com.taina.constantia.core.repository.StudyMaterialHit
import br.com.taina.constantia.core.repository.StudyMaterialSummary
import br.com.taina.constantia.engine.CompletionFeedbackLibrary
import br.com.taina.constantia.engine.PomodoroCycleEngine
import br.com.taina.constantia.engine.PomodoroPhase
import br.com.taina.constantia.engine.StudyReviewEngine
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun FocusScreen(viewModel: FocusViewModel, contextSettingsViewModel: ContextSettingsViewModel, onOpenReading: () -> Unit) {
    val subjects by viewModel.subjects.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val dueQuestions by viewModel.dueQuestions.collectAsState()
    val reviewMessage by viewModel.reviewMessage.collectAsState()
    val suggestion by viewModel.pomodoroSuggestion.collectAsState()
    val generatedQuestions by viewModel.generatedQuestions.collectAsState()
    val prefs by viewModel.preferencesState.collectAsState()
    val materials by viewModel.studyMaterials.collectAsState()
    val materialHits by viewModel.materialHits.collectAsState()
    val materialBusy by viewModel.materialBusy.collectAsState()
    val materialMessage by viewModel.materialMessage.collectAsState()

    var showSubjectDialog by remember { mutableStateOf(false) }
    var showTopicDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importSubjectId by remember { mutableStateOf<Long?>(null) }
    var searchingMaterial by remember { mutableStateOf<StudyMaterialSummary?>(null) }
    var deletingMaterial by remember { mutableStateOf<StudyMaterialSummary?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importStudyPdf(it, importSubjectId) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Foco e estudo") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { PomodoroCard(viewModel, subjects, topics, suggestion?.suggestedMinutes, prefs) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Biblioteca de leitura", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Cadastre livros e acompanhe o progresso por página ou porcentagem. A biblioteca fica local no aparelho e não altera suas metas de estudo.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(onClick = onOpenReading) { Text("Abrir biblioteca") }
                    }
                }
            }
            item { ContextualSettingsSection(contextSettingsViewModel) }

            item {
                Text("Plano de estudo", style = MaterialTheme.typography.titleLarge)
                Text("Cadastre disciplinas, tópicos e a frequência semanal que pretende cumprir.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    AssistChip(onClick = { showSubjectDialog = true }, label = { Text("+ Disciplina") })
                    AssistChip(onClick = { showTopicDialog = true }, enabled = subjects.isNotEmpty(), label = { Text("+ Tópico") })
                    AssistChip(onClick = { showGoalDialog = true }, enabled = subjects.isNotEmpty(), label = { Text("+ Meta") })
                }
                TextButton(onClick = viewModel::seedCurrentSemesterCatalog) {
                    Text("Adicionar matérias e temas deste semestre")
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
                        Text("Materiais locais", style = MaterialTheme.typography.titleLarge)
                        Text("O PDF é lido localmente, convertido em texto por página e não fica armazenado como PDF no Constantia.", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { showImportDialog = true }, enabled = !materialBusy) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Importar PDF")
                    }
                }
                if (materialBusy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                materialMessage?.let {
                    AssistChip(onClick = viewModel::clearMaterialMessage, label = { Text(it) }, modifier = Modifier.padding(top = 6.dp))
                }
            }

            if (materials.isEmpty()) {
                item { Text("Nenhuma apostila indexada no aparelho.") }
            } else {
                items(materials, key = { it.id }) { material ->
                    val subject = subjects.firstOrNull { it.id == material.subjectId }
                    StudyMaterialCard(
                        material = material,
                        subjectName = subject?.name,
                        onSearch = { viewModel.clearMaterialSearch(); searchingMaterial = material },
                        onDelete = { deletingMaterial = material }
                    )
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
                        sourceLabel = item.question.sourceLabel,
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
    if (showImportDialog) ImportMaterialDialog(
        subjects = subjects,
        onDismiss = { showImportDialog = false },
        onImport = { subjectId ->
            importSubjectId = subjectId
            showImportDialog = false
            pdfLauncher.launch(arrayOf("application/pdf"))
        }
    )
    searchingMaterial?.let { material ->
        MaterialSearchDialog(
            material = material,
            hits = materialHits,
            onSearch = { query -> viewModel.searchStudyMaterial(material.id, query) },
            onUseSnippet = { hit ->
                viewModel.generateQuestionsFromMaterial(hit.snippet, material.title, hit.pageNumber)
                viewModel.clearMaterialSearch()
                searchingMaterial = null
                showGenerateDialog = true
            },
            onDismiss = { viewModel.clearMaterialSearch(); searchingMaterial = null }
        )
    }
    deletingMaterial?.let { material ->
        AlertDialog(
            onDismissRequest = { deletingMaterial = null },
            title = { Text("Apagar material local?") },
            text = { Text("O texto indexado de ${material.title} será removido do Constantia. Disciplinas, tópicos, questões e histórico de estudo serão preservados.") },
            confirmButton = { Button(onClick = { viewModel.deleteStudyMaterial(material.id); deletingMaterial = null }) { Text("Apagar") } },
            dismissButton = { TextButton(onClick = { deletingMaterial = null }) { Text("Cancelar") } }
        )
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
    suggestedMinutes: Int?,
    prefs: PreferencesState
) {
    val context = LocalContext.current
    val cycleEngine = remember { PomodoroCycleEngine() }
    var studyText by remember(prefs.defaultPomodoroMinutes) { mutableStateOf(prefs.defaultPomodoroMinutes.toString()) }
    var breakText by remember(prefs.defaultBreakMinutes) { mutableStateOf(prefs.defaultBreakMinutes.toString()) }
    var sound by remember(prefs.pomodoroSound) { mutableStateOf(prefs.pomodoroSound) }
    var vibration by remember(prefs.pomodoroVibration) { mutableStateOf(prefs.pomodoroVibration) }
    var autoStartBreak by remember(prefs.autoStartBreak) { mutableStateOf(prefs.autoStartBreak) }
    val studyMinutes = studyText.toIntOrNull()?.coerceIn(5, 120) ?: 25
    val breakMinutes = breakText.toIntOrNull()?.coerceIn(1, 60) ?: 5

    var phase by remember { mutableStateOf(PomodoroPhase.STUDY) }
    var remainingSeconds by remember { mutableIntStateOf(studyMinutes * 60) }
    var running by remember { mutableStateOf(false) }
    var deadlineMillis by remember { mutableLongStateOf(0L) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var interruptions by remember { mutableIntStateOf(0) }
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var selectedTopicId by remember { mutableStateOf<Long?>(null) }
    var phaseMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(studyText, breakText, phase) {
        if (!running && startedAt == 0L) {
            remainingSeconds = if (phase == PomodoroPhase.STUDY) studyMinutes * 60 else breakMinutes * 60
        }
    }

    LaunchedEffect(running, deadlineMillis) {
        while (running) {
            val millisLeft = deadlineMillis - System.currentTimeMillis()
            remainingSeconds = ceil(millisLeft.coerceAtLeast(0L) / 1000.0).toInt()
            if (remainingSeconds <= 0) break
            delay(250)
        }
        if (running && remainingSeconds <= 0) {
            val finishedPhase = phase
            running = false
            if (finishedPhase == PomodoroPhase.STUDY) {
                viewModel.saveFocusSession(
                    label = "Pomodoro",
                    subjectId = selectedSubjectId,
                    topicId = selectedTopicId,
                    plannedMinutes = studyMinutes,
                    actualMinutes = studyMinutes,
                    interruptions = interruptions,
                    startedAtMillis = startedAt,
                    completed = true
                )
            }
            PomodoroAlertPlayer.signal(context, sound, vibration, studyFinished = finishedPhase == PomodoroPhase.STUDY)
            val transition = cycleEngine.completePhase(finishedPhase, studyMinutes, breakMinutes, autoStartBreak)
            phase = transition.next
            remainingSeconds = transition.nextSeconds
            startedAt = 0L
            interruptions = 0
            phaseMessage = if (finishedPhase == PomodoroPhase.STUDY) {
                CompletionFeedbackLibrary.study(startedAt) + if (transition.autoStartNext) " Descanso iniciado." else " Hora de descansar."
            } else {
                "Descanso concluído. Quando quiser, inicie o próximo bloco de estudo."
            }
            if (transition.autoStartNext) {
                deadlineMillis = System.currentTimeMillis() + transition.nextSeconds * 1000L
                running = true
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Pomodoro", style = MaterialTheme.typography.titleLarge)
                    Text(if (phase == PomodoroPhase.STUDY) "Estudo" else "Descanso", style = MaterialTheme.typography.labelLarge)
                }
                Text("%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60), style = MaterialTheme.typography.headlineMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = studyText,
                    onValueChange = { if (!running) studyText = it.filter(Char::isDigit).take(3) },
                    enabled = !running,
                    label = { Text("Estudo (min)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = breakText,
                    onValueChange = { if (!running) breakText = it.filter(Char::isDigit).take(2) },
                    enabled = !running,
                    label = { Text("Descanso (min)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            if (phase == PomodoroPhase.STUDY) {
                SubjectDropdown(subjects, selectedSubjectId, enabled = !running) { id ->
                    selectedSubjectId = id
                    selectedTopicId = null
                }
                TopicDropdown(topics.filter { selectedSubjectId == null || it.subjectId == selectedSubjectId }, selectedTopicId, enabled = !running) { selectedTopicId = it }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(sound, { sound = it }, enabled = !running)
                Text("Som")
                Spacer(Modifier.width(8.dp))
                Checkbox(vibration, { vibration = it }, enabled = !running)
                Text("Vibração")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(autoStartBreak, { autoStartBreak = it }, enabled = !running)
                Spacer(Modifier.width(8.dp))
                Text("Iniciar descanso automaticamente", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(
                onClick = { viewModel.setPomodoroSettings(studyMinutes, breakMinutes, sound, vibration, autoStartBreak) },
                enabled = !running
            ) { Text("Salvar padrão") }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!running) {
                    Button(onClick = {
                        if (remainingSeconds <= 0) remainingSeconds = if (phase == PomodoroPhase.STUDY) studyMinutes * 60 else breakMinutes * 60
                        if (phase == PomodoroPhase.STUDY && startedAt == 0L) {
                            startedAt = System.currentTimeMillis(); interruptions = 0
                        }
                        deadlineMillis = System.currentTimeMillis() + remainingSeconds * 1000L
                        running = true
                    }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text(if (phase == PomodoroPhase.STUDY) "Iniciar" else "Iniciar descanso") }
                } else {
                    Button(onClick = {
                        val millisLeft = deadlineMillis - System.currentTimeMillis()
                        remainingSeconds = ceil(millisLeft.coerceAtLeast(0L) / 1000.0).toInt()
                        running = false
                        if (phase == PomodoroPhase.STUDY) interruptions += 1
                    }) { Icon(Icons.Default.Pause, null); Spacer(Modifier.width(4.dp)); Text("Pausar") }
                }
                OutlinedButton(
                    onClick = {
                        if (phase == PomodoroPhase.STUDY && startedAt != 0L) {
                            val elapsedSeconds = studyMinutes * 60 - remainingSeconds
                            viewModel.saveFocusSession(
                                label = "Pomodoro",
                                subjectId = selectedSubjectId,
                                topicId = selectedTopicId,
                                plannedMinutes = studyMinutes,
                                actualMinutes = max(1, (elapsedSeconds / 60.0).roundToInt()),
                                interruptions = interruptions,
                                startedAtMillis = startedAt,
                                completed = false
                            )
                        }
                        running = false
                        phase = PomodoroPhase.STUDY
                        startedAt = 0L
                        interruptions = 0
                        remainingSeconds = studyMinutes * 60
                    }
                ) { Icon(Icons.Default.Stop, null); Spacer(Modifier.width(4.dp)); Text("Encerrar") }
            }
            if (phase == PomodoroPhase.BREAK) {
                TextButton(onClick = {
                    running = false
                    phase = PomodoroPhase.STUDY
                    startedAt = 0L
                    remainingSeconds = studyMinutes * 60
                    phaseMessage = "Descanso pulado."
                }) { Text("Pular descanso") }
            }
            phaseMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            suggestedMinutes?.takeIf { it != studyMinutes }?.let {
                Text("Sugestão com base nas sessões recentes: testar $it min de estudo. A mudança não é automática.", style = MaterialTheme.typography.bodySmall)
            }
            Text("O tempo de descanso é cronometrado separadamente e não entra como tempo estudado.", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun StudyMaterialCard(
    material: StudyMaterialSummary,
    subjectName: String?,
    onSearch: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(material.title, style = MaterialTheme.typography.titleMedium)
            subjectName?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            Text("${material.pageCount} páginas · ${material.charCount / 1000} mil caracteres indexados", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSearch) { Icon(Icons.Default.Search, null); Spacer(Modifier.width(4.dp)); Text("Buscar") }
                TextButton(onClick = onDelete) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("Apagar do app") }
            }
        }
    }
}

@Composable
private fun ImportMaterialDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onImport: (Long?) -> Unit
) {
    var subjectId by remember { mutableStateOf(subjects.firstOrNull()?.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importar apostila em PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SubjectDropdown(subjects, subjectId) { subjectId = it }
                Text("O Constantia extrai o texto localmente e apaga a cópia temporária do PDF ao terminar. O documento original no seu armazenamento não é alterado.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { onImport(subjectId) }) { Text("Escolher PDF") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun MaterialSearchDialog(
    material: StudyMaterialSummary,
    hits: List<StudyMaterialHit>,
    onSearch: (String) -> Unit,
    onUseSnippet: (StudyMaterialHit) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buscar em ${material.title}") },
        text = {
            Column(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text("Termo ou expressão") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onSearch(query) }, enabled = query.trim().length >= 2) { Text("Buscar") }
                if (hits.isEmpty()) {
                    Text("Digite um termo para localizar trechos com a página de origem.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(hits) { hit ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(8.dp)) {
                                    Text("Página ${hit.pageNumber}", style = MaterialTheme.typography.labelLarge)
                                    Text(hit.snippet, style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { onUseSnippet(hit) }) { Text("Gerar questões deste trecho") }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
private fun ReviewQuestionCard(title: String, prompt: String, answer: String, sourceLabel: String, onRate: (StudyReviewEngine.Rating) -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.labelMedium)
            if (sourceLabel != "MANUAL" && sourceLabel.isNotBlank()) Text(sourceLabel, style = MaterialTheme.typography.labelSmall)
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
