package br.com.taina.constantia.engine

import java.time.DayOfWeek
import kotlin.math.pow

class TrainingScheduleEngine {
    /** Dias que a pessoa declarou como disponíveis/preferidos. */
    fun resolveDays(daysPerWeek: Int, csv: String): List<DayOfWeek> {
        val explicit = parseExplicit(csv)
        if (explicit.isNotEmpty()) return explicit
        return fallback(daysPerWeek)
    }

    /**
     * Escolhe, dentre os dias disponíveis, apenas a quantidade de sessões que
     * realmente existe na ficha. Quando há mais disponibilidade que sessões,
     * prefere uma distribuição mais uniforme ao longo da semana.
     */
    fun resolveScheduledDays(sessionCount: Int, csv: String): List<DayOfWeek> {
        val count = sessionCount.coerceIn(1, 7)
        val available = parseExplicit(csv).ifEmpty { fallback(count) }
        if (available.size <= count) return available
        return combinations(available, count)
            .minWithOrNull(compareBy<List<DayOfWeek>> { spacingPenalty(it) }.thenBy { it.joinToString(",") { d -> d.value.toString() } })
            ?: available.take(count)
    }

    private fun parseExplicit(csv: String): List<DayOfWeek> = csv.split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..7 }
        .distinct()
        .map { DayOfWeek.of(it) }
        .sortedBy { it.value }

    private fun fallback(daysPerWeek: Int): List<DayOfWeek> = when (daysPerWeek.coerceIn(1, 7)) {
        1 -> listOf(DayOfWeek.WEDNESDAY)
        2 -> listOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)
        3 -> listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        4 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        5 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
        6 -> listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
        else -> DayOfWeek.values().toList()
    }

    private fun combinations(items: List<DayOfWeek>, count: Int): List<List<DayOfWeek>> {
        val result = mutableListOf<List<DayOfWeek>>()
        fun visit(start: Int, chosen: MutableList<DayOfWeek>) {
            if (chosen.size == count) {
                result += chosen.toList()
                return
            }
            for (i in start until items.size) {
                chosen += items[i]
                visit(i + 1, chosen)
                chosen.removeAt(chosen.lastIndex)
            }
        }
        visit(0, mutableListOf())
        return result
    }

    private fun spacingPenalty(days: List<DayOfWeek>): Double {
        val values = days.map { it.value }.sorted()
        val gaps = values.indices.map { i ->
            val current = values[i]
            val next = if (i == values.lastIndex) values.first() + 7 else values[i + 1]
            next - current
        }
        val ideal = 7.0 / values.size
        val unevenness = gaps.sumOf { (it - ideal).pow(2) }

        // Com 5+ sessões nem sempre é possível evitar 3 dias seguidos, mas é
        // possível evitar blocos ainda maiores quando há disponibilidade extra.
        // Penalizamos fortemente sequências longas e só depois usamos a
        // uniformidade dos intervalos como critério de desempate.
        val longestRun = longestCyclicConsecutiveRun(values)
        val longRunPenalty = (longestRun - 2).coerceAtLeast(0) * 100.0
        return longRunPenalty + unevenness
    }

    private fun longestCyclicConsecutiveRun(values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val selected = values.toSet()
        var best = 1
        for (start in 1..7) {
            if (start !in selected) continue
            var run = 1
            var day = start
            while (run < selected.size) {
                day = if (day == 7) 1 else day + 1
                if (day !in selected) break
                run += 1
            }
            best = maxOf(best, run)
        }
        return best
    }
}
