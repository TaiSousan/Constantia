import java.time.DayOfWeek
import java.time.LocalDate

enum class FrequencyType { DAILY, SPECIFIC_DAYS, TIMES_PER_WEEK, TIMES_PER_MONTH, EVERY_X_DAYS, ONCE }

data class Definition(val type: FrequencyType, val anchor: LocalDate, val days: Set<DayOfWeek> = emptySet(), val everyX: Int = 1, val target: Int = 1)

fun due(d: Definition, date: LocalDate, completedInPeriod: Int = 0): Boolean = when (d.type) {
    FrequencyType.DAILY -> !date.isBefore(d.anchor)
    FrequencyType.SPECIFIC_DAYS -> !date.isBefore(d.anchor) && date.dayOfWeek in d.days
    FrequencyType.TIMES_PER_WEEK, FrequencyType.TIMES_PER_MONTH -> completedInPeriod < d.target
    FrequencyType.EVERY_X_DAYS -> !date.isBefore(d.anchor) && (date.toEpochDay() - d.anchor.toEpochDay()) % d.everyX.coerceAtLeast(1) == 0L
    FrequencyType.ONCE -> date == d.anchor
}

fun main() {
    val monday = LocalDate.of(2026, 9, 21)
    check(due(Definition(FrequencyType.DAILY, monday), monday))
    check(due(Definition(FrequencyType.SPECIFIC_DAYS, monday, setOf(DayOfWeek.MONDAY)), monday))
    check(!due(Definition(FrequencyType.TIMES_PER_WEEK, monday, target = 3), monday, completedInPeriod = 3))
    check(due(Definition(FrequencyType.EVERY_X_DAYS, monday, everyX = 2), monday.plusDays(2)))
    println("ActivityScheduleSpec OK")
}
