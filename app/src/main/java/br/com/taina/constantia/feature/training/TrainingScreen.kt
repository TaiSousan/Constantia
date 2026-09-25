@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.taina.constantia.feature.training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.taina.constantia.core.database.EquipmentEntity
import br.com.taina.constantia.core.database.ExerciseEntity
import br.com.taina.constantia.core.database.ExerciseSetEntity
import br.com.taina.constantia.core.database.ProgressionSuggestionEntity
import br.com.taina.constantia.core.model.StagnationState
import br.com.taina.constantia.core.repository.WorkoutExerciseDetail
import br.com.taina.constantia.core.repository.ExerciseCatalog
import br.com.taina.constantia.core.repository.DatasetExercise
import br.com.taina.constantia.core.repository.DatasetExerciseLibrary
import br.com.taina.constantia.core.repository.CuratedDatasetExerciseCatalog
import br.com.taina.constantia.core.repository.WorkoutTemplateDetail
import br.com.taina.constantia.engine.TrainingTechniqueEngine
import br.com.taina.constantia.engine.TrainingCycleReviewStatus
import br.com.taina.constantia.engine.TechniqueExerciseInput
import br.com.taina.constantia.engine.TrainingTechniqueSuggestion
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun TrainingScreen(viewModel: TrainingViewModel) {
    val state by viewModel.state.collectAsState()
    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.activeWorkout != null -> WorkoutExecutionScreen(state, viewModel)
        else -> TrainingPlanScreen(state, viewModel)
    }

    state.celebrationText?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearCelebration,
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
            title = { Text("Parabéns! Treino concluído") },
            text = { Text(message) },
            confirmButton = { Button(onClick = viewModel::clearCelebration) { Text("Continuar") } }
        )
    }
}

@Composable
private fun TrainingPlanScreen(state: TrainingUiState, viewModel: TrainingViewModel) {
    var quickFor by remember { mutableStateOf<WorkoutTemplateDetail?>(null) }
    var showEquipment by remember { mutableStateOf(false) }
    var showRestrictions by remember { mutableStateOf(false) }
    var showDays by remember { mutableStateOf(false) }
    var showDatasetLibrary by remember { mutableStateOf(false) }

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
        OutlinedButton(onClick = { showDatasetLibrary = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Biblioteca ampliada de exercícios")
        }
        OutlinedButton(onClick = { showDays = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                "${state.trainingDays.size} dias disponíveis · " +
                    "${state.targetSessionsPerWeek} treino(s)/sem · " +
                    "${state.normalSessionMinutes ?: 60} min"
            )
        }

        val plan = state.plan
        if (plan == null) {
            Text("Ainda não há um plano ativo.")
            Button(onClick = viewModel::regeneratePlan) { Text("Gerar plano pela anamnese") }
            return@Column
        }

        Text(plan.name, style = MaterialTheme.typography.titleLarge)
        Text(plan.rationale, style = MaterialTheme.typography.bodyMedium)

        CycleReviewCard(state, viewModel)

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
                        Text("~${detail.template.estimatedMinutes} min força", style = MaterialTheme.typography.labelMedium)
                    }
                    state.normalSessionMinutes?.takeIf { it > detail.template.estimatedMinutes }?.let { window ->
                        val remaining = window - detail.template.estimatedMinutes
                        if (remaining >= 5) {
                            Text("Janela de $window min · cerca de $remaining min livres para cardio, aquecimento extra ou transições.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    detail.exercises.forEach { item ->
                        Column {
                            Text("• ${item.exercise.name} — ${planExerciseSummary(item)}")
                            if (item.stagnation?.state == StagnationState.PLATEAU) {
                                Text("  ↳ tendência de estagnação: revisar antes de aumentar demanda", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                    TechniqueSuggestionsBlock(techniqueSuggestions(detail.exercises, state.experienceLevel))
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
                    listOf(15, 20, 30, 40, 50).forEach { minutes ->
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
    if (showDays) {
        TrainingAvailabilityDialog(
            currentDays = state.trainingDays,
            currentSessions = state.targetSessionsPerWeek,
            currentNormalMinutes = state.normalSessionMinutes ?: 60,
            currentMinimumMinutes = state.minimumSessionMinutes,
            onDismiss = { showDays = false },
            onSave = { days, sessions, normal, minimum ->
                viewModel.saveTrainingAvailability(days, sessions, normal, minimum)
                showDays = false
            }
        )
    }
    if (showDatasetLibrary) ExerciseDatasetDialog { showDatasetLibrary = false }
}

@Composable
private fun ExerciseDatasetDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var library by remember { mutableStateOf<List<DatasetExercise>>(emptyList()) }
    var selected by remember { mutableStateOf<DatasetExercise?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        library = withContext(Dispatchers.IO) {
            DatasetExerciseLibrary.load(context.applicationContext)
        }
        loading = false
    }

    val filtered = remember(query, library) {
        val term = query.trim()
        val source = if (term.isBlank()) library else library.filter { item ->
            listOf(
                item.displayName,
                item.nameEn,
                item.bodyPartPt,
                item.equipmentPt,
                item.targetPt
            ).any { it.contains(term, ignoreCase = true) }
        }
        source.take(if (term.isBlank()) 80 else 120)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selected == null) "Biblioteca ampliada" else selected!!.displayName) },
        text = {
            if (selected != null) {
                val item = selected!!
                Column(
                    Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.id in CuratedDatasetExerciseCatalog.sourceIds) {
                        AssistChip(onClick = {}, label = { Text("✓ Integrado ao Constantia") })
                        Text(
                            "Pode participar de substituições seguras, adaptações de ficha e cálculos de volume quando for efetivamente usado.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (item.hasPortugueseName) {
                        Text("Original: ${item.nameEn}", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(
                            "Nome mantido em inglês para evitar uma tradução automática pouco confiável.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("Alvo: ${item.targetPt}")
                    Text("Região: ${item.bodyPartPt}")
                    Text("Equipamento: ${item.equipmentPt}")
                    if (item.instructionsEn.isNotBlank()) {
                        HorizontalDivider()
                        Text("Execução — texto original em inglês", fontWeight = FontWeight.SemiBold)
                        Text(item.instructionsEn, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "GIF/vídeo não incorporado nesta versão. O identificador da mídia foi preservado para um futuro pacote local devidamente licenciado.",
                        style = MaterialTheme.typography.labelSmall
                    )
                    TextButton(onClick = { selected = null }) { Text("Voltar à busca") }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "1.324 exercícios de referência. Destes, 69 foram promovidos nesta revisão e o catálogo inteligente passa de 169 para 238 exercícios.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Português é exibido apenas quando a tradução é conservadora; o nome original permanece preservado.",
                        style = MaterialTheme.typography.labelSmall
                    )
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Buscar exercício, alvo ou equipamento") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (loading) {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Text(
                            if (query.isBlank()) "Exibindo os primeiros ${filtered.size}" else "${filtered.size} resultado(s) exibido(s)",
                            style = MaterialTheme.typography.labelSmall
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filtered, key = { it.id }) { item ->
                                TextButton(onClick = { selected = item }, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(
                                            item.displayName +
                                                if (item.id in CuratedDatasetExerciseCatalog.sourceIds) " · ✓ Integrado" else ""
                                        )
                                        if (item.hasPortugueseName) {
                                            Text(item.nameEn, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text(
                                            "${item.targetPt} · ${item.equipmentPt}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        "Fonte de metadados: exercises-dataset (MIT). Mídia de terceiros não incluída.",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
private fun CycleReviewCard(state: TrainingUiState, viewModel: TrainingViewModel) {
    val review = state.cycleReview ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Revisor de ciclo", style = MaterialTheme.typography.titleMedium)
            Text(review.title, fontWeight = FontWeight.SemiBold)
            Text(review.summary, style = MaterialTheme.typography.bodySmall)
            review.adherencePercent?.let {
                Text("Aderência recente: $it%", style = MaterialTheme.typography.labelMedium)
            }
            review.reasons.take(5).forEach { reason ->
                Text("• $reason", style = MaterialTheme.typography.bodySmall)
            }
            val elevated = review.recoveryLoads.filter { it.elevated }.take(3)
            if (elevated.isNotEmpty()) {
                HorizontalDivider()
                Text("Carga de recuperação estimada", fontWeight = FontWeight.SemiBold)
                elevated.forEach { load ->
                    Text(
                        "${load.muscleName}: ${cleanNumber(load.recentEffectiveSets)} séries efetivas recentes " +
                            "(semana anterior ${cleanNumber(load.previousEffectiveSets)})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "É uma heurística do histórico (volume, RIR e recência), não uma medição fisiológica nem diagnóstico de fadiga.",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (review.status == TrainingCycleReviewStatus.REVIEW && state.planProposal == null) {
                Button(onClick = viewModel::buildCyclePlanProposal, modifier = Modifier.fillMaxWidth()) {
                    Text("Gerar proposta de nova ficha")
                }
            }

            state.planProposal?.let { proposal ->
                HorizontalDivider()
                Text("Proposta — ${proposal.spec.name}", fontWeight = FontWeight.Bold)
                Text(
                    "${proposal.review.recommendedSessionsPerWeek} treino(s)/semana. Nada será alterado até você aplicar.",
                    style = MaterialTheme.typography.bodySmall
                )
                val names = state.exerciseLibrary.associate { it.code to it.name }
                proposal.spec.workouts.forEach { workout ->
                    Text("${workout.name} · ~${workout.estimatedMinutes} min", fontWeight = FontWeight.SemiBold)
                    Text(
                        workout.exercises.joinToString(" · ") { names[it.exerciseCode] ?: it.exerciseCode },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Button(onClick = viewModel::applyCyclePlanProposal, modifier = Modifier.fillMaxWidth()) {
                    Text("Aplicar esta ficha")
                }
                TextButton(onClick = viewModel::dismissPlanProposal, modifier = Modifier.fillMaxWidth()) {
                    Text("Descartar proposta")
                }
            }
        }
    }
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
    var exerciseQuery by remember { mutableStateOf("") }
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
                OutlinedTextField(
                    value = exerciseQuery,
                    onValueChange = { exerciseQuery = it },
                    label = { Text("Buscar exercício") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = exercise?.name ?: "",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Exercício a evitar") },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        state.exerciseLibrary
                            .filter { exerciseQuery.isBlank() || it.name.contains(exerciseQuery, ignoreCase = true) }
                            .take(40)
                            .forEach { item ->
                                DropdownMenuItem(text = { Text(item.name) }, onClick = {
                                    exercise = item
                                    exerciseQuery = item.name
                                    expanded = false
                                })
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
                        exercise = null; exerciseQuery = ""; bodyRegion = ""; description = ""; professional = false
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
private fun TrainingAvailabilityDialog(
    currentDays: Set<Int>,
    currentSessions: Int,
    currentNormalMinutes: Int,
    currentMinimumMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Set<Int>, Int, Int, Int) -> Unit
) {
    val selected = remember(currentDays) {
        mutableStateListOf<Int>().also { it.addAll(currentDays.sorted()) }
    }
    var sessions by remember(currentSessions) { mutableIntStateOf(currentSessions.coerceIn(2, 5)) }
    var normalMinutes by remember(currentNormalMinutes) { mutableStateOf(currentNormalMinutes.toString()) }
    var minimumMinutes by remember(currentMinimumMinutes) { mutableStateOf(currentMinimumMinutes.toString()) }

    val maxSessions = minOf(5, selected.size.coerceAtLeast(2))
    LaunchedEffect(maxSessions) {
        sessions = sessions.coerceIn(2, maxSessions)
    }
    val normal = normalMinutes.toIntOrNull()
    val minimum = minimumMinutes.toIntOrNull()
    val valid = selected.size >= 2 &&
        normal != null && normal in 30..180 &&
        minimum != null && minimum in 10..minOf(120, normal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disponibilidade para treino") },
        text = {
            Column(
                Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Marque todos os dias em que você poderia treinar. Isso não obriga o Constantia a prescrever treino em todos eles.",
                    style = MaterialTheme.typography.bodySmall
                )
                DayOfWeek.values().forEach { day ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = day.value in selected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (day.value !in selected) selected.add(day.value)
                                } else {
                                    selected.remove(day.value)
                                }
                            }
                        )
                        Text(dayPt(day))
                    }
                }
                HorizontalDivider()
                Text("Frequência desejada: $sessions treino(s) de força por semana", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = sessions.toFloat(),
                    onValueChange = { sessions = it.roundToInt().coerceIn(2, maxSessions) },
                    valueRange = 2f..maxSessions.toFloat(),
                    steps = (maxSessions - 3).coerceAtLeast(0),
                    enabled = selected.size >= 2
                )
                Text(
                    "Nesta versão, o motor de força trabalha com 2–5 sessões semanais. Ter 7 dias livres significa flexibilidade de agenda, não necessidade de treinar força 7 dias.",
                    style = MaterialTheme.typography.labelSmall
                )
                OutlinedTextField(
                    value = normalMinutes,
                    onValueChange = { normalMinutes = it.filter(Char::isDigit).take(3) },
                    label = { Text("Tempo normal disponível (30–180 min)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minimumMinutes,
                    onValueChange = { minimumMinutes = it.filter(Char::isDigit).take(3) },
                    label = { Text("Mínimo para treino rápido (10–120 min)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Mais tempo livre não é preenchido com séries por obrigação. O revisor considera aderência, progressão, platô e carga de recuperação antes de sugerir mudança estrutural.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selected.toSet(), sessions, normal!!, minimum!!) },
                enabled = valid
            ) { Text("Salvar") }
        },
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
        TechniqueSuggestionsBlock(techniqueSuggestions(active.exercises, state.experienceLevel))

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
                    if (detail.exercise.demoUrl == null) Text("GIF/vídeo não incluído nesta versão; o app fica preparado para mídia local licenciada.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { instructionFor = null }) { Text("Fechar") } }
        )
    }

    if (state.substitutionForId != null) {
        val current = active.exercises.firstOrNull { it.prescription.id == state.substitutionForId }
        AlertDialog(
            onDismissRequest = viewModel::dismissSubstitutes,
            title = { Text("Substituir nesta sessão") },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Substituir ${current?.exercise?.name ?: "exercício"}. O equipamento atual é excluído da busca para priorizar alternativas realmente úteis.")
                    if (state.substitutionCandidates.isEmpty()) Text("Nenhuma alternativa compatível e disponível foi encontrada.")
                    val equipmentNames = state.equipment.associate { it.code to it.name }
                    state.substitutionCandidates.forEach { item ->
                        OutlinedButton(onClick = { viewModel.applySubstitute(item.code) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(item.name)
                                Text(equipmentNames[item.equipmentCode] ?: item.equipmentCode, style = MaterialTheme.typography.labelSmall)
                            }
                        }
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
    var extraRows by remember(detail.prescription.id) { mutableIntStateOf(0) }
    val savedMaxSetIndex = savedSets.maxOfOrNull { it.setIndex } ?: 0
    val visibleSetCount = maxOf(detail.selectedSets + extraRows, savedMaxSetIndex)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(detail.exercise.name, style = MaterialTheme.typography.titleMedium)
                    if (detail.exercise.code != detail.plannedExercise.code) Text("Hoje substitui: ${detail.plannedExercise.name}", style = MaterialTheme.typography.labelSmall)
                    val timed = isTimedExercise(detail.exercise)
                    val unit = if (timed) "s" else "reps"
                    Text("${detail.selectedSets}×${detail.prescription.repMin}–${detail.prescription.repMax} $unit · RIR ${detail.prescription.targetRirMin}–${detail.prescription.targetRirMax} · descanso ${detail.prescription.restSeconds}s", style = MaterialTheme.typography.bodySmall)
                    detail.prescription.targetLoadKg?.let { Text("Carga de referência: ${formatKg(it)}", style = MaterialTheme.typography.labelMedium) }
                    if (detail.previousSets.isNotEmpty() && detail.exercise.code == detail.plannedExercise.code) {
                        Text("Última sessão: ${formatPrevious(detail.previousSets, timed)}", style = MaterialTheme.typography.labelSmall)
                    }
                    detail.stagnation?.takeIf { it.state != StagnationState.NONE }?.let {
                        Text(if (it.state == StagnationState.PLATEAU) "Possível estagnação: ${it.rationale}" else it.rationale, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                IconButton(onClick = onInfo) { Icon(Icons.Default.Info, contentDescription = "Como fazer") }
            }
            OutlinedButton(onClick = onBusy, modifier = Modifier.fillMaxWidth()) { Text("Equipamento ocupado / substituir hoje") }
            val timed = isTimedExercise(detail.exercise)
            repeat(visibleSetCount) { idx ->
                val setIndex = idx + 1
                val saved = savedSets.firstOrNull { it.setIndex == setIndex }
                SetInputRow(
                    setIndex = setIndex,
                    saved = saved,
                    initialRir = detail.prescription.targetRirMax,
                    timed = timed,
                    extra = setIndex > detail.selectedSets,
                    onSave = onSave
                )
            }
            if (visibleSetCount < 12) {
                OutlinedButton(
                    onClick = { extraRows += 1 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Adicionar série extra")
                }
            }
            if (visibleSetCount > detail.selectedSets) {
                Text(
                    "Séries extras entram no histórico real desta sessão, mas não aumentam automaticamente o número de séries prescrito para os próximos treinos.",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun SetInputRow(
    setIndex: Int,
    saved: ExerciseSetEntity?,
    initialRir: Int,
    timed: Boolean,
    extra: Boolean,
    onSave: (Int, String, String, Int) -> Unit
) {
    var load by remember(saved?.id, timed) { mutableStateOf(saved?.loadKg?.let { cleanNumber(it) } ?: if (timed) "0" else "") }
    var reps by remember(saved?.id) { mutableStateOf(saved?.reps?.toString() ?: "") }
    var rir by remember(saved?.id) { mutableIntStateOf(saved?.rir ?: initialRir) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Série $setIndex${if (extra) " · extra" else ""}${if (saved != null) " · salva" else ""}",
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(load, { load = it }, label = { Text(if (timed) "kg extra" else "kg") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(reps, { reps = it }, label = { Text(if (timed) "seg" else "reps") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("RIR $rir", modifier = Modifier.width(48.dp))
            Slider(value = rir.toFloat(), onValueChange = { rir = it.roundToInt().coerceIn(0, 5) }, valueRange = 0f..5f, steps = 4, modifier = Modifier.weight(1f))
            Button(onClick = { onSave(setIndex, load, reps, rir) }) { Text(if (saved == null) "Salvar" else "Atualizar") }
        }
    }
}

private fun formatPrevious(sets: List<ExerciseSetEntity>, timed: Boolean): String = sets.joinToString(" · ") {
    if (timed) {
        val load = if (it.loadKg > 0) "${cleanNumber(it.loadKg)} kg · " else ""
        "$load${it.reps}s (RIR ${it.rir})"
    } else {
        "${cleanNumber(it.loadKg)}×${it.reps} (RIR ${it.rir})"
    }
}

private val techniqueEngine = TrainingTechniqueEngine()

@Composable
private fun TechniqueSuggestionsBlock(items: List<TrainingTechniqueSuggestion>) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        HorizontalDivider()
        Text("Técnicas opcionais", style = MaterialTheme.typography.labelLarge)
        Text(
            "Bi-set, tri-set e métodos de intensificação são sugestões; não mudam sua ficha nem a progressão automaticamente.",
            style = MaterialTheme.typography.labelSmall
        )
        items.take(4).forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(item.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Text(item.instruction, style = MaterialTheme.typography.bodySmall)
                    Text(item.rationale, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun techniqueSuggestions(details: List<WorkoutExerciseDetail>, experienceLevel: String): List<TrainingTechniqueSuggestion> =
    techniqueEngine.suggest(
        details.map { detail ->
            TechniqueExerciseInput(
                code = detail.exercise.code,
                name = detail.exercise.name,
                movementPattern = detail.exercise.movementPattern,
                equipmentCode = detail.exercise.equipmentCode,
                priorityScore = detail.prescription.priorityScore,
                timed = isTimedExercise(detail.exercise)
            )
        },
        experienceLevel = experienceLevel
    )

private fun isTimedExercise(exercise: ExerciseEntity): Boolean =
    exercise.code in ExerciseCatalog.timedExerciseCodes ||
        exercise.movementPattern in setOf("CORE_STABILITY", "ISOMETRIC_LOWER", "ISOMETRIC_ADDUCTION", "GRIP_ISOMETRIC", "LOADED_CARRY")

private fun planExerciseSummary(item: WorkoutExerciseDetail): String {
    val unit = if (isTimedExercise(item.exercise)) "s" else "reps"
    return "${item.prescription.plannedSets}×${item.prescription.repMin}–${item.prescription.repMax} $unit · RIR ${item.prescription.targetRirMin}–${item.prescription.targetRirMax}"
}

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
