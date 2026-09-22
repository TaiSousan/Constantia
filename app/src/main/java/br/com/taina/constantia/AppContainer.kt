package br.com.taina.constantia

import android.content.Context
import br.com.taina.constantia.core.database.AppDatabase
import br.com.taina.constantia.core.preferences.AppPreferences
import br.com.taina.constantia.core.repository.ActivityRepository
import br.com.taina.constantia.core.repository.ProfileRepository
import br.com.taina.constantia.core.repository.TrainingRepository
import br.com.taina.constantia.core.repository.StudyRepository
import br.com.taina.constantia.core.repository.ContextualRepository
import br.com.taina.constantia.core.repository.NutritionRepository
import br.com.taina.constantia.core.repository.ProgressRepository
import br.com.taina.constantia.core.notifications.ContextualNotificationScheduler
import br.com.taina.constantia.core.notifications.NotificationCoordinator
import br.com.taina.constantia.core.focusgate.FocusGateController

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    val profileRepository = ProfileRepository(database.profileDao())
    val activityRepository = ActivityRepository(database.activityDao())
    val trainingRepository = TrainingRepository(database.trainingDao(), database.profileDao())
    val studyRepository = StudyRepository(database.studyDao())
    val contextualRepository = ContextualRepository(database.contextualDao(), database.trainingDao(), database.studyDao())
    val nutritionRepository = NutritionRepository(database.nutritionDao(), database.profileDao())
    val progressRepository = ProgressRepository(database.profileDao(), database.activityDao(), database.trainingDao(), database.studyDao())
    val focusGateController = FocusGateController(context, contextualRepository)
    val preferences = AppPreferences(context)
    val notificationCoordinator = NotificationCoordinator(
        profileRepository = profileRepository,
        trainingRepository = trainingRepository,
        studyRepository = studyRepository,
        contextualRepository = contextualRepository,
        preferences = preferences,
        scheduler = ContextualNotificationScheduler(context)
    )
}
