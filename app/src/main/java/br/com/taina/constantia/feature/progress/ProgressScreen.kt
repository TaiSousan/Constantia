package br.com.taina.constantia.feature.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.taina.constantia.engine.AdherenceMetric
import br.com.taina.constantia.engine.FocusMetric
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun ProgressScreen(viewModel: ProgressViewModel) {
    val summary by viewModel.summary.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val weekOffset by viewModel.weekOffset.collectAsState()
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progresso") },
                actions = {
                    IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, contentDescription = "Atualizar") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = viewModel::previousWeek) { Icon(Icons.Default.ChevronLeft, contentDescription = "Semana anterior") }
                    Text(
                        summary?.let { "${it.startDate.format(formatter)} – ${it.endDate.format(formatter)}" } ?: "Resumo semanal",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    IconButton(onClick = viewModel::nextWeek, enabled = weekOffset < 0) { Icon(Icons.Default.ChevronRight, contentDescription = "Semana seguinte") }
                }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            summary?.let { data ->
                item {
                    SectionCard("Aderência") {
                        AdherenceRow("Treinos", data.training)
                        AdherenceRow("Estudo", data.study)
                        AdherenceRow("Obrigações", data.activities)
                        FocusRow("Blocos de foco", data.focus)
                    }
                }

                item {
                    SectionCard("Tempo e foco") {
                        Text("Estudo registrado: ${formatMinutes(data.studyMinutes)}")
                        Text("Foco registrado: ${formatMinutes(data.focusMinutes)}")
                        Text("Interrupções informadas: ${data.focusInterruptions}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                item {
                    SectionCard("Peso") {
                        val current = data.weight.currentAverageKg
                        val previous = data.weight.previousAverageKg
                        when {
                            current == null -> Text("Sem pesagens suficientes neste período.")
                            previous == null -> Text("Média do período: ${"%.1f".format(current)} kg · ainda sem período anterior comparável.")
                            else -> {
                                val delta = current - previous
                                Text("Média do período: ${"%.1f".format(current)} kg")
                                Text("Período equivalente anterior: ${"%.1f".format(previous)} kg", style = MaterialTheme.typography.bodySmall)
                                Text("Diferença: ${if (delta >= 0) "+" else ""}${"%.1f".format(delta)} kg", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (data.exerciseProgress.isNotEmpty()) {
                    item { Text("Exercícios recentes", style = MaterialTheme.typography.titleMedium) }
                    items(data.exerciseProgress, key = { it.exerciseCode }) { item ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(item.exerciseName, style = MaterialTheme.typography.titleSmall)
                                Text("Última: ${formatLoad(item.latestLoadKg)} × ${item.latestReps}")
                                if (item.previousLoadKg != null && item.previousReps != null) {
                                    Text("Anterior: ${formatLoad(item.previousLoadKg)} × ${item.previousReps}", style = MaterialTheme.typography.bodySmall)
                                } else Text("Ainda sem segunda sessão comparável.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (data.failureReasons.isNotEmpty()) {
                    item {
                        SectionCard("Motivos registrados para atividades não feitas") {
                            data.failureReasons.forEach { Text("${it.reason}: ${it.count}x") }
                            Text("Só entram aqui motivos que você registrou; o app não tenta adivinhar a causa.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item {
                    SectionCard("Sugestões da semana") {
                        if (data.suggestions.isEmpty()) {
                            Text("Ainda não há dados suficientes para uma sugestão útil.")
                        } else {
                            data.suggestions.forEach { suggestion ->
                                Text("${suggestion.area} · ${suggestion.title}", style = MaterialTheme.typography.titleSmall)
                                Text(suggestion.rationale, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                        Text("Estas sugestões não alteram seu plano automaticamente.", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun AdherenceRow(label: String, metric: AdherenceMetric) {
    val percent = metric.percent
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(if (metric.planned == 0) "sem meta" else "${metric.completed}/${metric.planned}${percent?.let { " · $it%" } ?: ""}")
        }
        if (metric.planned > 0) LinearProgressIndicator(progress = { (metric.ratio ?: 0.0).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FocusRow(label: String, metric: FocusMetric) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(if (metric.started == 0) "sem sessões" else "${metric.completed}/${metric.started}${metric.percent?.let { " · $it%" } ?: ""}")
        }
        if (metric.started > 0) LinearProgressIndicator(progress = { (metric.ratio ?: 0.0).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
    }
}

private fun formatMinutes(total: Int): String {
    if (total < 60) return "$total min"
    val hours = total / 60
    val minutes = total % 60
    return if (minutes == 0) "${hours}h" else "${hours}h ${minutes}min"
}

private fun formatLoad(value: Double): String = if (abs(value - value.toInt()) < 0.0001) "${value.toInt()} kg" else "${"%.1f".format(value)} kg"
