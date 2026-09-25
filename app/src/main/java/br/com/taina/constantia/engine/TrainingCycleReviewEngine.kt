package br.com.taina.constantia.engine

enum class TrainingCycleReviewStatus { INSUFFICIENT_DATA, KEEP, REVIEW }

data class MuscleRecoveryLoad(
    val muscleCode: String,
    val muscleName: String,
    val recentEffectiveSets: Double,
    val previousEffectiveSets: Double,
    val hardEffectiveSetsLast72h: Double,
    val elevated: Boolean
)

data class TrainingCycleSignals(
    val daysObserved: Int,
    val completedSessions: Int,
    val plannedSessions: Int,
    val currentPlanSessionsPerWeek: Int,
    val desiredSessionsPerWeek: Int,
    val availableDaysPerWeek: Int,
    val currentSessionMinutes: Int,
    val planSessionMinutes: Int?,
    val progressingExerciseCount: Int,
    val plateauExerciseCodes: Set<String>,
    val watchExerciseCodes: Set<String>,
    val unavailableExerciseCodes: Set<String>,
    val recoveryLoads: List<MuscleRecoveryLoad>
)

data class TrainingCycleReview(
    val status: TrainingCycleReviewStatus,
    val title: String,
    val summary: String,
    val reasons: List<String>,
    val adherencePercent: Int?,
    val recommendedSessionsPerWeek: Int,
    val rotateExerciseCodes: Set<String>,
    val recoveryLoads: List<MuscleRecoveryLoad>,
    val progressingExerciseCount: Int,
    val plateauExerciseCount: Int,
    val canBuildProposal: Boolean
)

/**
 * Revisor conservador de ciclo.
 *
 * Ele não diagnostica "fadiga muscular" e não muda a ficha sozinho.
 * "Carga de recuperação" é somente uma heurística interna baseada no histórico
 * do próprio usuário: séries efetivas recentes, RIR, recência e desempenho.
 */
class TrainingCycleReviewEngine {
    fun evaluate(signals: TrainingCycleSignals): TrainingCycleReview {
        val adherence = if (signals.plannedSessions > 0) {
            signals.completedSessions.coerceAtMost(signals.plannedSessions).toDouble() / signals.plannedSessions
        } else null
        val adherencePercent = adherence?.times(100)?.toInt()

        val available = signals.availableDaysPerWeek.coerceIn(2, 7)
        val desired = signals.desiredSessionsPerWeek.coerceIn(2, 5).coerceAtMost(available)
        val current = signals.currentPlanSessionsPerWeek.coerceIn(2, 5)
        val elevated = signals.recoveryLoads.filter { it.elevated }

        if (signals.daysObserved < 14 || signals.completedSessions < 4) {
            return TrainingCycleReview(
                status = TrainingCycleReviewStatus.INSUFFICIENT_DATA,
                title = "Ainda coletando histórico",
                summary = "O Constantia precisa de pelo menos 14 dias e 4 treinos concluídos antes de sugerir uma troca estrutural.",
                reasons = listOf(
                    "${signals.daysObserved.coerceAtLeast(0)} dia(s) observados",
                    "${signals.completedSessions} treino(s) concluído(s)"
                ),
                adherencePercent = adherencePercent,
                recommendedSessionsPerWeek = desired,
                rotateExerciseCodes = emptySet(),
                recoveryLoads = signals.recoveryLoads,
                progressingExerciseCount = signals.progressingExerciseCount,
                plateauExerciseCount = signals.plateauExerciseCodes.size,
                canBuildProposal = false
            )
        }

        val reasons = mutableListOf<String>()
        var target = desired
        val frequencyChanged = desired != current
        val durationChanged = signals.planSessionMinutes?.let {
            kotlin.math.abs(it - signals.currentSessionMinutes) >= 15
        } ?: false
        val lowAdherence = (adherence ?: 1.0) < 0.65
        val strongAdherence = (adherence ?: 0.0) >= 0.85
        val hasPlateau = signals.plateauExerciseCodes.isNotEmpty()
        val hasUnavailable = signals.unavailableExerciseCodes.isNotEmpty()
        val recoveryConcern = elevated.isNotEmpty()

        if (frequencyChanged) {
            reasons += "A frequência desejada mudou de $current para $desired sessão(ões) por semana."
        }
        if (durationChanged) {
            reasons += "A janela normal de treino mudou para ${signals.currentSessionMinutes} min."
        }
        if (lowAdherence) {
            reasons += "A aderência recente ficou em ${adherencePercent ?: 0}% dos treinos previstos."
            target = minOf(target, (current - 1).coerceAtLeast(2))
        } else if (strongAdherence) {
            reasons += "A aderência recente está consistente (${adherencePercent ?: 0}%)."
        }
        if (signals.progressingExerciseCount > 0) {
            reasons += "${signals.progressingExerciseCount} exercício(s) mostraram melhora de carga ou repetições no histórico recente."
        }
        if (hasPlateau) {
            reasons += "${signals.plateauExerciseCodes.size} exercício(s) atingiram o critério conservador de possível platô."
        } else if (signals.watchExerciseCodes.isNotEmpty()) {
            reasons += "${signals.watchExerciseCodes.size} exercício(s) estão em observação, ainda sem critério para troca."
        }
        if (hasUnavailable) {
            reasons += "${signals.unavailableExerciseCodes.size} exercício(s) da ficha usam equipamento marcado como indisponível."
        }
        if (recoveryConcern) {
            reasons += "A carga de recuperação recente está acima do padrão anterior em ${elevated.size} grupo(s) muscular(es)."
            if (target > current) target = current
        }

        val rotate = linkedSetOf<String>().apply {
            addAll(signals.plateauExerciseCodes)
            addAll(signals.unavailableExerciseCodes)
        }

        val structuralReason =
            frequencyChanged || durationChanged || lowAdherence || hasPlateau || hasUnavailable

        val status = if (structuralReason) TrainingCycleReviewStatus.REVIEW else TrainingCycleReviewStatus.KEEP
        val canBuild = status == TrainingCycleReviewStatus.REVIEW

        val title = when (status) {
            TrainingCycleReviewStatus.REVIEW -> "Vale revisar a ficha"
            TrainingCycleReviewStatus.KEEP -> "Estrutura atual coerente"
            TrainingCycleReviewStatus.INSUFFICIENT_DATA -> "Ainda coletando histórico"
        }
        val summary = when {
            status == TrainingCycleReviewStatus.KEEP && recoveryConcern ->
                "O desempenho não pede troca estrutural agora, mas a carga de recuperação recente recomenda evitar aumentar volume/frequência neste momento."
            status == TrainingCycleReviewStatus.KEEP ->
                "O histórico ainda favorece manter a estrutura e continuar acumulando dados."
            lowAdherence ->
                "A proposta priorizará uma rotina mais realizável antes de acrescentar volume."
            hasPlateau || hasUnavailable ->
                "A proposta preservará a estrutura útil e trocará somente o que tem motivo objetivo para revisão."
            else ->
                "A proposta será recalculada com sua frequência e disponibilidade atuais."
        }

        return TrainingCycleReview(
            status = status,
            title = title,
            summary = summary,
            reasons = reasons.distinct(),
            adherencePercent = adherencePercent,
            recommendedSessionsPerWeek = target.coerceIn(2, 5).coerceAtMost(available),
            rotateExerciseCodes = rotate,
            recoveryLoads = signals.recoveryLoads,
            progressingExerciseCount = signals.progressingExerciseCount,
            plateauExerciseCount = signals.plateauExerciseCodes.size,
            canBuildProposal = canBuild
        )
    }
}
