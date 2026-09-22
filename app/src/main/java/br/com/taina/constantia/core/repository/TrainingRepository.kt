package br.com.taina.constantia.core.repository

import br.com.taina.constantia.core.database.*
import br.com.taina.constantia.core.model.ProgressionAction
import br.com.taina.constantia.core.model.StagnationAssessment
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
    val completedToday: Boolean
)

class TrainingRepository(
    private val trainingDao: TrainingDao,
    private val profileDao: ProfileDao,
    private val prescriptionEngine: TrainingPrescriptionEngine = TrainingPrescriptionEngine(),
    private val quickWorkoutEngine: QuickWorkoutEngine = QuickWorkoutEngine(),
    private val progressionEngine: TrainingProgressionEngine = TrainingProgressionEngine(),
    private val adaptationEngine: ExerciseAdaptationEngine = ExerciseAdaptationEngine(),
    private val stagnationEngine: StagnationEngine = StagnationEngine(),
    private val scheduleEngine: TrainingScheduleEngine = TrainingScheduleEngine()
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
        ensureCatalog()
        val user = profileDao.getUserProfile() ?: return null
        val training = profileDao.getTrainingProfile() ?: return null
        val restrictions = profileDao.getActiveRestrictions()
        val raw = prescriptionEngine.generate(user, training, restrictions.isNotEmpty())
        val availability = availableEquipmentCodes()
        val exerciseMap = ExerciseCatalog.exercises.associateBy { it.code }
        var replacements = 0
        var omissions = 0
        val adaptedWorkouts = raw.workouts.map { workout ->
            val used = linkedSetOf<String>()
            val adapted = workout.exercises.mapNotNull { spec ->
                val source = exerciseMap[spec.exerciseCode] ?: return@mapNotNull null
                val blockedByRestriction = adaptationEngine.isBlocked(source, restrictions)
                val blockedByEquipment = source.equipmentCode !in availability
                val blocked = blockedByRestriction || blockedByEquipment
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
                        excludedExerciseCodes = used,
                        excludedEquipmentCodes = if (blockedByEquipment) setOf(source.equipmentCode) else emptySet(),
                        limit = 1
                    ).firstOrNull()
                    if (replacement != null) {
                        replacements++
                        used += replacement.code
                        spec.copy(exerciseCode = replacement.code)
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
            if (omissions > 0) append(" $omissions exercício(s) foram omitidos por falta de alternativa segura cadastrada.")
        }

        trainingDao.deactivatePlans()
        val planId = trainingDao.insertPlan(
            TrainingPlanEntity(
                name = raw.name,
                goal = raw.goal,
                rationale = raw.rationale + adjustmentNote
            )
        )
        adaptedWorkouts.forEachIndexed { templateIndex, workout ->
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
        require(setIndex in 1..detail.selectedSets)
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
        profileDao.upsertTrainingProfile(
            profile.copy(
                preferredTrainingDaysCsv = dayValues.sorted().joinToString(","),
                availableDaysPerWeek = dayValues.size.coerceAtLeast(1),
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun getTodayWorkout(date: LocalDate): TodayWorkoutSummary? {
        val plan = trainingDao.getActivePlan() ?: return null
        val profile = profileDao.getTrainingProfile() ?: return null
        val templates = trainingDao.getTemplates(plan.id)
        if (templates.isEmpty()) return null
        val days = scheduleEngine.resolveScheduledDays(templates.size, profile.preferredTrainingDaysCsv)
        val dayIndex = days.indexOf(date.dayOfWeek)
        if (dayIndex < 0) return null
        val template = templates[dayIndex % templates.size]
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val completed = trainingDao.getFinishedSessionsForTemplateBetween(template.id, start, end).isNotEmpty()
        return TodayWorkoutSummary(template, profile.preferredTrainingMinuteOfDay, completed)
    }

    private suspend fun availableEquipmentCodes(): Set<String> {
        val availability = trainingDao.getEquipmentAvailability().associate { it.equipmentCode to it.available }
        return ExerciseCatalog.equipment.filter { availability[it.code] ?: it.commonAtSmartFit }.map { it.code }.toSet()
    }
}
