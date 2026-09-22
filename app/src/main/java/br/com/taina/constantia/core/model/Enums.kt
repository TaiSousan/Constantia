package br.com.taina.constantia.core.model

enum class Sex { FEMALE, MALE, OTHER, NOT_INFORMED }
enum class TrainingGoal { FAT_LOSS, HYPERTROPHY, STRENGTH, CONDITIONING, HEALTH, MIXED }
enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED, RETURNING }
enum class ActivityCategory { OBLIGATION, HABIT, TASK, LEISURE, STUDY, TRAINING, OTHER }
enum class FrequencyType { DAILY, SPECIFIC_DAYS, TIMES_PER_WEEK, TIMES_PER_MONTH, EVERY_X_DAYS, ONCE }
enum class ActivityPriority { REQUIRED, PLANNED, FLEXIBLE, OPTIONAL }
enum class MissedPolicy { KEEP_PENDING, RESCHEDULE, MARK_MISSED, ASK }
enum class OccurrenceStatus { PENDING, COMPLETED, SKIPPED, MISSED }
enum class RestrictionType { GENERAL, EXERCISE_AVOID, MOVEMENT_AVOID }
enum class StagnationState { NONE, WATCH, PLATEAU }
