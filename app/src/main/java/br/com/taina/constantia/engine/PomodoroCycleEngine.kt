package br.com.taina.constantia.engine

enum class PomodoroPhase { STUDY, BREAK }

data class PomodoroPhaseTransition(
    val finished: PomodoroPhase,
    val next: PomodoroPhase,
    val nextSeconds: Int,
    val autoStartNext: Boolean
)

class PomodoroCycleEngine {
    fun completePhase(
        phase: PomodoroPhase,
        studyMinutes: Int,
        breakMinutes: Int,
        autoStartBreak: Boolean
    ): PomodoroPhaseTransition {
        val studySeconds = studyMinutes.coerceIn(5, 120) * 60
        val breakSeconds = breakMinutes.coerceIn(1, 60) * 60
        return if (phase == PomodoroPhase.STUDY) {
            PomodoroPhaseTransition(phase, PomodoroPhase.BREAK, breakSeconds, autoStartBreak)
        } else {
            PomodoroPhaseTransition(phase, PomodoroPhase.STUDY, studySeconds, false)
        }
    }
}
