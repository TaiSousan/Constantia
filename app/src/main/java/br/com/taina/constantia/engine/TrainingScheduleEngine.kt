package br.com.taina.constantia.engine

import java.time.DayOfWeek

class TrainingScheduleEngine {
    fun resolveDays(daysPerWeek: Int, csv: String): List<DayOfWeek> {
        val explicit = csv.split(',').mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }.distinct().map { DayOfWeek.of(it) }.sortedBy { it.value }
        if (explicit.isNotEmpty()) return explicit
        return when (daysPerWeek.coerceIn(1, 7)) {
            1 -> listOf(DayOfWeek.WEDNESDAY)
            2 -> listOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)
            3 -> listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            4 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            5 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            6 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
            else -> DayOfWeek.values().toList()
        }
    }
}
