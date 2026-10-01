package br.com.taina.constantia.core.notifications

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object PomodoroAlertPlayer {
    fun signal(context: Context, sound: Boolean, vibration: Boolean, studyFinished: Boolean) {
        if (sound) {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            tone.startTone(if (studyFinished) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP, 650)
            android.os.Handler(context.mainLooper).postDelayed({ tone.release() }, 900)
        }
        if (vibration) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createOneShot(220, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
