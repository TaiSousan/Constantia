package br.com.taina.constantia.engine

import br.com.taina.constantia.core.database.ActivityDefinitionEntity
import br.com.taina.constantia.core.database.ActivityOccurrenceEntity
import br.com.taina.constantia.core.model.FrequencyType
import br.com.taina.constantia.core.model.OccurrenceStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class ActivityScheduleEngine {
    fun isDueToday(
        definition: ActivityDefinitionEntity,
        date: LocalDate,
        periodOccurrences: List<ActivityOccurrenceEntity>
    ): Boolean {
        if (!definition.active) return false
        val type = runCatching { FrequencyType.valueOf(definition.frequencyType) }.getOrNull() ?: return false
        val completed = periodOccurrences.count { it.status == OccurrenceStatus.COMPLETED.name }

        return when (type) {
            FrequencyType.DAILY -> date.toEpochDay() >= definition.anchorEpochDay
            FrequencyType.SPECIFIC_DAYS -> {
                val allowed = definition.specificDaysCsv.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
                date.dayOfWeek.value in allowed && date.toEpochDay() >= definition.anchorEpochDay
            }
            FrequencyType.TIMES_PER_WEEK -> completed < definition.timesPerPeriod
            FrequencyType.TIMES_PER_MONTH -> completed < definition.timesPerPeriod
            FrequencyType.EVERY_X_DAYS -> {
                val delta = date.toEpochDay() - definition.anchorEpochDay
                delta >= 0 && delta % definition.everyXDays.coerceAtLeast(1) == 0L
            }
            FrequencyType.ONCE -> date.toEpochDay() == definition.anchorEpochDay
        }
    }

    fun periodBounds(type: FrequencyType, date: LocalDate): Pair<Long, Long> = when (type) {
        FrequencyType.TIMES_PER_WEEK -> {
            val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            start.toEpochDay() to start.plusDays(6).toEpochDay()
        }
        FrequencyType.TIMES_PER_MONTH -> {
            val start = date.withDayOfMonth(1)
            start.toEpochDay() to start.with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
        }
        else -> date.toEpochDay() to date.toEpochDay()
    }
}
