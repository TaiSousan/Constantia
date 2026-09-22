@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.taina.constantia.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.taina.constantia.core.model.ExperienceLevel
import br.com.taina.constantia.core.model.Sex
import br.com.taina.constantia.core.model.TrainingGoal

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onCompleted: () -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onCompleted() }

    Scaffold(topBar = { TopAppBar(title = { Text("Anamnese inicial") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("O app usa estes dados para montar a rotina inicial. Tudo poderá ser alterado depois.")
            SectionTitle("Perfil")
            NumberField("Idade", state.age) { viewModel.update { s -> s.copy(age = it) } }
            NumberField("Altura (cm)", state.heightCm) { viewModel.update { s -> s.copy(heightCm = it) } }
            NumberField("Peso atual (kg)", state.weightKg) { viewModel.update { s -> s.copy(weightKg = it) } }
            EnumSelector("Sexo", Sex.entries, state.sex, { it.name }) { viewModel.update { s -> s.copy(sex = it) } }
            EnumSelector("Objetivo principal", TrainingGoal.entries, state.goal, { it.name }) { viewModel.update { s -> s.copy(goal = it) } }
            OutlinedTextField(state.secondaryGoalNotes, { v -> viewModel.update { it.copy(secondaryGoalNotes = v) } }, label = { Text("Objetivos secundários / prioridades") }, minLines = 2, modifier = Modifier.fillMaxWidth())

            SectionTitle("Treino")
            EnumSelector("Experiência", ExperienceLevel.entries, state.experience, { it.name }) { viewModel.update { s -> s.copy(experience = it) } }
            NumberField("Dias que treina hoje/semana", state.currentDays) { viewModel.update { s -> s.copy(currentDays = it) } }
            NumberField("Dias realmente disponíveis/semana", state.availableDays) { viewModel.update { s -> s.copy(availableDays = it) } }
            NumberField("Duração normal (min)", state.normalMinutes) { viewModel.update { s -> s.copy(normalMinutes = it) } }
            NumberField("Tempo mínimo para treino rápido (min)", state.minimumMinutes) { viewModel.update { s -> s.copy(minimumMinutes = it) } }
            TextField(state.preferredTrainingTime, { v -> viewModel.update { it.copy(preferredTrainingTime = v) } }, label = { Text("Horário preferido do treino (HH:MM)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.currentPlanNotes, { v -> viewModel.update { it.copy(currentPlanNotes = v) } }, label = { Text("Como é seu treino atual? (opcional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            SectionTitle("Rotina")
            TimeField("Acordar", state.wakeTime) { viewModel.update { s -> s.copy(wakeTime = it) } }
            TimeField("Dormir", state.sleepTime) { viewModel.update { s -> s.copy(sleepTime = it) } }
            TimeField("Início do trabalho", state.workStart) { viewModel.update { s -> s.copy(workStart = it) } }
            TimeField("Fim do trabalho", state.workEnd) { viewModel.update { s -> s.copy(workEnd = it) } }
            TimeField("Almoço", state.lunchTime) { viewModel.update { s -> s.copy(lunchTime = it) } }
            TimeField("Horário preferido de estudo", state.studyTime) { viewModel.update { s -> s.copy(studyTime = it) } }

            SectionTitle("Alimentação (opcional)")
            Text("Preencha apenas se já tiver metas definidas. O Constantia não inventa uma meta calórica nesta etapa.", style = MaterialTheme.typography.bodySmall)
            NumberField("Meta diária de kcal", state.dailyCalories) { viewModel.update { s -> s.copy(dailyCalories = it) } }
            NumberField("Meta diária de proteína (g)", state.dailyProtein) { viewModel.update { s -> s.copy(dailyProtein = it) } }

            SectionTitle("Estudo inicial (opcional)")
            OutlinedTextField(state.initialSubject, { v -> viewModel.update { it.copy(initialSubject = v) } }, label = { Text("Primeira disciplina") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.initialTopic, { v -> viewModel.update { it.copy(initialTopic = v) } }, label = { Text("Matéria / tópico") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            NumberField("Sessões por semana", state.studySessionsPerWeek) { viewModel.update { s -> s.copy(studySessionsPerWeek = it) } }
            NumberField("Minutos por sessão", state.studyMinutes) { viewModel.update { s -> s.copy(studyMinutes = it) } }

            SectionTitle("Obrigação recorrente inicial (opcional)")
            OutlinedTextField(state.initialActivity, { v -> viewModel.update { it.copy(initialActivity = v) } }, label = { Text("Ex.: Passear com o cachorro") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            NumberField("Vezes por semana (7 = diariamente)", state.initialActivityTimesPerWeek) { viewModel.update { s -> s.copy(initialActivityTimesPerWeek = it) } }

            SectionTitle("Restrições")
            OutlinedTextField(
                value = state.restrictions,
                onValueChange = { v -> viewModel.update { it.copy(restrictions = v) } },
                label = { Text("Dores, limitações ou orientações profissionais") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::save, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.saving) "Salvando..." else "Concluir anamnese")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium) }
@Composable private fun NumberField(label: String, value: String, onChange: (String) -> Unit) = OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
@Composable private fun TimeField(label: String, value: String, onChange: (String) -> Unit) = OutlinedTextField(value, onChange, label = { Text("$label (HH:MM)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

@Composable
private fun <T> EnumSelector(label: String, values: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = text(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value ->
                DropdownMenuItem(text = { Text(text(value)) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}
