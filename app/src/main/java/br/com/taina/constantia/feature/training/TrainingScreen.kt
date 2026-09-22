@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.taina.constantia.feature.training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.taina.constantia.core.database.EquipmentEntity
import br.com.taina.constantia.core.database.ExerciseEntity
import br.com.taina.constantia.core.database.ExerciseSetEntity
import br.com.taina.constantia.core.database.ProgressionSuggestionEntity
import br.com.taina.constantia.core.model.StagnationState
import br.com.taina.constantia.core.repository.WorkoutExerciseDetail
import br.com.taina.constantia.core.repository.WorkoutTemplateDetail
import java.time.DayOfWeek
import kotlin.math.roundToInt

@Composable
fun TrainingScreen(viewModel: TrainingViewModel) {
    val state by viewModel.state.collectAsState()
    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.activeWorkout != null -> WorkoutExecutionScreen(state, viewModel)
        else -> TrainingPlanScreen(state, viewModel)
    }
}

@Composable
private fun TrainingPlanScreen(state: TrainingUiState, viewModel: TrainingViewModel) {
    var quickFor by remember { mutableStateOf<WorkoutTemplateDetail?>(null) }
    var showEquipment by remember { mutableStateOf(false) }
    var showRestrictions by remember { mutableStateOf(false) }
    var showDays by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Treino", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        state.message?.let { AssistChip(onClick = viewModel::clearMessage, label = { Text(it) }) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showEquipment = true }) { Text("Equipamentos") }
            OutlinedButton(onClick = { showRestrictions = true }) { Text("Restrições") }
        }
        OutlinedButton(onClick = { showDays = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Dias de treino: ${formatDays(state.trainingDays)}")
        }

        val plan = state.plan
        if (plan == null) {
            Text("Ainda não há um plano ativo.")
            Button(onClick = viewModel::regeneratePlan) { Text("Gerar plano pela anamnese") }
            return@Column
        }

        Text(plan.name, style = MaterialTheme.typography.titleLarge)
        Text(plan.rationale, style = MaterialTheme.typography.bodyMedium)

        if (state.weeklyVolume.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Volume semanal estimado", fontWeight = FontWeight.SemiBold)
                    state.weeklyVolume.forEach { item ->
                        Text("${item.muscleName}: ${cleanNumber(item.effectiveSets)} séries ponderadas", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Séries secundárias entram de forma fracionada como heurística de acompanhamento.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        state.templates.forEach { detail ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(detail.template.name, style = MaterialTheme.typography.titleMedium)
                        Text("~${detail.template.estimatedMinutes} min", style = MaterialTheme.typography.labelMedium)
                    }
                    detail.exercises.forEach { item ->
                        Column {
                            Text("• ${item.exercise.name} — ${item.prescription.plannedSets}×${item.prescription.repMin}–${item.prescription.repMax} · RIR ${item.prescription.targetRirMin}–${item.prescription.targetRirMax}")
                            if (item.stagnation?.state == StagnationState.PLATEAU) {
                                Text("  ↳ tendência de estagnação: revisar antes de aumentar demanda", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.startNormal(detail) }) { Text("Iniciar") }
                        OutlinedButton(onClick = { quickFor = detail }) { Text("Tenho pouco tempo") }
                    }
                }
            }
        }

        if (state.suggestions.isNotEmpty()) {
            HorizontalDivider()
            Text("Sugestões de progressão", style = MaterialTheme.typography.titleMedium)
            state.suggestions.take(8).forEach { item -> SuggestionCard(item, state, viewModel) }
        }

        OutlinedButton(onClick = viewModel::regeneratePlan, modifier = Modifier.fillMaxWidth()) {
            Text("Recalcular plano com configurações atuais")
        }
        Text("Equipamentos e restrições só alteram estruturalmente a ficha quando você recalcula o plano. Substituições por aparelho ocupado valem apenas para a sessão atual.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
    }

    quickFor?.let { template ->
        AlertDialog(
            onDismissRequest = { quickFor = null },
            title = { Text("Quanto tempo você tem?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(20, 30, 40).forEach { minutes ->
                        OutlinedButton(onClick = { viewModel.startQuick(template, minutes); quickFor = null }, modifier = Modifier.fillMaxWidth()) { Text("$minutes minutos") }
                    }
                    Text("O modo rápido preserva exercícios prioritários e reduz primeiro volume/acessórios.")
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { quickFor = null }) { Text("Cancelar") } }
        )
    }
    if (showEquipment) EquipmentDialog(state.equipment, state.unavailableEquipmentCodes, { showEquipment = false }, viewModel::toggleEquipment)
    if (showRestrictions) RestrictionDialog(state, { showRestrictions = false }, viewModel::addExerciseRestriction, viewModel::removeRestriction)
    if (showDays) TrainingDaysDialog(state.trainingDays, { showDays = false }) { viewModel.setTrainingDays(it); showDays = false }
}

@Composable
private fun EquipmentDialog(
    equipment: List<EquipmentEntity>,
    unavailable: Set<String>,
    onDismiss: () -> Unit,
    onToggle: (String, Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Equipamentos da sua unidade") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Text("Desmarque somente o que sua unidade realmente não possui. O catálogo começa com o perfil comum da Smart Fit.", style = MaterialTheme.typography.bodySmall)
                equipment.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.code !in unavailable, onCheckedChange = { onToggle(item.code, it) })
                        Text(item.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Concluir") } }
    )
}

@Composable
private fun RestrictionDialog(
    state: TrainingUiState,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, Boolean) -> Unit,
    onRemove: (Long) -> Unit
) {
    var exercise by remember { mutableStateOf<ExerciseEntity?>(null) }
    var bodyRegion by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var professional by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restrições estruturadas") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Somente exercícios que você marcar explicitamente como 'evitar' são excluídos automaticamente. Observações gerais não são interpretadas como diagnóstico.", style = MaterialTheme.typography.bodySmall)
                state.restrictions.forEach { r ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(8.dp)) {
                            val name = state.exerciseLibrary.firstOrNull { it.code == r.exerciseCode }?.name
                            Text(name ?: r.description, fontWeight = FontWeight.SemiBold)
                            if (name != null && r.description.isNotBlank()) Text(r.description, style = MaterialTheme.typography.bodySmall)
                            if (r.bodyRegion.isNotBlank()) Text("Região: ${r.bodyRegion}", style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = { onRemove(r.id) }) { Text("Remover") }
                        }
                    }
                }
                HorizontalDivider()
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = exercise?.name ?: "",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Exercício a evitar") },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        state.exerciseLibrary.forEach { item ->
                            DropdownMenuItem(text = { Text(item.name) }, onClick = { exercise = item; expanded = false })
                        }
                    }
                }
                OutlinedTextField(bodyRegion, { bodyRegion = it }, label = { Text("Região afetada (opcional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Motivo/observação") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = professional, onCheckedChange = { professional = it })
                    Text("Orientação de profissional para evitar")
                }
                Button(
                    onClick = {
                        exercise?.let { onAdd(it.code, bodyRegion, description, professional) }
                        exercise = null; bodyRegion = ""; description = ""; professional = false
                    },
                    enabled = exercise != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Adicionar restrição") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
private fun TrainingDaysDialog(current: Set<Int>, onDismiss: () -> Unit, onSave: (Set<Int>) -> Unit) {
    val selected = remember(current) { mutableStateListOf<Int>().also { it.addAll(current.sorted()) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dias de treino") },
        text = {
            Column {
                DayOfWeek.values().forEach { day ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = day.value in selected,
                            onCheckedChange = { checked -> if (checked) { if (day.value !in selected) selected.add(day.value) } else selected.remove(day.value) }
                        )
                        Text(dayPt(day))
                    }
                }
                Text("A ordem dos dias é associada à ordem dos treinos da ficha.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { onSave(selected.toSet()) }, enabled = selected.isNotEmpty()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SuggestionCard(item: ProgressionSuggestionEntity, state: TrainingUiState, viewModel: TrainingViewModel) {
    val name = state.templates.flatMap { it.exercises }.firstOrNull { it.exercise.code == item.exerciseCode }?.exercise?.name ?: item.exerciseCode
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(when (item.action) {
                "INCREASE_LOAD" -> "Aumentar ${formatKg(item.currentLoadKg)} → ${formatKg(item.suggestedLoadKg)}"
                "CONSIDER_REDUCTION" -> "Considerar reduzir ${formatKg(item.currentLoadKg)} → ${formatKg(item.suggestedLoadKg)}"
                else -> "Manter ${formatKg(item.currentLoadKg)}"
            })
            Text(item.rationale, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.acceptSuggestion(item) }) { Text("Aceitar") }
                TextButton(onClick = { viewModel.rejectSuggestion(item) }) { Text("Não alterar") }
            }
        }
    }
}

@Composable
private fun WorkoutExecutionScreen(state: TrainingUiState, viewModel: TrainingViewModel) {
    val active = state.activeWorkout ?: return
    var instructionFor by remember { mutableStateOf<WorkoutExerciseDetail?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(active.template.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(if (active.session.mode == "QUICK") "Modo rápido · ${active.session.availableMinutes} min" else "Treino normal", style = MaterialTheme.typography.labelLarge)
        state.message?.let { AssistChip(onClick = viewModel::clearMessage, label = { Text(it) }) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (active.session.mode == "QUICK") Text("O plano original permanece intacto; hoje foram mantidos os exercícios de maior prioridade.", style = MaterialTheme.typography.bodySmall)

        active.exercises.forEach { detail ->
            ExerciseExecutionCard(
                detail = detail,
                savedSets = state.sessionSets.filter { it.workoutExerciseId == detail.prescription.id },
                onInfo = { instructionFor = detail },
                onBusy = { viewModel.requestSubstitutes(detail.prescription.id) },
                onSave = { setIndex, load, reps, rir -> viewModel.saveSet(detail.prescription.id, setIndex, load, reps, rir) }
            )
        }

        Button(onClick = viewModel::finishWorkout, modifier = Modifier.fillMaxWidth()) { Text("Concluir treino") }
        Text("Uma sessão ruim isolada não troca sua ficha. O detector de estagnação exige histórico repetido antes de sinalizar revisão.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
    }

    instructionFor?.let { detail ->
        AlertDialog(
            onDismissRequest = { instructionFor = null }, icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text(detail.exercise.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Execução", fontWeight = FontWeight.Bold); Text(detail.exercise.instructions)
                    Text("Erros comuns", fontWeight = FontWeight.Bold); Text(detail.exercise.commonErrors)
                    if (detail.exercise.demoUrl == null) Text("Vídeo/GIF ainda não cadastrado.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { instructionFor = null }) { Text("Fechar") } }
        )
    }

    if (state.substitutionForId != null) {
        val current = active.exercises.firstOrNull { it.prescription.id == state.substitutionForId }
        AlertDialog(
            onDismissRequest = viewModel::dismissSubstitutes,
            title = { Text("Máquina ocupada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Substituir ${current?.exercise?.name ?: "exercício"} somente nesta sessão por:")
                    if (state.substitutionCandidates.isEmpty()) Text("Nenhuma alternativa compatível e disponível foi encontrada.")
                    state.substitutionCandidates.forEach { item ->
                        OutlinedButton(onClick = { viewModel.applySubstitute(item.code) }, modifier = Modifier.fillMaxWidth()) { Text(item.name) }
                    }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = viewModel::dismissSubstitutes) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ExerciseExecutionCard(
    detail: WorkoutExerciseDetail,
    savedSets: List<ExerciseSetEntity>,
    onInfo: () -> Unit,
    onBusy: () -> Unit,
    onSave: (Int, String, String, Int) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(detail.exercise.name, style = MaterialTheme.typography.titleMedium)
                    if (detail.exercise.code != detail.plannedExercise.code) Text("Hoje substitui: ${detail.plannedExercise.name}", style = MaterialTheme.typography.labelSmall)
                    Text("${detail.selectedSets}×${detail.prescription.repMin}–${detail.prescription.repMax} · RIR ${detail.prescription.targetRirMin}–${detail.prescription.targetRirMax} · descanso ${detail.prescription.restSeconds}s", style = MaterialTheme.typography.bodySmall)
                    detail.prescription.targetLoadKg?.let { Text("Carga de referência: ${formatKg(it)}", style = MaterialTheme.typography.labelMedium) }
                    if (detail.previousSets.isNotEmpty() && detail.exercise.code == detail.plannedExercise.code) {
                        Text("Última sessão: ${formatPrevious(detail.previousSets)}", style = MaterialTheme.typography.labelSmall)
                    }
                    detail.stagnation?.takeIf { it.state != StagnationState.NONE }?.let {
                        Text(if (it.state == StagnationState.PLATEAU) "Possível estagnação: ${it.rationale}" else it.rationale, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                IconButton(onClick = onInfo) { Icon(Icons.Default.Info, contentDescription = "Como fazer") }
            }
            OutlinedButton(onClick = onBusy, modifier = Modifier.fillMaxWidth()) { Text("Máquina ocupada / substituir hoje") }
            repeat(detail.selectedSets) { idx ->
                val setIndex = idx + 1
                val saved = savedSets.firstOrNull { it.setIndex == setIndex }
                SetInputRow(setIndex, saved, detail.prescription.targetRirMax, onSave)
            }
        }
    }
}

@Composable
private fun SetInputRow(setIndex: Int, saved: ExerciseSetEntity?, initialRir: Int, onSave: (Int, String, String, Int) -> Unit) {
    var load by remember(saved?.id) { mutableStateOf(saved?.loadKg?.let { cleanNumber(it) } ?: "") }
    var reps by remember(saved?.id) { mutableStateOf(saved?.reps?.toString() ?: "") }
    var rir by remember(saved?.id) { mutableIntStateOf(saved?.rir ?: initialRir) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Série $setIndex${if (saved != null) " · salva" else ""}", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(load, { load = it }, label = { Text("kg") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(reps, { reps = it }, label = { Text("reps") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("RIR $rir", modifier = Modifier.width(48.dp))
            Slider(value = rir.toFloat(), onValueChange = { rir = it.roundToInt().coerceIn(0, 5) }, valueRange = 0f..5f, steps = 4, modifier = Modifier.weight(1f))
            Button(onClick = { onSave(setIndex, load, reps, rir) }) { Text(if (saved == null) "Salvar" else "Atualizar") }
        }
    }
}

private fun formatPrevious(sets: List<ExerciseSetEntity>): String = sets.joinToString(" · ") { "${cleanNumber(it.loadKg)}×${it.reps} (RIR ${it.rir})" }
private fun formatDays(values: Set<Int>): String = if (values.isEmpty()) "automático" else values.sorted().joinToString(", ") { dayPt(DayOfWeek.of(it)).take(3) }
private fun dayPt(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Segunda"
    DayOfWeek.TUESDAY -> "Terça"
    DayOfWeek.WEDNESDAY -> "Quarta"
    DayOfWeek.THURSDAY -> "Quinta"
    DayOfWeek.FRIDAY -> "Sexta"
    DayOfWeek.SATURDAY -> "Sábado"
    DayOfWeek.SUNDAY -> "Domingo"
}
private fun formatKg(value: Double): String = "${cleanNumber(value)} kg"
private fun cleanNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.1f", value)
