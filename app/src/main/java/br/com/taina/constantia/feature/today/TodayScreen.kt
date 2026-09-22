@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.taina.constantia.feature.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.taina.constantia.core.model.FrequencyType
import br.com.taina.constantia.core.repository.TodayActivity
import br.com.taina.constantia.engine.CompletionFeedbackLibrary
import java.time.DayOfWeek
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(viewModel: TodayViewModel, onOpenTraining: () -> Unit, onOpenFocus: () -> Unit, onOpenNutrition: () -> Unit) {
    val activities by viewModel.activities.collectAsState()
    val workout by viewModel.workout.collectAsState()
    val studyGoals by viewModel.studyGoals.collectAsState()
    val dueQuestionCount by viewModel.dueQuestionCount.collectAsState()
    val nutrition by viewModel.nutrition.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshWorkout(); viewModel.refreshStudy() }
    var showAdd by remember { mutableStateOf(false) }
    var missedActivity by remember { mutableStateOf<TodayActivity?>(null) }
    val done = activities.count { it.completed }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hoje") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = "Adicionar atividade") } }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("$done / ${activities.size} atividades concluídas", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                workout?.let { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Treino de hoje", style = MaterialTheme.typography.titleMedium)
                            Text(item.template.name)
                            item.scheduledMinuteOfDay?.let { minute ->
                                Text("Horário preferido: %02d:%02d".format(minute / 60, minute % 60), style = MaterialTheme.typography.bodySmall)
                            }
                            Text(if (item.completedToday) "Concluído hoje" else "Pendente", style = MaterialTheme.typography.labelLarge)
                            if (!item.completedToday) Button(onClick = onOpenTraining) { Text("Abrir treino") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (studyGoals.isNotEmpty() || dueQuestionCount > 0) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Estudo de hoje", style = MaterialTheme.typography.titleMedium)
                            studyGoals.forEach { goal ->
                                Text("${goal.subject.name}${goal.topic?.let { " · ${it.name}" } ?: ""} — ${goal.goal.targetMinutesPerSession} min")
                            }
                            if (dueQuestionCount > 0) Text("$dueQuestionCount questão(ões) de revisão disponível(is)", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = onOpenFocus) { Text("Abrir foco e estudo") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Alimentação", style = MaterialTheme.typography.titleMedium)
                        val target = nutrition.goal?.dailyCalories
                        Text(if (target != null) "${nutrition.totals.kcal.toInt()} / $target kcal" else "${nutrition.totals.kcal.toInt()} kcal registradas")
                        Text("P ${nutrition.totals.proteinGrams.toInt()} g · C ${nutrition.totals.carbsGrams.toInt()} g · G ${nutrition.totals.fatGrams.toInt()} g", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onOpenNutrition) { Text("Abrir alimentação") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (activities.isEmpty() && workout == null && studyGoals.isEmpty() && dueQuestionCount == 0 && nutrition.entries.isEmpty()) Text("Nenhuma atividade, treino ou estudo programado para hoje. Use + para cadastrar uma obrigação ou hábito.")
            }
            items(activities, key = { it.definition.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = item.completed,
                                onCheckedChange = { checked ->
                                    viewModel.toggle(item, checked)
                                    if (checked) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(CompletionFeedbackLibrary.activity(item.definition.id))
                                        }
                                    }
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(item.definition.name, style = MaterialTheme.typography.bodyLarge)
                                Text(item.definition.frequencyType.replace('_', ' ').lowercase(), style = MaterialTheme.typography.bodySmall)
                                if (item.missed) Text("Não feita · ${item.failureReason}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (!item.completed) {
                            TextButton(onClick = { missedActivity = item }) { Text(if (item.missed) "Alterar motivo" else "Não fiz") }
                        }
                    }
                }
            }
        }
    }


    missedActivity?.let { activity ->
        MissedReasonDialog(
            activityName = activity.definition.name,
            initialReason = activity.failureReason,
            onDismiss = { missedActivity = null },
            onSave = { reason ->
                viewModel.markMissed(activity, reason)
                missedActivity = null
            }
        )
    }

    if (showAdd) AddActivityDialog(onDismiss = { showAdd = false }) { name, frequency, times, days ->
        viewModel.addActivity(name, frequency, times, days)
        showAdd = false
    }
}

@Composable
private fun MissedReasonDialog(
    activityName: String,
    initialReason: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var reason by remember(initialReason) { mutableStateOf(initialReason) }
    val options = listOf("Procrastinei", "Fiquei sem tempo", "Estava cansada", "Esqueci", "Horário ruim", "Imprevisto", "Não quis", "Outro")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Por que não fez?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(activityName, style = MaterialTheme.typography.bodySmall)
                options.forEach { option ->
                    FilterChip(selected = reason == option, onClick = { reason = option }, label = { Text(option) })
                }
                if (reason == "Outro" || (reason.isNotBlank() && reason !in options)) {
                    OutlinedTextField(
                        value = if (reason == "Outro") "" else reason,
                        onValueChange = { reason = it },
                        label = { Text("Motivo") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(reason.ifBlank { "Não informado" }) }) { Text("Registrar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AddActivityDialog(
    onDismiss: () -> Unit,
    onSave: (String, FrequencyType, Int, Set<DayOfWeek>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(FrequencyType.DAILY) }
    var times by remember { mutableStateOf("3") }
    var expanded by remember { mutableStateOf(false) }
    val selectedDays = remember { mutableStateListOf<DayOfWeek>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova atividade") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = frequency.name.replace('_', ' '), onValueChange = {}, readOnly = true,
                        label = { Text("Frequência") },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        FrequencyType.entries.forEach { f -> DropdownMenuItem(text = { Text(f.name.replace('_', ' ')) }, onClick = { frequency = f; expanded = false }) }
                    }
                }
                if (frequency == FrequencyType.TIMES_PER_WEEK || frequency == FrequencyType.TIMES_PER_MONTH) {
                    OutlinedTextField(times, { times = it }, label = { Text("Vezes no período") }, singleLine = true)
                }
                if (frequency == FrequencyType.SPECIFIC_DAYS) {
                    Text("Dias")
                    DayOfWeek.values().forEach { day ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = day in selectedDays, onCheckedChange = { checked -> if (checked) selectedDays.add(day) else selectedDays.remove(day) })
                            Text(day.name)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(name, frequency, times.toIntOrNull() ?: 1, selectedDays.toSet()) }, enabled = name.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
