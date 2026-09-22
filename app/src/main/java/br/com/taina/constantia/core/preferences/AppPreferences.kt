package br.com.taina.constantia.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "constantia_preferences")

data class PreferencesState(
    val defaultPomodoroMinutes: Int = 25,
    val defaultBreakMinutes: Int = 5,
    val contextualNotifications: Boolean = true,
    val motivationalNotifications: Boolean = true,
    val workoutReminders: Boolean = true,
    val studyReminders: Boolean = true,
    val studyQuestionNotifications: Boolean = true,
    val studyQuestionsPerDay: Int = 2,
    val maxNonEssentialNotificationsPerDay: Int = 3
)

class AppPreferences(private val context: Context) {
    private object Keys {
        val pomodoro = intPreferencesKey("pomodoro_minutes")
        val breakMinutes = intPreferencesKey("break_minutes")
        val contextual = booleanPreferencesKey("contextual_notifications")
        val motivational = booleanPreferencesKey("motivational_notifications")
        val workoutReminders = booleanPreferencesKey("workout_reminders")
        val studyReminders = booleanPreferencesKey("study_reminders")
        val studyQuestionNotifications = booleanPreferencesKey("study_question_notifications")
        val studyQuestions = intPreferencesKey("study_questions_per_day")
        val maxNotifications = intPreferencesKey("max_nonessential_notifications_per_day")
    }

    val state: Flow<PreferencesState> = context.dataStore.data.map { prefs ->
        PreferencesState(
            defaultPomodoroMinutes = prefs[Keys.pomodoro] ?: 25,
            defaultBreakMinutes = prefs[Keys.breakMinutes] ?: 5,
            contextualNotifications = prefs[Keys.contextual] ?: true,
            motivationalNotifications = prefs[Keys.motivational] ?: true,
            workoutReminders = prefs[Keys.workoutReminders] ?: true,
            studyReminders = prefs[Keys.studyReminders] ?: true,
            studyQuestionNotifications = prefs[Keys.studyQuestionNotifications] ?: true,
            studyQuestionsPerDay = (prefs[Keys.studyQuestions] ?: 2).coerceIn(0, 2),
            maxNonEssentialNotificationsPerDay = (prefs[Keys.maxNotifications] ?: 3).coerceIn(1, 5)
        )
    }

    suspend fun setPomodoro(minutes: Int, breakMinutes: Int) {
        context.dataStore.edit {
            it[Keys.pomodoro] = minutes.coerceIn(5, 120)
            it[Keys.breakMinutes] = breakMinutes.coerceIn(1, 60)
        }
    }

    suspend fun setNotificationMaster(enabled: Boolean) = context.dataStore.edit { it[Keys.contextual] = enabled }
    suspend fun setMotivationalNotifications(enabled: Boolean) = context.dataStore.edit { it[Keys.motivational] = enabled }
    suspend fun setWorkoutReminders(enabled: Boolean) = context.dataStore.edit { it[Keys.workoutReminders] = enabled }
    suspend fun setStudyReminders(enabled: Boolean) = context.dataStore.edit { it[Keys.studyReminders] = enabled }
    suspend fun setStudyQuestionNotifications(enabled: Boolean) = context.dataStore.edit { it[Keys.studyQuestionNotifications] = enabled }
    suspend fun setStudyQuestionsPerDay(value: Int) = context.dataStore.edit { it[Keys.studyQuestions] = value.coerceIn(0, 2) }
    suspend fun setMaxNonEssentialNotifications(value: Int) = context.dataStore.edit { it[Keys.maxNotifications] = value.coerceIn(1, 5) }
}
