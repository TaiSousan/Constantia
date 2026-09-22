package br.com.taina.constantia.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val ACTION = "constantia_action"
    const val STUDY = "constantia_study"
    const val MOTIVATION = "constantia_motivation"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(ACTION, "Ações e treino", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(STUDY, "Estudo e revisões", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(MOTIVATION, "Motivação", NotificationManager.IMPORTANCE_LOW)
            )
        )
    }

    fun channelFor(type: String): String = when (type) {
        "STUDY_QUESTION", "STUDY_REMINDER" -> STUDY
        "MOTIVATION" -> MOTIVATION
        else -> ACTION
    }
}
