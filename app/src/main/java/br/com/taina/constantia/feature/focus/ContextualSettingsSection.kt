@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.taina.constantia.feature.focus

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.com.taina.constantia.core.focusgate.FocusGateVpnPhase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextualSettingsSection(viewModel: ContextSettingsViewModel) {
    val prefs by viewModel.preferencesState.collectAsState()
    val rules by viewModel.gateRules.collectAsState()
    val statuses by viewModel.gateStatuses.collectAsState()
    val vpnStatus by viewModel.vpnStatus.collectAsState()
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
        if (it) viewModel.replanNow()
    }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.onVpnPermissionResult(result.resultCode == Activity.RESULT_OK)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider()
        Text("Rotina contextual", style = MaterialTheme.typography.titleLarge)
        Text(
            "Lembretes de treino e estudo, questões rápidas e frases em horários úteis. O app reduz mensagens opcionais que você ignora repetidamente.",
            style = MaterialTheme.typography.bodySmall
        )

        if (!permissionGranted && Build.VERSION.SDK_INT >= 33) {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Permita notificações para usar os lembretes.", Modifier.weight(1f))
                    Button(onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Permitir") }
                }
            }
        }

        SettingsSwitch("Notificações contextuais", prefs.contextualNotifications, viewModel::setMaster)
        if (prefs.contextualNotifications) {
            SettingsSwitch("Antes do treino", prefs.workoutReminders, viewModel::setWorkoutReminders)
            SettingsSwitch("Lembretes de estudo", prefs.studyReminders, viewModel::setStudyReminders)
            SettingsSwitch("Frases motivacionais", prefs.motivationalNotifications, viewModel::setMotivation)
            SettingsSwitch("Questões de revisão", prefs.studyQuestionNotifications, viewModel::setQuestionNotifications)
            if (prefs.studyQuestionNotifications) {
                CounterRow("Questões por dia", prefs.studyQuestionsPerDay, 0, 2, viewModel::setQuestionCount)
            }
            CounterRow("Máx. notificações opcionais/dia", prefs.maxNonEssentialNotificationsPerDay, 1, 5, viewModel::setMaxNotifications)
            OutlinedButton(onClick = viewModel::replanNow) { Text("Recalcular horários") }
        }

        HorizontalDivider()
        Text("Portão de foco", style = MaterialTheme.typography.titleLarge)
        Text(
            "Quando uma regra estiver bloqueada, o Constantia pode criar uma VPN local apenas para os apps selecionados e descartar o tráfego deles. Seus dados não são enviados a um servidor VPN.",
            style = MaterialTheme.typography.bodySmall
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bloqueio técnico", style = MaterialTheme.typography.titleMedium)
                Text(vpnStatus.message, style = MaterialTheme.typography.bodySmall)
                if (vpnStatus.blockedLabels.isNotEmpty()) {
                    Text("Apps bloqueados: ${vpnStatus.blockedLabels.joinToString()}", style = MaterialTheme.typography.bodySmall)
                }
                if (vpnStatus.missingPackages.isNotEmpty()) {
                    Text("Não encontrados no aparelho: ${vpnStatus.missingPackages.joinToString()}", style = MaterialTheme.typography.bodySmall)
                }
                if (vpnStatus.phase == FocusGateVpnPhase.VPN_CONFLICT) {
                    Text(
                        "O Android permite uma VPN preparada por vez. Se você continuar, a autorização da VPN atual pode ser substituída pelo Constantia.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (vpnStatus.phase == FocusGateVpnPhase.PERMISSION_REQUIRED || vpnStatus.phase == FocusGateVpnPhase.VPN_CONFLICT) {
                    Button(onClick = {
                        val intent = viewModel.vpnPermissionIntent()
                        if (intent != null) vpnPermissionLauncher.launch(intent) else viewModel.refreshFocusGate()
                    }) {
                        Text(if (vpnStatus.phase == FocusGateVpnPhase.VPN_CONFLICT) "Continuar e autorizar" else "Autorizar bloqueio")
                    }
                } else {
                    OutlinedButton(onClick = viewModel::refreshFocusGate) { Text("Reavaliar agora") }
                }
            }
        }

        rules.forEach { rule ->
            val status = statuses.firstOrNull { it.rule.id == rule.id }
            var expanded by remember(rule.id) { mutableStateOf(false) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(rule.appLabel, style = MaterialTheme.typography.titleMedium)
                            Text(rule.packageName, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = rule.active, onCheckedChange = { viewModel.setGateActive(rule.id, it) })
                    }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = if (rule.conditionType == "FOCUS_SESSIONS_TODAY") "${rule.threshold} blocos de foco" else "Treino do dia",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Libera quando") },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Treino do dia concluído") }, onClick = {
                                viewModel.setGateCondition(rule.id, "WORKOUT_TODAY", 1); expanded = false
                            })
                            DropdownMenuItem(text = { Text("2 blocos de foco concluídos") }, onClick = {
                                viewModel.setGateCondition(rule.id, "FOCUS_SESSIONS_TODAY", 2); expanded = false
                            })
                        }
                    }
                    if (rule.active && status != null) {
                        Text(
                            if (status.decision.unlocked) "Liberado · ${status.decision.reason}" else "Bloqueado · ${status.decision.reason}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!status.decision.unlocked) {
                            TextButton(onClick = { viewModel.temporaryOverride(rule.id, rule.emergencyMinutes) }) {
                                Text("Exceção por ${rule.emergencyMinutes} min")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun CounterRow(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f))
        OutlinedButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }, enabled = value > min) { Text("−") }
        Text(value.toString())
        OutlinedButton(onClick = { onChange((value + 1).coerceAtMost(max)) }, enabled = value < max) { Text("+") }
    }
}
