@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.taina.constantia.feature.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.taina.constantia.core.database.ActivityDefinitionEntity
import br.com.taina.constantia.core.model.FrequencyType
import br.com.taina.constantia.core.repository.TodayActivity
import br.com.taina.constantia.engine.CompletionFeedbackLibrary
import java.time.DayOfWeek
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(viewModel: TodayViewModel, onOpenTraining: () -> Unit, onOpenFocus: () -> Unit, onOpenNutrition: () -> Unit) {
    val activities by viewModel.activities.collectAsState()
    val workout by viewModel.workout.collectAsState()
    val completedWorkoutToday by viewModel.completedWorkoutToday.collectAsState()
    val overdueWorkout by viewModel.overdueWorkout.collectAsState()
    val studyGoals by viewModel.studyGoals.collectAsState()
    val dueQuestionCount by viewModel.dueQuestionCount.collectAsState()
    val nutrition by viewModel.nutrition.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshWorkout(); viewModel.refreshStudy() }
    var showAdd by remember { mutableStateOf(false) }
    var missedActivity by remember { mutableStateOf<TodayActivity?>(null) }
    var editingActivity by remember { mutableStateOf<TodayActivity?>(null) }
    var deletingActivity by remember { mutableStateOf<TodayActivity?>(null) }
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
                completedWorkoutToday?.let { completed ->
                    if (workout?.template?.id != completed.template.id) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Treino concluído hoje", style = MaterialTheme.typography.titleMedium)
                                Text(completed.template.name)
                                Text(
                                    "Registrado pela sessão realmente executada. Isso não conclui automaticamente outro treino programado para hoje.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                overdueWorkout?.let { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Treino atrasado", style = MaterialTheme.typography.titleMedium)
                            Text(item.template.name)
                            Text(
                                "Previsto para ${dayName(item.scheduledDate.dayOfWeek)} e ainda pendente.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(onClick = onOpenTraining) { Text("Abrir treino e recuperar") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
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
                                Text(activityFrequencyLabel(item.definition), style = MaterialTheme.typography.bodySmall)
                                if (item.missed) Text("Não feita · ${item.failureReason}", style = MaterialTheme.typography.labelSmall)
                            }
                            var menuExpanded by remember(item.definition.id) { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Opções da atividade")
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Editar") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            editingActivity = item
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Excluir") },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            deletingActivity = item
                                        }
                                    )
                                }
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

    editingActivity?.let { activity ->
        ActivityEditorDialog(
            definition = activity.definition,
            onDismiss = { editingActivity = null },
            onSave = { name, frequency, times, days, everyXDays ->
                viewModel.updateActivity(activity.definition, name, frequency, times, days, everyXDays)
                editingActivity = null
            }
        )
    }

    deletingActivity?.let { activity ->
        AlertDialog(
            onDismissRequest = { deletingActivity = null },
            title = { Text("Excluir atividade?") },
            text = {
                Text("${activity.definition.name} deixará de aparecer nas metas futuras. O histórico já registrado será preservado.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteActivity(activity.definition)
                        deletingActivity = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deletingActivity = null }) { Text("Cancelar") } }
        )
    }

    if (showAdd) {
        ActivityEditorDialog(
            definition = null,
            onDismiss = { showAdd = false },
            onSave = { name, frequency, times, days, everyXDays ->
                viewModel.addActivity(name, frequency, times, days, everyXDays)
                showAdd = false
            }
        )
    }
}

private fun dayName(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "segunda-feira"
    DayOfWeek.TUESDAY -> "terça-feira"
    DayOfWeek.WEDNESDAY -> "quarta-feira"
    DayOfWeek.THURSDAY -> "quinta-feira"
    DayOfWeek.FRIDAY -> "sexta-feira"
    DayOfWeek.SATURDAY -> "sábado"
    DayOfWeek.SUNDAY -> "domingo"
}

private fun shortDayName(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Seg"
    DayOfWeek.TUESDAY -> "Ter"
    DayOfWeek.WEDNESDAY -> "Qua"
    DayOfWeek.THURSDAY -> "Qui"
    DayOfWeek.FRIDAY -> "Sex"
    DayOfWeek.SATURDAY -> "Sáb"
    DayOfWeek.SUNDAY -> "Dom"
}

private fun frequencyLabel(type: FrequencyType): String = when (type) {
    FrequencyType.DAILY -> "Todos os dias"
    FrequencyType.SPECIFIC_DAYS -> "Dias específicos"
    FrequencyType.TIMES_PER_WEEK -> "Vezes por semana"
    FrequencyType.TIMES_PER_MONTH -> "Vezes por mês"
    FrequencyType.EVERY_X_DAYS -> "A cada X dias"
    FrequencyType.ONCE -> "Uma vez"
}

private fun activityFrequencyLabel(definition: ActivityDefinitionEntity): String {
    val type = runCatching { FrequencyType.valueOf(definition.frequencyType) }.getOrNull() ?: return definition.frequencyType
    return when (type) {
        FrequencyType.DAILY -> "Todos os dias"
        FrequencyType.SPECIFIC_DAYS -> definition.specificDaysCsv
            .split(',')
            .mapNotNull { it.trim().toIntOrNull()?.takeIf { value -> value in 1..7 } }
            .map { shortDayName(DayOfWeek.of(it)) }
            .joinToString(" · ")
            .ifBlank { "Dias específicos" }
        FrequencyType.TIMES_PER_WEEK -> "${definition.timesPerPeriod}x por semana"
        FrequencyType.TIMES_PER_MONTH -> "${definition.timesPerPeriod}x por mês"
        FrequencyType.EVERY_X_DAYS -> "A cada ${definition.everyXDays.coerceAtLeast(1)} dia(s)"
        FrequencyType.ONCE -> "Uma vez"
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
private fun ActivityEditorDialog(
    definition: ActivityDefinitionEntity?,
    onDismiss: () -> Unit,
    onSave: (String, FrequencyType, Int, Set<DayOfWeek>, Int) -> Unit
) {
    val initialFrequency = remember(definition?.id) {
        definition?.frequencyType?.let { value -> runCatching { FrequencyType.valueOf(value) }.getOrNull() } ?: FrequencyType.DAILY
    }
    var name by remember(definition?.id) { mutableStateOf(definition?.name.orEmpty()) }
    var frequency by remember(definition?.id) { mutableStateOf(initialFrequency) }
    var times by remember(definition?.id) { mutableStateOf((definition?.timesPerPeriod ?: 3).coerceAtLeast(1).toString()) }
    var everyXDays by remember(definition?.id) { mutableStateOf((definition?.everyXDays ?: 2).coerceAtLeast(1).toString()) }
    var expanded by remember { mutableStateOf(false) }
    val selectedDays = remember(definition?.id) {
        mutableStateListOf<DayOfWeek>().also { list ->
            definition?.specificDaysCsv
                ?.split(',')
                ?.mapNotNull { it.trim().toIntOrNull()?.takeIf { value -> value in 1..7 } }
                ?.mapTo(list) { DayOfWeek.of(it) }
        }
    }
    val validDays = frequency != FrequencyType.SPECIFIC_DAYS || selectedDays.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (definition == null) "Nova atividade" else "Editar atividade") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = frequencyLabel(frequency), onValueChange = {}, readOnly = true,
                        label = { Text("Frequência") },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        FrequencyType.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(frequencyLabel(item)) },
                                onClick = { frequency = item; expanded = false }
                            )
                        }
                    }
                }
                if (frequency == FrequencyType.TIMES_PER_WEEK || frequency == FrequencyType.TIMES_PER_MONTH) {
                    OutlinedTextField(
                        times,
                        { times = it.filter(Char::isDigit).take(2) },
                        label = { Text("Vezes no período") },
                        singleLine = true
                    )
                }
                if (frequency == FrequencyType.EVERY_X_DAYS) {
                    OutlinedTextField(
                        everyXDays,
                        { everyXDays = it.filter(Char::isDigit).take(3) },
                        label = { Text("Repetir a cada quantos dias") },
                        singleLine = true
                    )
                }
                if (frequency == FrequencyType.SPECIFIC_DAYS) {
                    Text("Dias")
                    DayOfWeek.values().forEach { day ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = day in selectedDays,
                                onCheckedChange = { checked ->
                                    if (checked && day !in selectedDays) selectedDays.add(day)
                                    if (!checked) selectedDays.remove(day)
                                }
                            )
                            Text(dayName(day).replaceFirstChar { it.uppercase() })
                        }
                    }
                }
                if (definition != null) {
                    Text(
                        "Editar não apaga registros anteriores. Se a frequência mudar, o novo ciclo começa hoje.",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        frequency,
                        times.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                        selectedDays.toSet(),
                        everyXDays.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    )
                },
                enabled = name.isNotBlank() && validDays
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
