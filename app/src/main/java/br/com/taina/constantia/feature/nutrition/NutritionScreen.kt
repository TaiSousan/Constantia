@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.taina.constantia.feature.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.taina.constantia.core.database.FoodEntity
import java.util.Locale

@Composable
fun NutritionScreen(viewModel: NutritionViewModel) {
    val foods by viewModel.foods.collectAsState()
    val day by viewModel.day.collectAsState()
    val weights by viewModel.weights.collectAsState()
    val sevenDayAverage by viewModel.sevenDayAverage.collectAsState()
    val mealDraft by viewModel.mealDraft.collectAsState()

    var showAddFood by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }
    var showWeight by remember { mutableStateOf(false) }
    var showCustomFood by remember { mutableStateOf(false) }
    var showDescribeMeal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Alimentação") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddFood = true }) {
                Icon(Icons.Default.Add, contentDescription = "Registrar alimento")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Hoje", style = MaterialTheme.typography.titleMedium)
                        val target = day.goal?.dailyCalories
                        Text(
                            if (target != null) "${fmt(day.totals.kcal)} / $target kcal"
                            else "${fmt(day.totals.kcal)} kcal registradas"
                        )
                        Text(
                            "Proteína ${fmt(day.totals.proteinGrams)} g · Carboidratos ${fmt(day.totals.carbsGrams)} g · Gorduras ${fmt(day.totals.fatGrams)} g",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { showDescribeMeal = true }) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(4.dp)); Text("Descrever refeição") }
                            TextButton(onClick = { showGoal = true }) { Text("Metas") }
                        }
                        TextButton(onClick = { showCustomFood = true }) { Text("Cadastrar alimento do rótulo") }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonitorWeight, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Peso", style = MaterialTheme.typography.titleMedium)
                        }
                        val latest = weights.firstOrNull()?.weightKg
                        Text(if (latest != null) "Último registro: ${fmt(latest)} kg" else "Ainda sem registro de peso")
                        sevenDayAverage?.let { Text("Média móvel de 7 dias: ${fmt(it)} kg", style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { showWeight = true }) { Text("Registrar peso") }
                    }
                }
            }

            item { Text("Refeições de hoje", style = MaterialTheme.typography.titleMedium) }
            if (day.entries.isEmpty()) {
                item { Text("Nenhum alimento registrado hoje.", style = MaterialTheme.typography.bodyMedium) }
            } else {
                day.mealGroups.forEach { group ->
                    item(key = "meal-${group.meal.id}") {
                        Text(group.meal.mealType, style = MaterialTheme.typography.labelLarge)
                    }
                    items(group.entries, key = { it.id }) { entry ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(entry.foodNameSnapshot, style = MaterialTheme.typography.bodyLarge)
                                Text("${fmt(entry.amountValue)} ${entry.amountUnit} · ${fmt(entry.estimatedGrams)} g estimados")
                                Text("${fmt(entry.kcal)} kcal · P ${fmt(entry.proteinGrams)} · C ${fmt(entry.carbsGrams)} · G ${fmt(entry.fatGrams)}", style = MaterialTheme.typography.bodySmall)
                                Text("Confiança: ${confidenceLabel(entry.confidence)}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }


    if (showDescribeMeal) DescribeMealDialog(
        draft = mealDraft,
        onDismiss = { viewModel.clearMealDraft(); showDescribeMeal = false },
        onAnalyze = viewModel::parseMealDescription,
        onSave = { mealType -> viewModel.saveMealDraft(mealType); showDescribeMeal = false }
    )

    if (showAddFood) AddFoodDialog(foods, onDismiss = { showAddFood = false }) { food, mealType, amount, unitMode ->
        viewModel.addFood(food, mealType, amount, unitMode)
        showAddFood = false
    }

    if (showGoal) GoalDialog(
        currentCalories = day.goal?.dailyCalories,
        currentProtein = day.goal?.dailyProteinGrams,
        currentCarbs = day.goal?.dailyCarbsGrams,
        currentFat = day.goal?.dailyFatGrams,
        onDismiss = { showGoal = false }
    ) { kcal, protein, carbs, fat ->
        viewModel.saveGoal(kcal, protein, carbs, fat)
        showGoal = false
    }

    if (showWeight) NumberDialog(
        title = "Registrar peso",
        label = "Peso (kg)",
        onDismiss = { showWeight = false }
    ) { value ->
        viewModel.addWeight(value)
        showWeight = false
    }

    if (showCustomFood) CustomFoodDialog(
        onDismiss = { showCustomFood = false },
        onSave = { name, kcal, p, c, f, measure, grams ->
            viewModel.addCustomFood(name, kcal, p, c, f, measure, grams)
            showCustomFood = false
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DescribeMealDialog(
    draft: br.com.taina.constantia.engine.MealParseResult?,
    onDismiss: () -> Unit,
    onAnalyze: (String) -> Unit,
    onSave: (String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf("ALMOÇO") }
    var mealMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Descrever refeição") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ex.: 4 colheres de arroz, 1 filé de frango e 1 ovo. Revise antes de salvar.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("O que você comeu?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = mealMenu, onExpandedChange = { mealMenu = !mealMenu }) {
                    OutlinedTextField(mealType, {}, readOnly = true, label = { Text("Refeição") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded = mealMenu, onDismissRequest = { mealMenu = false }) {
                        listOf("CAFÉ DA MANHÃ", "LANCHE", "ALMOÇO", "JANTAR", "CEIA").forEach { item ->
                            DropdownMenuItem(text = { Text(item) }, onClick = { mealType = item; mealMenu = false })
                        }
                    }
                }
                if (draft == null) {
                    Text("A interpretação desta versão é local e reconhece apenas alimentos já cadastrados no Constantia.", style = MaterialTheme.typography.labelSmall)
                } else {
                    if (draft.items.isEmpty()) Text("Nenhum alimento conhecido foi reconhecido.")
                    draft.items.forEach { item ->
                        Text("• ${item.food.name}: ${item.displayAmount} · ~${fmt(item.estimatedGrams)} g · ${confidenceLabel(item.confidence.name)}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (draft.items.isNotEmpty()) {
                        Text("Estimativa: ${fmt(draft.range.minKcal)}–${fmt(draft.range.maxKcal)} kcal", style = MaterialTheme.typography.labelLarge)
                    }
                    if (draft.unmatchedFragments.isNotEmpty()) {
                        Text("Não reconhecido: ${draft.unmatchedFragments.joinToString("; ")}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (draft?.items?.isNotEmpty() == true) {
                Button(onClick = { onSave(mealType) }) { Text("Salvar itens reconhecidos") }
            } else {
                Button(onClick = { onAnalyze(description) }, enabled = description.isNotBlank()) { Text("Interpretar") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AddFoodDialog(
    foods: List<FoodEntity>,
    onDismiss: () -> Unit,
    onSave: (FoodEntity, String, Double, String) -> Unit
) {
    var selectedFood by remember(foods) { mutableStateOf(foods.firstOrNull()) }
    var foodMenu by remember { mutableStateOf(false) }
    var mealType by remember { mutableStateOf("ALMOÇO") }
    var mealMenu by remember { mutableStateOf(false) }
    var unitMode by remember { mutableStateOf("GRAMS") }
    var unitMenu by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }

    val food = selectedFood
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar alimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = foodMenu, onExpandedChange = { foodMenu = !foodMenu }) {
                    OutlinedTextField(
                        value = food?.name ?: "Sem alimentos",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Alimento") },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = foodMenu, onDismissRequest = { foodMenu = false }) {
                        foods.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { selectedFood = item; foodMenu = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = mealMenu, onExpandedChange = { mealMenu = !mealMenu }) {
                    OutlinedTextField(mealType, {}, readOnly = true, label = { Text("Refeição") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded = mealMenu, onDismissRequest = { mealMenu = false }) {
                        listOf("CAFÉ DA MANHÃ", "LANCHE", "ALMOÇO", "JANTAR", "CEIA").forEach { item ->
                            DropdownMenuItem(text = { Text(item) }, onClick = { mealType = item; mealMenu = false })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = unitMenu, onExpandedChange = { unitMenu = !unitMenu }) {
                    OutlinedTextField(
                        value = when (unitMode) { "GRAMS" -> "gramas"; "MEASURE" -> food?.defaultMeasureName ?: "medida caseira"; else -> "estimativa em gramas" },
                        onValueChange = {}, readOnly = true, label = { Text("Como medir") }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = unitMenu, onDismissRequest = { unitMenu = false }) {
                        DropdownMenuItem(text = { Text("gramas (maior confiança)") }, onClick = { unitMode = "GRAMS"; unitMenu = false })
                        if (food != null) DropdownMenuItem(text = { Text(food.defaultMeasureName) }, onClick = { unitMode = "MEASURE"; unitMenu = false })
                        DropdownMenuItem(text = { Text("estimativa em gramas") }, onClick = { unitMode = "ESTIMATE"; unitMenu = false })
                    }
                }
                OutlinedTextField(amount, { amount = it.replace(',', '.') }, label = { Text(if (unitMode == "MEASURE") "Quantidade de porções" else "Quantidade") }, singleLine = true)
                food?.let {
                    Text("Referência: ${it.sourceLabel} ${it.sourceCode} · ${fmt(it.kcalPer100g)} kcal/100 g", style = MaterialTheme.typography.bodySmall)
                    if (unitMode == "MEASURE") Text("1 ${it.defaultMeasureName} ≈ ${fmt(it.defaultMeasureGrams)} g", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { food?.let { onSave(it, mealType, amount.toDoubleOrNull() ?: 0.0, unitMode) } },
                enabled = food != null && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun GoalDialog(
    currentCalories: Int?, currentProtein: Int?, currentCarbs: Int?, currentFat: Int?,
    onDismiss: () -> Unit,
    onSave: (Int?, Int?, Int?, Int?) -> Unit
) {
    var kcal by remember { mutableStateOf(currentCalories?.toString() ?: "") }
    var protein by remember { mutableStateOf(currentProtein?.toString() ?: "") }
    var carbs by remember { mutableStateOf(currentCarbs?.toString() ?: "") }
    var fat by remember { mutableStateOf(currentFat?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Metas diárias") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Deixe em branco o que você não quiser acompanhar como meta.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(kcal, { kcal = it.filter(Char::isDigit) }, label = { Text("Calorias (kcal)") }, singleLine = true)
                OutlinedTextField(protein, { protein = it.filter(Char::isDigit) }, label = { Text("Proteína (g)") }, singleLine = true)
                OutlinedTextField(carbs, { carbs = it.filter(Char::isDigit) }, label = { Text("Carboidratos (g)") }, singleLine = true)
                OutlinedTextField(fat, { fat = it.filter(Char::isDigit) }, label = { Text("Gorduras (g)") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onSave(kcal.toIntOrNull(), protein.toIntOrNull(), carbs.toIntOrNull(), fat.toIntOrNull()) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun NumberDialog(title: String, label: String, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it.replace(',', '.') }, label = { Text(label) }, singleLine = true) },
        confirmButton = { Button(onClick = { onSave(value.toDoubleOrNull() ?: 0.0) }, enabled = (value.toDoubleOrNull() ?: 0.0) > 0) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun CustomFoodDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, Double, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var measure by remember { mutableStateOf("porção") }
    var grams by remember { mutableStateOf("100") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo alimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Use os valores do rótulo ou de uma fonte confiável, sempre por 100 g.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(kcal, { kcal = it.replace(',', '.') }, label = { Text("kcal / 100 g") }, singleLine = true)
                OutlinedTextField(protein, { protein = it.replace(',', '.') }, label = { Text("Proteína / 100 g") }, singleLine = true)
                OutlinedTextField(carbs, { carbs = it.replace(',', '.') }, label = { Text("Carboidratos / 100 g") }, singleLine = true)
                OutlinedTextField(fat, { fat = it.replace(',', '.') }, label = { Text("Gorduras / 100 g") }, singleLine = true)
                OutlinedTextField(measure, { measure = it }, label = { Text("Nome da medida caseira") }, singleLine = true)
                OutlinedTextField(grams, { grams = it.replace(',', '.') }, label = { Text("Gramas nessa medida") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, kcal.toDoubleOrNull() ?: 0.0, protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0, measure, grams.toDoubleOrNull() ?: 100.0) },
                enabled = name.isNotBlank() && (grams.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun fmt(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)
private fun confidenceLabel(value: String): String = when (value) {
    "HIGH" -> "alta (peso informado)"
    "MEDIUM" -> "média (medida caseira)"
    else -> "baixa (estimativa)"
}
