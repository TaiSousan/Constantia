package br.com.taina.constantia.core.repository

import br.com.taina.constantia.core.database.*
import br.com.taina.constantia.core.model.ProgressionAction
import br.com.taina.constantia.core.model.StagnationAssessment
import br.com.taina.constantia.core.model.StagnationState
import br.com.taina.constantia.core.model.SuggestionStatus
import br.com.taina.constantia.core.model.WorkoutMode
import br.com.taina.constantia.engine.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId


data class WorkoutExerciseDetail(
    val prescription: WorkoutExerciseEntity,
    val exercise: ExerciseEntity,
    val selectedSets: Int = prescription.plannedSets,
    val plannedExercise: ExerciseEntity = exercise,
    val previousSets: List<ExerciseSetEntity> = emptyList(),
    val stagnation: StagnationAssessment? = null
)

data class WorkoutTemplateDetail(
    val template: WorkoutTemplateEntity,
    val exercises: List<WorkoutExerciseDetail>
)

data class MuscleVolumeSummary(
    val muscleCode: String,
    val muscleName: String,
    val effectiveSets: Double
)

data class ActiveWorkout(
    val session: WorkoutSessionEntity,
    val template: WorkoutTemplateEntity,
    val exercises: List<WorkoutExerciseDetail>
)

data class TodayWorkoutSummary(
    val template: WorkoutTemplateEntity,
    val scheduledMinuteOfDay: Int?,
    val completedToday: Boolean,
    val scheduledDate: LocalDate
)

data class CompletedWorkoutSummary(
    val session: WorkoutSessionEntity,
    val template: WorkoutTemplateEntity
)

data class TrainingPlanProposal(
    val review: TrainingCycleReview,
    val spec: TrainingPlanSpec
)

class TrainingRepository(
    private val trainingDao: TrainingDao,
    private val profileDao: ProfileDao,
    private val prescriptionEngine: TrainingPrescriptionEngine = TrainingPrescriptionEngine(),
    private val quickWorkoutEngine: QuickWorkoutEngine = QuickWorkoutEngine(),
    private val progressionEngine: TrainingProgressionEngine = TrainingProgressionEngine(),
    private val adaptationEngine: ExerciseAdaptationEngine = ExerciseAdaptationEngine(),
    private val stagnationEngine: StagnationEngine = StagnationEngine(),
    private val scheduleEngine: TrainingScheduleEngine = TrainingScheduleEngine(),
    private val cycleReviewEngine: TrainingCycleReviewEngine = TrainingCycleReviewEngine()
) {
    val activePlan: Flow<TrainingPlanEntity?> = trainingDao.observeActivePlan()
    val pendingSuggestions: Flow<List<ProgressionSuggestionEntity>> = trainingDao.observePendingSuggestions()
    val exerciseLibrary: Flow<List<ExerciseEntity>> = trainingDao.observeExercises()
    val equipment: Flow<List<EquipmentEntity>> = trainingDao.observeEquipment()
    val equipmentAvailability: Flow<List<EquipmentAvailabilityEntity>> = trainingDao.observeEquipmentAvailability()
    val restrictions: Flow<List<RestrictionEntity>> = profileDao.observeActiveRestrictions()
    val trainingProfile: Flow<TrainingProfileEntity?> = profileDao.observeTrainingProfile()

    suspend fun ensureReady() {
        ensureCatalog()
        if (trainingDao.getActivePlan() == null) generateNewPlan()
    }

    suspend fun ensureCatalog() {
        // RC3 também sincroniza novos itens em instalações já existentes. Inserts de
        // músculos/equipamentos/exercícios usam IGNORE para não substituir pais
        // referenciados pelo histórico; vínculos musculares podem ser atualizados.
        trainingDao.insertMuscles(ExerciseCatalog.muscles)
        trainingDao.insertEquipment(ExerciseCatalog.equipment)
        trainingDao.insertExercises(ExerciseCatalog.exercises)
        trainingDao.insertExerciseMuscles(ExerciseCatalog.exerciseMuscles)

        val existingAvailability = trainingDao.getEquipmentAvailability().map { it.equipmentCode }.toSet()
        ExerciseCatalog.equipment
            .filter { it.code !in existingAvailability }
            .forEach { item ->
                trainingDao.upsertEquipmentAvailability(
                    EquipmentAvailabilityEntity(item.code, item.commonAtSmartFit)
                )
            }
    }

    suspend fun generateNewPlan(): Long? {
        val spec = buildAdaptedPlanSpec() ?: return null
        return persistPlan(spec)
    }

    private suspend fun buildAdaptedPlanSpec(
        trainingOverride: TrainingProfileEntity? = null,
        rotateExerciseCodes: Set<String> = emptySet()
    ): TrainingPlanSpec? {
        ensureCatalog()
        val user = profileDao.getUserProfile() ?: return null
        val training = trainingOverride ?: profileDao.getTrainingProfile() ?: return null
        val restrictions = profileDao.getActiveRestrictions()
        val raw = prescriptionEngine.generate(user, training, restrictions.isNotEmpty())
        val availability = availableEquipmentCodes()
        val exerciseMap = ExerciseCatalog.exercises.associateBy { it.code }
        var replacements = 0
        var rotations = 0
        var omissions = 0

        val adaptedWorkouts = raw.workouts.map { workout ->
            val used = linkedSetOf<String>()
            val adapted = workout.exercises.mapNotNull { spec ->
                val source = exerciseMap[spec.exerciseCode] ?: return@mapNotNull null
                val blockedByRestriction = adaptationEngine.isBlocked(source, restrictions)
                val blockedByEquipment = source.equipmentCode !in availability
                val blockedForReview = source.code in rotateExerciseCodes
                val blocked = blockedByRestriction || blockedByEquipment || blockedForReview

                if (!blocked) {
                    used += source.code
                    spec
                } else {
                    val replacement = adaptationEngine.findSubstitutes(
                        source = source,
                        exercises = ExerciseCatalog.exercises,
                        muscleLinks = ExerciseCatalog.exerciseMuscles,
                        availableEquipmentCodes = availability,
                        restrictions = restrictions,
                        excludedExerciseCodes = used + rotateExerciseCodes,
                        excludedEquipmentCodes = if (blockedByEquipment) setOf(source.equipmentCode) else emptySet(),
                        limit = 1
                    ).firstOrNull()

                    if (replacement != null) {
                        if (blockedForReview && !blockedByRestriction && !blockedByEquipment) rotations++ else replacements++
                        used += replacement.code
                        spec.copy(exerciseCode = replacement.code)
                    } else if (blockedForReview && !blockedByRestriction && !blockedByEquipment) {
                        used += source.code
                        spec
                    } else {
                        omissions++
                        null
                    }
                }
            }
            workout.copy(exercises = adapted)
        }

        val adjustmentNote = buildString {
            if (replacements > 0) append(" $replacements exercício(s) foram substituídos por restrição ou equipamento indisponível.")
            if (rotations > 0) append(" $rotations exercício(s) foram rotacionados após revisão do histórico.")
            if (omissions > 0) append(" $omissions exercício(s) foram omitidos por falta de alternativa segura cadastrada.")
        }

        return raw.copy(
            rationale = raw.rationale + adjustmentNote,
            workouts = adaptedWorkouts
        )
    }

    private suspend fun persistPlan(spec: TrainingPlanSpec): Long {
        trainingDao.deactivatePlans()
        val planId = trainingDao.insertPlan(
            TrainingPlanEntity(
                name = spec.name,
                goal = spec.goal,
                rationale = spec.rationale
            )
        )
        spec.workouts.forEachIndexed { templateIndex, workout ->
            val templateId = trainingDao.insertTemplate(
                WorkoutTemplateEntity(
                    planId = planId,
                    name = workout.name,
                    orderIndex = templateIndex,
                    estimatedMinutes = workout.estimatedMinutes
                )
            )
            trainingDao.insertWorkoutExercises(
                workout.exercises.mapIndexed { exerciseIndex, ex ->
                    WorkoutExerciseEntity(
                        templateId = templateId,
                        exerciseCode = ex.exerciseCode,
                        orderIndex = exerciseIndex,
                        plannedSets = ex.sets,
                        repMin = ex.repMin,
                        repMax = ex.repMax,
                        targetRirMin = ex.rirMin,
                        targetRirMax = ex.rirMax,
                        restSeconds = ex.restSeconds,
                        priorityScore = ex.priorityScore
                    )
                }
            )
        }
        return planId
    }

    suspend fun loadPlan(planId: Long): List<WorkoutTemplateDetail> {
        return trainingDao.getTemplates(planId).map { template ->
            val prescriptions = trainingDao.getWorkoutExercises(template.id)
            val exerciseMap = trainingDao.getExercises(prescriptions.map { it.exerciseCode }).associateBy { it.code }
            WorkoutTemplateDetail(
                template = template,
                exercises = prescriptions.mapNotNull { p ->
                    val exercise = exerciseMap[p.exerciseCode] ?: return@mapNotNull null
                    val recentSlot = trainingDao.getRecentSetsForWorkoutExercise(p.id, 40)
                    val recentExercise = trainingDao.getRecentSets(p.exerciseCode, 40)
                    val previous = recentExercise.groupBy { it.sessionId }.values.firstOrNull().orEmpty().sortedBy { it.setIndex }
                    WorkoutExerciseDetail(
                        prescription = p,
                        exercise = exercise,
                        plannedExercise = exercise,
                        previousSets = previous,
                        stagnation = stagnationEngine.evaluate(recentSlot, p.exerciseCode)
                    )
                }
            )
        }
    }

    suspend fun calculateWeeklyVolume(templates: List<WorkoutTemplateDetail>): List<MuscleVolumeSummary> {
        val all = templates.flatMap { it.exercises }
        if (all.isEmpty()) return emptyList()
        val codes = all.map { it.plannedExercise.code }.distinct()
        val contributions = trainingDao.getExerciseMuscles(codes).groupBy { it.exerciseCode }
        val totals = linkedMapOf<String, Double>()
        all.forEach { detail ->
            contributions[detail.plannedExercise.code].orEmpty().forEach { link ->
                totals[link.muscleCode] = (totals[link.muscleCode] ?: 0.0) + detail.prescription.plannedSets * link.contribution
            }
        }
        val names = trainingDao.getMuscles().associate { it.code to it.name }
        return totals.map { (code, sets) -> MuscleVolumeSummary(code, names[code] ?: code, sets) }
            .sortedByDescending { it.effectiveSets }
    }

    suspend fun startWorkout(template: WorkoutTemplateEntity, mode: WorkoutMode, quickMinutes: Int? = null): ActiveWorkout {
        val prescriptions = trainingDao.getWorkoutExercises(template.id)
        val exercises = trainingDao.getExercises(prescriptions.map { it.exerciseCode })
        val exerciseMap = exercises.associateBy { it.code }
        val selection = if (mode == WorkoutMode.QUICK) {
            quickWorkoutEngine.build(prescriptions, exerciseMap, quickMinutes ?: 25)
                .associate { it.workoutExerciseId to it.selectedSets }
        } else prescriptions.associate { it.id to it.plannedSets }
        val sessionId = trainingDao.insertSession(
            WorkoutSessionEntity(templateId = template.id, mode = mode.name, availableMinutes = quickMinutes)
        )
        val session = trainingDao.getSession(sessionId) ?: error("Sessão recém-criada não encontrada")
        val details = prescriptions.mapNotNull { p ->
            val selectedSets = selection[p.id] ?: return@mapNotNull null
            val exercise = exerciseMap[p.exerciseCode] ?: return@mapNotNull null
            val recentSlot = trainingDao.getRecentSetsForWorkoutExercise(p.id, 40)
            val recentExercise = trainingDao.getRecentSets(p.exerciseCode, 40)
            WorkoutExerciseDetail(
                prescription = p,
                exercise = exercise,
                selectedSets = selectedSets,
                plannedExercise = exercise,
                previousSets = recentExercise.groupBy { it.sessionId }.values.firstOrNull().orEmpty().sortedBy { it.setIndex },
                stagnation = stagnationEngine.evaluate(recentSlot, p.exerciseCode)
            )
        }
        return ActiveWorkout(session, template, details)
    }

    fun observeSessionSets(sessionId: Long): Flow<List<ExerciseSetEntity>> = trainingDao.observeSetsForSession(sessionId)

    suspend fun findSubstitutes(active: ActiveWorkout, workoutExerciseId: Long): List<ExerciseEntity> {
        val detail = active.exercises.first { it.prescription.id == workoutExerciseId }
        val restrictions = profileDao.getActiveRestrictions()
        val available = availableEquipmentCodes()
        val used = active.exercises.filter { it.prescription.id != workoutExerciseId }.map { it.exercise.code }.toSet()
        return adaptationEngine.findSubstitutes(
            source = detail.exercise,
            exercises = ExerciseCatalog.exercises,
            muscleLinks = ExerciseCatalog.exerciseMuscles,
            availableEquipmentCodes = available,
            restrictions = restrictions,
            excludedExerciseCodes = used,
            excludedEquipmentCodes = setOf(detail.exercise.equipmentCode),
            limit = 8
        )
    }

    suspend fun substituteExercise(active: ActiveWorkout, workoutExerciseId: Long, replacementCode: String): ActiveWorkout {
        val replacement = trainingDao.getExercise(replacementCode) ?: error("Exercício substituto não encontrado")
        val index = active.exercises.indexOfFirst { it.prescription.id == workoutExerciseId }
        require(index >= 0)
        val current = active.exercises[index]
        trainingDao.insertSubstitution(
            WorkoutSubstitutionEntity(
                sessionId = active.session.id,
                workoutExerciseId = workoutExerciseId,
                plannedExerciseCode = current.plannedExercise.code,
                executedExerciseCode = replacement.code,
                reason = "EQUIPMENT_BUSY"
            )
        )
        val updated = active.exercises.toMutableList()
        updated[index] = current.copy(exercise = replacement, previousSets = emptyList(), stagnation = null)
        return active.copy(exercises = updated)
    }

    suspend fun saveSet(
        active: ActiveWorkout,
        workoutExerciseId: Long,
        setIndex: Int,
        loadKg: Double,
        reps: Int,
        rir: Int
    ) {
        val detail = active.exercises.first { it.prescription.id == workoutExerciseId }
        // A prescrição continua em detail.selectedSets; índices acima dela são
        // séries extras voluntárias da sessão e não alteram a ficha automaticamente.
        require(setIndex in 1..12)
        require(loadKg >= 0.0)
        require(reps in 1..100)
        require(rir in 0..10)
        trainingDao.insertSet(
            ExerciseSetEntity(
                sessionId = active.session.id,
                workoutExerciseId = workoutExerciseId,
                exerciseCode = detail.exercise.code,
                setIndex = setIndex,
                loadKg = loadKg,
                reps = reps,
                rir = rir
            )
        )
    }

    suspend fun finishWorkout(active: ActiveWorkout) {
        trainingDao.finishSession(active.session.id, System.currentTimeMillis())
        active.exercises.forEach { detail ->
            if (detail.exercise.code != detail.plannedExercise.code) return@forEach
            val sets = trainingDao.getSetsForWorkoutExercise(active.session.id, detail.prescription.id)
            if (sets.isEmpty()) return@forEach
            val equipment = trainingDao.getEquipment(detail.exercise.equipmentCode)
            val adjustedPrescription = detail.prescription.copy(plannedSets = detail.selectedSets)
            val decision = progressionEngine.evaluate(adjustedPrescription, sets, equipment) ?: return@forEach
            if (decision.action == ProgressionAction.MAINTAIN && detail.prescription.targetLoadKg == null) {
                trainingDao.updateTargetLoad(detail.prescription.id, decision.currentLoadKg)
            }
            trainingDao.insertSuggestion(
                ProgressionSuggestionEntity(
                    workoutExerciseId = detail.prescription.id,
                    exerciseCode = detail.exercise.code,
                    action = decision.action.name,
                    currentLoadKg = decision.currentLoadKg,
                    suggestedLoadKg = decision.suggestedLoadKg,
                    rationale = decision.rationale
                )
            )
        }
    }

    suspend fun acceptSuggestion(suggestion: ProgressionSuggestionEntity) {
        trainingDao.updateTargetLoad(suggestion.workoutExerciseId, suggestion.suggestedLoadKg)
        trainingDao.setSuggestionStatus(suggestion.id, SuggestionStatus.ACCEPTED.name)
    }

    suspend fun rejectSuggestion(suggestion: ProgressionSuggestionEntity) {
        trainingDao.setSuggestionStatus(suggestion.id, SuggestionStatus.REJECTED.name)
    }

    suspend fun setEquipmentAvailable(code: String, available: Boolean) {
        trainingDao.upsertEquipmentAvailability(EquipmentAvailabilityEntity(code, available))
    }

    suspend fun addExerciseRestriction(exerciseCode: String, bodyRegion: String, description: String, professional: Boolean) {
        val exercise = trainingDao.getExercise(exerciseCode) ?: error("Exercício não encontrado")
        profileDao.insertRestriction(
            RestrictionEntity(
                restrictionType = "EXERCISE_AVOID",
                bodyRegion = bodyRegion.trim(),
                description = description.trim().ifBlank { "Evitar ${exercise.name}" },
                professionalGuidance = if (professional) "Orientação profissional para evitar este exercício." else "",
                exerciseCode = exercise.code
            )
        )
    }

    suspend fun removeRestriction(id: Long) = profileDao.deactivateRestriction(id)

    suspend fun setPreferredTrainingDays(dayValues: Set<Int>) {
        val profile = profileDao.getTrainingProfile() ?: return
        setTrainingAvailability(
            dayValues = dayValues,
            sessionsPerWeek = profile.currentTrainingDaysPerWeek.coerceIn(2, 5),
            normalSessionMinutes = profile.normalSessionMinutes,
            minimumSessionMinutes = profile.minimumSessionMinutes
        )
    }

    suspend fun setTrainingAvailability(
        dayValues: Set<Int>,
        sessionsPerWeek: Int,
        normalSessionMinutes: Int,
        minimumSessionMinutes: Int
    ) {
        val normalizedDays = dayValues.filter { it in 1..7 }.toSortedSet()
        require(normalizedDays.size >= 2) { "Selecione pelo menos dois dias disponíveis." }
        val profile = profileDao.getTrainingProfile() ?: return
        val targetSessions = sessionsPerWeek.coerceIn(2, minOf(5, normalizedDays.size))
        val normalMinutes = normalSessionMinutes.coerceIn(30, 180)
        val quickMinutes = minimumSessionMinutes.coerceIn(10, minOf(120, normalMinutes))
        profileDao.upsertTrainingProfile(
            profile.copy(
                currentTrainingDaysPerWeek = targetSessions,
                availableDaysPerWeek = normalizedDays.size,
                normalSessionMinutes = normalMinutes,
                minimumSessionMinutes = quickMinutes,
                preferredTrainingDaysCsv = normalizedDays.joinToString(","),
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun buildCycleReview(anchor: LocalDate = LocalDate.now()): TrainingCycleReview {
        val plan = trainingDao.getActivePlan()
        val profile = profileDao.getTrainingProfile()
        if (plan == null || profile == null) {
            return emptyCycleReview()
        }

        val templates = trainingDao.getTemplates(plan.id)
        if (templates.isEmpty()) return emptyCycleReview(profile)

        val zone = ZoneId.systemDefault()
        val planStart = java.time.Instant.ofEpochMilli(plan.createdAtMillis).atZone(zone).toLocalDate()
        val startDate = maxOf(planStart, anchor.minusDays(27))
        val daysObserved = (
            java.time.temporal.ChronoUnit.DAYS.between(startDate, anchor) + 1
        ).toInt().coerceAtLeast(0)
        val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = anchor.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val templateIds = templates.map { it.id }.toSet()
        val sessions = trainingDao.getFinishedSessionsBetween(startMillis, endMillis)
            .filter { it.templateId in templateIds }
        val sessionIds = sessions.map { it.id }.toSet()

        val scheduledDays = scheduleEngine.resolveScheduledDays(
            templates.size,
            profile.preferredTrainingDaysCsv
        )
        val plannedSessions = generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(anchor) }
            .count { it.dayOfWeek in scheduledDays }

        val details = loadPlan(plan.id)
        val plateauCodes = details.flatMap { it.exercises }
            .filter { it.stagnation?.state == StagnationState.PLATEAU }
            .map { it.exercise.code }
            .toSet()
        val watchCodes = details.flatMap { it.exercises }
            .filter { it.stagnation?.state == StagnationState.WATCH }
            .map { it.exercise.code }
            .toSet()

        val availability = trainingDao.getEquipmentAvailability().associate { it.equipmentCode to it.available }
        val unavailableCodes = details.flatMap { it.exercises }
            .map { it.exercise }
            .filter { exercise -> availability[exercise.equipmentCode] == false }
            .map { it.code }
            .toSet()

        val allPeriodSets = trainingDao.getExerciseSetsBetween(startMillis, endMillis)
        val periodSets = allPeriodSets.filter { it.sessionId in sessionIds }
        val progressingCount = progressingExerciseCount(periodSets)

        val links = trainingDao.getExerciseMuscles(periodSets.map { it.exerciseCode }.distinct())
            .groupBy { it.exerciseCode }
        val muscleNames = trainingDao.getMuscles().associate { it.code to it.name }
        val recentStart = anchor.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        val previousStart = anchor.minusDays(13).atStartOfDay(zone).toInstant().toEpochMilli()
        val previousEnd = anchor.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val hardStart = endMillis - 72L * 60L * 60L * 1000L

        fun effortWeight(rir: Int): Double = when {
            rir <= 0 -> 1.20
            rir == 1 -> 1.10
            rir == 2 -> 1.00
            rir == 3 -> 0.90
            else -> 0.80
        }

        val muscleCodes = links.values.flatten().map { it.muscleCode }.distinct()
        val recoveryLoads = muscleCodes.map { muscleCode ->
            fun effective(from: Long, to: Long): Double =
                periodSets.asSequence()
                    .filter { it.completedAtMillis in from..to }
                    .sumOf { set ->
                        links[set.exerciseCode].orEmpty()
                            .filter { it.muscleCode == muscleCode }
                            .sumOf { link -> link.contribution * effortWeight(set.rir) }
                    }

            val recent = effective(recentStart, endMillis)
            val previous = effective(previousStart, previousEnd)
            val hard72 = periodSets.asSequence()
                .filter { it.completedAtMillis in hardStart..endMillis && it.rir <= 1 }
                .sumOf { set ->
                    links[set.exerciseCode].orEmpty()
                        .filter { it.muscleCode == muscleCode }
                        .sumOf { it.contribution }
                }
            MuscleRecoveryLoad(
                muscleCode = muscleCode,
                muscleName = muscleNames[muscleCode] ?: muscleCode,
                recentEffectiveSets = recent,
                previousEffectiveSets = previous,
                hardEffectiveSetsLast72h = hard72,
                elevated = previous >= 4.0 &&
                    recent >= maxOf(8.0, previous * 1.35) &&
                    hard72 >= 3.0
            )
        }.sortedWith(
            compareByDescending<MuscleRecoveryLoad> { it.elevated }
                .thenByDescending { it.recentEffectiveSets }
        )

        val planMinutes = Regex("janela de até (\\d+) min")
            .find(plan.rationale)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        return cycleReviewEngine.evaluate(
            TrainingCycleSignals(
                daysObserved = daysObserved,
                completedSessions = sessions.size,
                plannedSessions = plannedSessions,
                currentPlanSessionsPerWeek = templates.size.coerceIn(2, 5),
                desiredSessionsPerWeek = profile.currentTrainingDaysPerWeek
                    .takeIf { it in 2..5 } ?: templates.size.coerceIn(2, 5),
                availableDaysPerWeek = scheduleEngine.resolveDays(
                    profile.availableDaysPerWeek,
                    profile.preferredTrainingDaysCsv
                ).size.coerceAtLeast(2),
                currentSessionMinutes = profile.normalSessionMinutes,
                planSessionMinutes = planMinutes,
                progressingExerciseCount = progressingCount,
                plateauExerciseCodes = plateauCodes,
                watchExerciseCodes = watchCodes,
                unavailableExerciseCodes = unavailableCodes,
                recoveryLoads = recoveryLoads
            )
        )
    }

    private fun emptyCycleReview(profile: TrainingProfileEntity? = null): TrainingCycleReview =
        cycleReviewEngine.evaluate(
            TrainingCycleSignals(
                daysObserved = 0,
                completedSessions = 0,
                plannedSessions = 0,
                currentPlanSessionsPerWeek = 2,
                desiredSessionsPerWeek = profile?.currentTrainingDaysPerWeek?.coerceAtLeast(2) ?: 2,
                availableDaysPerWeek = profile?.availableDaysPerWeek?.coerceAtLeast(2) ?: 2,
                currentSessionMinutes = profile?.normalSessionMinutes ?: 60,
                planSessionMinutes = null,
                progressingExerciseCount = 0,
                plateauExerciseCodes = emptySet(),
                watchExerciseCodes = emptySet(),
                unavailableExerciseCodes = emptySet(),
                recoveryLoads = emptyList()
            )
        )

    suspend fun buildCyclePlanProposal(review: TrainingCycleReview): TrainingPlanProposal? {
        if (!review.canBuildProposal) return null
        val profile = profileDao.getTrainingProfile() ?: return null
        val adjustedProfile = profile.copy(
            currentTrainingDaysPerWeek = review.recommendedSessionsPerWeek
        )
        val spec = buildAdaptedPlanSpec(
            trainingOverride = adjustedProfile,
            rotateExerciseCodes = review.rotateExerciseCodes
        ) ?: return null
        return TrainingPlanProposal(review = review, spec = spec)
    }

    suspend fun applyCyclePlanProposal(proposal: TrainingPlanProposal): Long? {
        val profile = profileDao.getTrainingProfile() ?: return null
        profileDao.upsertTrainingProfile(
            profile.copy(
                currentTrainingDaysPerWeek = proposal.review.recommendedSessionsPerWeek,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        return persistPlan(proposal.spec)
    }

    private fun progressingExerciseCount(sets: List<ExerciseSetEntity>): Int =
        sets.groupBy { it.exerciseCode }.count { (_, exerciseSets) ->
            val sessions = exerciseSets.groupBy { it.sessionId }.values
                .sortedByDescending { group -> group.maxOf { it.completedAtMillis } }
            val latest = sessions.getOrNull(0) ?: return@count false
            val previous = sessions.getOrNull(1) ?: return@count false
            val latestLoad = latest.maxOf { it.loadKg }
            val previousLoad = previous.maxOf { it.loadKg }
            val meaningfulLoadGain = latestLoad > previousLoad + maxOf(0.5, previousLoad * 0.01)
            val comparableLoad = kotlin.math.abs(latestLoad - previousLoad) <= 0.25
            val repGain = comparableLoad && latest.sumOf { it.reps } > previous.sumOf { it.reps }
            meaningfulLoadGain || repGain
        }

    suspend fun getTodayWorkout(date: LocalDate): TodayWorkoutSummary? {
        val plan = trainingDao.getActivePlan() ?: return null
        val profile = profileDao.getTrainingProfile() ?: return null
        val templates = trainingDao.getTemplates(plan.id)
        if (templates.isEmpty()) return null
        val templateIndex = scheduleEngine.templateIndexFor(
            date,
            templates.size,
            profile.preferredTrainingDaysCsv
        ) ?: return null
        val template = templates[templateIndex]
        val (start, end) = dayBounds(date)
        val completed = trainingDao.getFinishedSessionsForTemplateBetween(template.id, start, end).isNotEmpty()
        return TodayWorkoutSummary(
            template = template,
            scheduledMinuteOfDay = profile.preferredTrainingMinuteOfDay,
            completedToday = completed,
            scheduledDate = date
        )
    }

    /**
     * Última sessão de treino realmente concluída nesta data.
     */
    suspend fun getCompletedWorkout(date: LocalDate): CompletedWorkoutSummary? {
        val plan = trainingDao.getActivePlan() ?: return null
        val templatesById = trainingDao.getTemplates(plan.id).associateBy { it.id }
        if (templatesById.isEmpty()) return null
        val (start, end) = dayBounds(date)
        val session = trainingDao.getFinishedSessionsBetween(start, end)
            .asReversed()
            .firstOrNull { it.templateId in templatesById } ?: return null
        return CompletedWorkoutSummary(session, templatesById.getValue(session.templateId))
    }

    /**
     * Procura o treino programado mais recente dos últimos seis dias que ainda
     * não foi recuperado. A conclusão posterior do MESMO template quita o atraso,
     * sem marcar automaticamente outro treino programado para hoje.
     */
    suspend fun getMostRecentOverdueWorkout(date: LocalDate): TodayWorkoutSummary? {
        val plan = trainingDao.getActivePlan() ?: return null
        val profile = profileDao.getTrainingProfile() ?: return null
        val templates = trainingDao.getTemplates(plan.id)
        if (templates.isEmpty()) return null
        val referenceEnd = dayBounds(date).second

        for (offset in 1L..6L) {
            val scheduledDate = date.minusDays(offset)
            val templateIndex = scheduleEngine.templateIndexFor(
                scheduledDate,
                templates.size,
                profile.preferredTrainingDaysCsv
            ) ?: continue
            val template = templates[templateIndex]
            val scheduledStart = dayBounds(scheduledDate).first
            val recovered = trainingDao.getFinishedSessionsForTemplateBetween(
                template.id,
                scheduledStart,
                referenceEnd
            ).isNotEmpty()
            if (!recovered) {
                return TodayWorkoutSummary(
                    template = template,
                    scheduledMinuteOfDay = profile.preferredTrainingMinuteOfDay,
                    completedToday = false,
                    scheduledDate = scheduledDate
                )
            }
        }
        return null
    }

    private fun dayBounds(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    private suspend fun availableEquipmentCodes(): Set<String> {
        val availability = trainingDao.getEquipmentAvailability().associate { it.equipmentCode to it.available }
        return ExerciseCatalog.equipment.filter { availability[it.code] ?: it.commonAtSmartFit }.map { it.code }.toSet()
    }
}
