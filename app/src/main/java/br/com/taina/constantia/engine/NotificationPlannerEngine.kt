package br.com.taina.constantia.engine


data class NotificationInteractionStats(
    val samples: Int = 0,
    val opened: Int = 0,
    val dismissed: Int = 0
) {
    val openRate: Double get() = if (samples == 0) 0.0 else opened.toDouble() / samples
    val dismissRate: Double get() = if (samples == 0) 0.0 else dismissed.toDouble() / samples
}

data class NotificationPlanInput(
    val lunchMinuteOfDay: Int? = null,
    val workoutMinuteOfDay: Int? = null,
    val workoutPending: Boolean = false,
    val preferredStudyMinuteOfDay: Int? = null,
    val studyDue: Boolean = false,
    val dueQuestionPrompts: List<Pair<Long, String>> = emptyList(),
    val motivationalEnabled: Boolean = true,
    val workoutReminderEnabled: Boolean = true,
    val studyReminderEnabled: Boolean = true,
    val studyQuestionsEnabled: Boolean = true,
    val questionsPerDay: Int = 2,
    val maxNonEssential: Int = 3,
    val messageSeed: Int = 0,
    val stats: Map<String, NotificationInteractionStats> = emptyMap()
)

data class PlannedNotification(
    val type: String,
    val title: String,
    val body: String,
    val minuteOfDay: Int,
    val sourceRef: String = "",
    val essential: Boolean = false
)

class NotificationPlannerEngine {
    fun plan(input: NotificationPlanInput): List<PlannedNotification> {
        val items = mutableListOf<PlannedNotification>()
        val lunch = input.lunchMinuteOfDay ?: (12 * 60 + 30)
        val study = input.preferredStudyMinuteOfDay ?: (19 * 60)

        if (input.workoutReminderEnabled && input.workoutPending && input.workoutMinuteOfDay != null) {
            items += PlannedNotification(
                type = "PRE_WORKOUT",
                title = "Treino hoje",
                body = "Seu treino está se aproximando. Hoje basta começar.",
                minuteOfDay = (input.workoutMinuteOfDay - 40).coerceAtLeast(7 * 60),
                essential = true
            )
        }

        if (input.studyReminderEnabled && input.studyDue) {
            items += PlannedNotification(
                type = "STUDY_REMINDER",
                title = "Estudo planejado",
                body = "Você tem um bloco de estudo previsto para hoje.",
                minuteOfDay = (study - 20).coerceAtLeast(8 * 60)
            )
        }

        if (input.studyQuestionsEnabled) {
            val prompts = input.dueQuestionPrompts.take(input.questionsPerDay.coerceIn(0, 2))
            prompts.forEachIndexed { index, (id, prompt) ->
                val minute = if (index == 0) lunch + 20 else (study - 60).coerceAtLeast(lunch + 90)
                items += PlannedNotification(
                    type = "STUDY_QUESTION",
                    title = "Questão rápida",
                    body = prompt.take(180),
                    minuteOfDay = minute.coerceIn(7 * 60, 22 * 60),
                    sourceRef = "question:$id"
                )
            }
        }

        if (input.motivationalEnabled && Math.floorMod(input.messageSeed, 3) != 1) {
            val message = MotivationalMessageLibrary.forSeed(input.messageSeed + lunch)
            items += PlannedNotification(
                type = "MOTIVATION",
                title = message.title,
                body = message.body,
                minuteOfDay = lunch.coerceIn(7 * 60, 21 * 60),
                sourceRef = message.sourceRef
            )
        }

        val essential = items.filter { it.essential }
        val optional = items.filterNot { it.essential }
            .filterNot { shouldSuppress(it.type, input.stats[it.type]) }
            .distinctBy { it.type to it.minuteOfDay }
            .sortedBy { priority(it.type) }
            .take(input.maxNonEssential.coerceIn(1, 5))

        return (essential + optional)
            .distinctBy { it.type to it.minuteOfDay }
            .sortedBy { it.minuteOfDay }
    }

    private fun shouldSuppress(type: String, stats: NotificationInteractionStats?): Boolean {
        if (stats == null || stats.samples < 5) return false
        if (type == "PRE_WORKOUT") return false
        return stats.openRate < 0.20 && stats.dismissRate >= 0.50
    }

    private fun priority(type: String): Int = when (type) {
        "STUDY_QUESTION" -> 0
        "STUDY_REMINDER" -> 1
        "MOTIVATION" -> 2
        else -> 3
    }
}

data class MotivationalMessage(val title: String, val body: String, val sourceRef: String = "")

object MotivationalMessageLibrary {
    private val ownMessages = listOf(
        "Hoje basta começar.",
        "Constância vale mais do que um dia perfeito.",
        "Faça o próximo bloco; o resto vem depois.",
        "Pouco feito com regularidade ainda é progresso.",
        "A rotina melhora quando a ação vem antes da vontade.",
        "Termine o que importa antes de procurar distração."
    )

    fun forSeed(seed: Int): MotivationalMessage {
        return if (Math.floorMod(seed, 3) == 0) {
            val q = RomanQuoteLibrary.forSeed(seed)
            MotivationalMessage(
                title = "${q.author} · ${q.source}",
                body = "${q.portuguese} — ${q.original}",
                sourceRef = "quote:${q.author}:${q.source}"
            )
        } else {
            MotivationalMessage(
                title = "Constantia",
                body = ownMessages[Math.floorMod(seed, ownMessages.size)],
                sourceRef = "constantia:own"
            )
        }
    }
}
