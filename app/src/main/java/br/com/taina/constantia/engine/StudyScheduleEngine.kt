package br.com.taina.constantia.engine

import kotlin.math.ceil

class StudyScheduleEngine {
    /**
     * Meta semanal flexível. A carga esperada cresce ao longo da semana.
     * Ex.: 3x/semana tende a pedir sessões em torno de seg/qua/sex, mas uma
     * sessão perdida permanece recuperável nos dias seguintes.
     */
    fun isDueToday(sessionsPerWeek: Int, completedThisWeek: Int, dayOfWeekValue: Int): Boolean {
        val target = sessionsPerWeek.coerceIn(1, 7)
        val day = dayOfWeekValue.coerceIn(1, 7)
        val expectedByToday = ceil(target * day / 7.0).toInt()
        return completedThisWeek < expectedByToday && completedThisWeek < target
    }
}
