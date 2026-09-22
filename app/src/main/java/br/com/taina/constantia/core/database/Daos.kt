package br.com.taina.constantia.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Query("SELECT * FROM training_profile WHERE id = 1")
    suspend fun getTrainingProfile(): TrainingProfileEntity?

    @Query("SELECT * FROM training_profile WHERE id = 1")
    fun observeTrainingProfile(): Flow<TrainingProfileEntity?>

    @Query("SELECT * FROM routine_profile WHERE id = 1")
    suspend fun getRoutineProfile(): RoutineProfileEntity?

    @Query("SELECT * FROM restrictions WHERE active = 1")
    suspend fun getActiveRestrictions(): List<RestrictionEntity>

    @Query("SELECT * FROM restrictions WHERE active = 1 ORDER BY createdAtMillis DESC")
    fun observeActiveRestrictions(): Flow<List<RestrictionEntity>>

    @Upsert suspend fun upsertUserProfile(profile: UserProfileEntity)
    @Upsert suspend fun upsertTrainingProfile(profile: TrainingProfileEntity)
    @Upsert suspend fun upsertRoutineProfile(profile: RoutineProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestriction(restriction: RestrictionEntity): Long

    @Query("UPDATE restrictions SET active = 0 WHERE id = :id")
    suspend fun deactivateRestriction(id: Long)

    @Insert
    suspend fun insertBodyMetric(metric: BodyMetricEntity)

    @Query("SELECT * FROM body_metrics ORDER BY recordedAtMillis DESC")
    fun observeBodyMetrics(): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM body_metrics ORDER BY recordedAtMillis DESC LIMIT :limit")
    suspend fun getRecentBodyMetrics(limit: Int): List<BodyMetricEntity>

    @Query("SELECT * FROM body_metrics WHERE recordedAtMillis BETWEEN :startMillis AND :endMillis ORDER BY recordedAtMillis")
    suspend fun getBodyMetricsBetween(startMillis: Long, endMillis: Long): List<BodyMetricEntity>
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_definitions WHERE active = 1 ORDER BY priority, name")
    fun observeActiveDefinitions(): Flow<List<ActivityDefinitionEntity>>

    @Query("SELECT * FROM activity_definitions WHERE active = 1 ORDER BY priority, name")
    suspend fun getActiveDefinitions(): List<ActivityDefinitionEntity>

    @Query("SELECT * FROM activity_occurrences WHERE scheduledEpochDay = :epochDay")
    fun observeOccurrencesForDay(epochDay: Long): Flow<List<ActivityOccurrenceEntity>>

    @Query("SELECT * FROM activity_occurrences WHERE scheduledEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    fun observeOccurrencesInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<ActivityOccurrenceEntity>>

    @Query("SELECT * FROM activity_occurrences WHERE scheduledEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun getAllOccurrencesInRange(startEpochDay: Long, endEpochDay: Long): List<ActivityOccurrenceEntity>

    @Query("SELECT * FROM activity_occurrences WHERE activityId = :activityId AND scheduledEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun getOccurrencesInRange(activityId: Long, startEpochDay: Long, endEpochDay: Long): List<ActivityOccurrenceEntity>

    @Insert
    suspend fun insertDefinition(definition: ActivityDefinitionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrence(occurrence: ActivityOccurrenceEntity): Long

    @Query("UPDATE activity_occurrences SET status = :status, completedAtMillis = :completedAtMillis WHERE activityId = :activityId AND scheduledEpochDay = :epochDay")
    suspend fun updateStatus(activityId: Long, epochDay: Long, status: String, completedAtMillis: Long?)

    @Query("UPDATE activity_occurrences SET status = :status, completedAtMillis = NULL, failureReason = :failureReason WHERE activityId = :activityId AND scheduledEpochDay = :epochDay")
    suspend fun updateMissed(activityId: Long, epochDay: Long, status: String, failureReason: String)

    @Query("SELECT * FROM activity_occurrences WHERE activityId = :activityId AND scheduledEpochDay = :epochDay LIMIT 1")
    suspend fun findOccurrence(activityId: Long, epochDay: Long): ActivityOccurrenceEntity?
}

@Dao
interface TrainingDao {
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMuscles(items: List<MuscleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(items: List<EquipmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseMuscles(items: List<ExerciseMuscleEntity>)

    @Query("SELECT * FROM exercises WHERE active = 1 ORDER BY name")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE code = :code LIMIT 1")
    suspend fun getExercise(code: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE code IN (:codes)")
    suspend fun getExercises(codes: List<String>): List<ExerciseEntity>

    @Query("SELECT * FROM equipment ORDER BY name")
    fun observeEquipment(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment WHERE code = :code LIMIT 1")
    suspend fun getEquipment(code: String): EquipmentEntity?

    @Query("SELECT * FROM exercise_muscles WHERE exerciseCode IN (:codes)")
    suspend fun getExerciseMuscles(codes: List<String>): List<ExerciseMuscleEntity>

    @Query("SELECT * FROM muscles ORDER BY name")
    suspend fun getMuscles(): List<MuscleEntity>

    @Query("SELECT * FROM training_plans WHERE active = 1 ORDER BY createdAtMillis DESC LIMIT 1")
    fun observeActivePlan(): Flow<TrainingPlanEntity?>

    @Query("SELECT * FROM training_plans WHERE active = 1 ORDER BY createdAtMillis DESC LIMIT 1")
    suspend fun getActivePlan(): TrainingPlanEntity?

    @Insert
    suspend fun insertPlan(plan: TrainingPlanEntity): Long

    @Query("UPDATE training_plans SET active = 0 WHERE active = 1")
    suspend fun deactivatePlans()

    @Insert
    suspend fun insertTemplate(template: WorkoutTemplateEntity): Long

    @Insert
    suspend fun insertWorkoutExercises(items: List<WorkoutExerciseEntity>)

    @Query("SELECT * FROM workout_templates WHERE planId = :planId ORDER BY orderIndex")
    fun observeTemplates(planId: Long): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE planId = :planId ORDER BY orderIndex")
    suspend fun getTemplates(planId: Long): List<WorkoutTemplateEntity>

    @Query("SELECT * FROM workout_exercises WHERE templateId = :templateId ORDER BY orderIndex")
    suspend fun getWorkoutExercises(templateId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE templateId = :templateId ORDER BY orderIndex")
    fun observeWorkoutExercises(templateId: Long): Flow<List<WorkoutExerciseEntity>>

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Query("UPDATE workout_sessions SET finishedAtMillis = :finishedAtMillis WHERE id = :sessionId")
    suspend fun finishSession(sessionId: Long, finishedAtMillis: Long)

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: Long): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: ExerciseSetEntity): Long

    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId ORDER BY workoutExerciseId, setIndex")
    fun observeSetsForSession(sessionId: Long): Flow<List<ExerciseSetEntity>>

    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId AND workoutExerciseId = :workoutExerciseId ORDER BY setIndex")
    suspend fun getSetsForWorkoutExercise(sessionId: Long, workoutExerciseId: Long): List<ExerciseSetEntity>

    @Query("SELECT * FROM exercise_sets WHERE exerciseCode = :exerciseCode ORDER BY completedAtMillis DESC LIMIT :limit")
    suspend fun getRecentSets(exerciseCode: String, limit: Int): List<ExerciseSetEntity>

    @Insert
    suspend fun insertSuggestion(suggestion: ProgressionSuggestionEntity): Long

    @Query("SELECT * FROM progression_suggestions WHERE status = 'PENDING' ORDER BY createdAtMillis DESC")
    fun observePendingSuggestions(): Flow<List<ProgressionSuggestionEntity>>

    @Query("UPDATE progression_suggestions SET status = :status WHERE id = :id")
    suspend fun setSuggestionStatus(id: Long, status: String)

    @Query("UPDATE workout_exercises SET targetLoadKg = :loadKg WHERE id = :workoutExerciseId")
    suspend fun updateTargetLoad(workoutExerciseId: Long, loadKg: Double)

    @Query("SELECT * FROM equipment_availability ORDER BY equipmentCode")
    fun observeEquipmentAvailability(): Flow<List<EquipmentAvailabilityEntity>>

    @Query("SELECT * FROM equipment_availability")
    suspend fun getEquipmentAvailability(): List<EquipmentAvailabilityEntity>

    @Upsert
    suspend fun upsertEquipmentAvailability(item: EquipmentAvailabilityEntity)

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE finishedAtMillis BETWEEN :startMillis AND :endMillis")
    suspend fun countFinishedSessionsBetween(startMillis: Long, endMillis: Long): Int

    @Query("SELECT * FROM workout_sessions WHERE finishedAtMillis BETWEEN :startMillis AND :endMillis ORDER BY finishedAtMillis")
    suspend fun getFinishedSessionsBetween(startMillis: Long, endMillis: Long): List<WorkoutSessionEntity>

    @Query("SELECT es.* FROM exercise_sets es JOIN workout_sessions ws ON ws.id = es.sessionId WHERE ws.finishedAtMillis BETWEEN :startMillis AND :endMillis ORDER BY ws.finishedAtMillis, es.exerciseCode, es.setIndex")
    suspend fun getExerciseSetsBetween(startMillis: Long, endMillis: Long): List<ExerciseSetEntity>

    @Query("SELECT * FROM workout_sessions WHERE templateId = :templateId AND finishedAtMillis BETWEEN :startMillis AND :endMillis ORDER BY finishedAtMillis DESC")
    suspend fun getFinishedSessionsForTemplateBetween(templateId: Long, startMillis: Long, endMillis: Long): List<WorkoutSessionEntity>

    @Query("SELECT es.* FROM exercise_sets es JOIN workout_sessions ws ON ws.id = es.sessionId WHERE es.workoutExerciseId = :workoutExerciseId AND ws.finishedAtMillis IS NOT NULL ORDER BY ws.finishedAtMillis DESC, es.setIndex ASC LIMIT :limit")
    suspend fun getRecentSetsForWorkoutExercise(workoutExerciseId: Long, limit: Int): List<ExerciseSetEntity>

    @Insert
    suspend fun insertSubstitution(item: WorkoutSubstitutionEntity): Long

    @Query("SELECT * FROM workout_substitutions WHERE sessionId = :sessionId ORDER BY createdAtMillis")
    suspend fun getSubstitutionsForSession(sessionId: Long): List<WorkoutSubstitutionEntity>
}

@Dao
interface StudyDao {
    @Query("SELECT * FROM subjects WHERE active = 1 ORDER BY name")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Insert
    suspend fun insertSubject(item: SubjectEntity): Long

    @Query("SELECT * FROM study_topics WHERE active = 1 ORDER BY name")
    fun observeTopics(): Flow<List<StudyTopicEntity>>

    @Query("SELECT * FROM study_topics WHERE subjectId = :subjectId AND active = 1 ORDER BY name")
    fun observeTopicsForSubject(subjectId: Long): Flow<List<StudyTopicEntity>>

    @Insert
    suspend fun insertTopic(item: StudyTopicEntity): Long

    @Query("SELECT * FROM study_goals WHERE active = 1 ORDER BY createdAtMillis")
    fun observeStudyGoals(): Flow<List<StudyGoalEntity>>

    @Query("SELECT * FROM study_goals WHERE active = 1 ORDER BY createdAtMillis")
    suspend fun getStudyGoals(): List<StudyGoalEntity>

    @Insert
    suspend fun insertStudyGoal(item: StudyGoalEntity): Long

    @Query("SELECT * FROM study_sessions WHERE startedAtMillis BETWEEN :startMillis AND :endMillis ORDER BY startedAtMillis DESC")
    suspend fun getStudySessionsBetween(startMillis: Long, endMillis: Long): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE subjectId = :subjectId AND startedAtMillis BETWEEN :startMillis AND :endMillis ORDER BY startedAtMillis DESC")
    suspend fun getStudySessionsForSubjectBetween(subjectId: Long, startMillis: Long, endMillis: Long): List<StudySessionEntity>

    @Insert
    suspend fun insertStudySession(item: StudySessionEntity): Long

    @Query("SELECT * FROM review_questions WHERE active = 1 AND nextReviewEpochDay <= :epochDay ORDER BY nextReviewEpochDay, reviewCount LIMIT :limit")
    suspend fun getDueQuestions(epochDay: Long, limit: Int): List<ReviewQuestionEntity>

    @Query("SELECT * FROM review_questions WHERE active = 1 ORDER BY nextReviewEpochDay, createdAtMillis")
    fun observeQuestions(): Flow<List<ReviewQuestionEntity>>

    @Insert
    suspend fun insertQuestion(item: ReviewQuestionEntity): Long

    @Query("SELECT * FROM review_questions WHERE id = :id LIMIT 1")
    suspend fun getQuestion(id: Long): ReviewQuestionEntity?

    @Query("UPDATE review_questions SET nextReviewEpochDay = :nextEpochDay, intervalDays = :intervalDays, reviewCount = reviewCount + 1, lastRating = :rating WHERE id = :id")
    suspend fun updateQuestionSchedule(id: Long, nextEpochDay: Long, intervalDays: Int, rating: String)

    @Insert
    suspend fun insertReviewAttempt(item: ReviewAttemptEntity): Long

    @Query("SELECT * FROM review_attempts WHERE questionId = :questionId ORDER BY reviewedAtMillis DESC")
    suspend fun getAttempts(questionId: Long): List<ReviewAttemptEntity>

    @Insert
    suspend fun insertFocusSession(item: FocusSessionEntity): Long

    @Query("SELECT * FROM focus_sessions ORDER BY startedAtMillis DESC LIMIT :limit")
    suspend fun getRecentFocusSessions(limit: Int): List<FocusSessionEntity>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE completed = 1 AND finishedAtMillis BETWEEN :startMillis AND :endMillis")
    suspend fun countCompletedFocusSessionsBetween(startMillis: Long, endMillis: Long): Int

    @Query("SELECT * FROM focus_sessions WHERE finishedAtMillis BETWEEN :startMillis AND :endMillis ORDER BY finishedAtMillis")
    suspend fun getFocusSessionsBetween(startMillis: Long, endMillis: Long): List<FocusSessionEntity>
}
