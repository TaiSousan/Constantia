package br.com.taina.constantia.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import br.com.taina.constantia.AppContainer
import br.com.taina.constantia.feature.onboarding.OnboardingScreen
import br.com.taina.constantia.feature.onboarding.OnboardingViewModel
import br.com.taina.constantia.feature.today.TodayScreen
import br.com.taina.constantia.feature.today.TodayViewModel
import br.com.taina.constantia.feature.training.TrainingScreen
import br.com.taina.constantia.feature.training.TrainingViewModel
import br.com.taina.constantia.feature.focus.FocusScreen
import br.com.taina.constantia.feature.focus.FocusViewModel
import br.com.taina.constantia.feature.focus.ContextSettingsViewModel
import br.com.taina.constantia.feature.nutrition.NutritionScreen
import br.com.taina.constantia.feature.nutrition.NutritionViewModel
import br.com.taina.constantia.feature.progress.ProgressScreen
import br.com.taina.constantia.feature.progress.ProgressViewModel

enum class MainDestination(val route: String, val label: String, val icon: ImageVector) {
    Today("today", "Hoje", Icons.Default.Today),
    Training("training", "Treino", Icons.Default.FitnessCenter),
    Focus("focus", "Foco", Icons.Default.Timer),
    Nutrition("nutrition", "Alimentação", Icons.Default.Restaurant),
    Progress("progress", "Progresso", Icons.Default.ShowChart)
}

@Composable
fun ConstantiaApp(container: AppContainer) {
    MaterialTheme {
        var profileLoaded by remember { mutableStateOf(false) }
        var profile by remember { mutableStateOf<br.com.taina.constantia.core.database.UserProfileEntity?>(null) }
        var onboardingFinishedThisSession by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            container.profileRepository.profile.collect { value ->
                profile = value
                profileLoaded = true
            }
        }

        when {
            !profileLoaded -> CircularProgressIndicator()
            profile?.onboardingCompleted != true && !onboardingFinishedThisSession -> {
                val vm: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory(
                    container.profileRepository,
                    container.studyRepository,
                    container.nutritionRepository,
                    container.activityRepository
                ))
                OnboardingScreen(vm) { onboardingFinishedThisSession = true }
            }
            else -> MainShell(container)
        }
    }
}

@Composable
private fun MainShell(container: AppContainer) {
    val navController = rememberNavController()
    val destinations = MainDestination.entries
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = route == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = MainDestination.Today.route, modifier = Modifier.padding(padding)) {
            composable(MainDestination.Today.route) {
                val vm: TodayViewModel = viewModel(factory = TodayViewModel.Factory(container.activityRepository, container.trainingRepository, container.studyRepository, container.nutritionRepository))
                TodayScreen(
                    viewModel = vm,
                    onOpenTraining = { navController.navigate(MainDestination.Training.route) { launchSingleTop = true } },
                    onOpenFocus = { navController.navigate(MainDestination.Focus.route) { launchSingleTop = true } },
                    onOpenNutrition = { navController.navigate(MainDestination.Nutrition.route) { launchSingleTop = true } }
                )
            }
            composable(MainDestination.Training.route) {
                val vm: TrainingViewModel = viewModel(factory = TrainingViewModel.Factory(container.trainingRepository, container.focusGateController))
                TrainingScreen(vm)
            }
            composable(MainDestination.Focus.route) {
                val vm: FocusViewModel = viewModel(factory = FocusViewModel.Factory(container.studyRepository, container.focusGateController))
                val contextVm: ContextSettingsViewModel = viewModel(
                    factory = ContextSettingsViewModel.Factory(
                        container.preferences,
                        container.contextualRepository,
                        container.notificationCoordinator,
                        container.focusGateController
                    )
                )
                FocusScreen(vm, contextVm)
            }
            composable(MainDestination.Nutrition.route) {
                val vm: NutritionViewModel = viewModel(factory = NutritionViewModel.Factory(container.nutritionRepository))
                NutritionScreen(vm)
            }
            composable(MainDestination.Progress.route) {
                val vm: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory(container.progressRepository))
                ProgressScreen(vm)
            }
        }
    }
}
