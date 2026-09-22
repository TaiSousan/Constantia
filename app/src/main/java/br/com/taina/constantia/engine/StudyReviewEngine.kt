package br.com.taina.constantia.engine

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Scheduler simples, determinístico e auditável para o MVP.
 * Não tenta modelar memória humana com falsa precisão. Os intervalos podem ser
 * substituídos futuramente por FSRS quando houver histórico suficiente.
 */
class StudyReviewEngine {
    enum class Rating { AGAIN, HARD, GOOD, EASY }

    data class ReviewDecision(
        val nextIntervalDays: Int,
        val reason: String
    )

    fun nextInterval(previousIntervalDays: Int, reviewCount: Int, rating: Rating): ReviewDecision {
        val previous = previousIntervalDays.coerceAtLeast(0)
        val next = if (reviewCount <= 0 || previous == 0) {
            when (rating) {
                Rating.AGAIN -> 1
                Rating.HARD -> 2
                Rating.GOOD -> 3
                Rating.EASY -> 5
            }
        } else {
            when (rating) {
                Rating.AGAIN -> 1
                Rating.HARD -> max(previous + 1, (previous * 1.2).roundToInt())
                Rating.GOOD -> max(previous + 1, (previous * 2.0).roundToInt())
                Rating.EASY -> max(previous + 2, (previous * 3.0).roundToInt())
            }
        }.coerceIn(1, 180)

        val reason = when (rating) {
            Rating.AGAIN -> "Resposta incorreta: revisão volta para amanhã."
            Rating.HARD -> "Acertou com dificuldade: intervalo cresce pouco."
            Rating.GOOD -> "Acertou: intervalo cresce de forma moderada."
            Rating.EASY -> "Acertou com facilidade: intervalo cresce mais."
        }
        return ReviewDecision(next, reason)
    }
}
