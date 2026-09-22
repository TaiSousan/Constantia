package br.com.taina.constantia.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Migrações reais a partir do primeiro banco constantia.db (v2). */
object DatabaseMigrations {
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // training_profile ganhou dias preferidos. Recriamos para não deixar DEFAULT persistente no schema final.
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `training_profile_new` (
                    `id` INTEGER NOT NULL,
                    `experienceLevel` TEXT NOT NULL,
                    `currentTrainingDaysPerWeek` INTEGER NOT NULL,
                    `availableDaysPerWeek` INTEGER NOT NULL,
                    `normalSessionMinutes` INTEGER NOT NULL,
                    `minimumSessionMinutes` INTEGER NOT NULL,
                    `preferredTrainingMinuteOfDay` INTEGER,
                    `preferredTrainingDaysCsv` TEXT NOT NULL,
                    `gymType` TEXT NOT NULL,
                    `currentPlanNotes` TEXT NOT NULL,
                    `updatedAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO training_profile_new (
                    id, experienceLevel, currentTrainingDaysPerWeek, availableDaysPerWeek,
                    normalSessionMinutes, minimumSessionMinutes, preferredTrainingMinuteOfDay,
                    preferredTrainingDaysCsv, gymType, currentPlanNotes, updatedAtMillis
                )
                SELECT id, experienceLevel, currentTrainingDaysPerWeek, availableDaysPerWeek,
                    normalSessionMinutes, minimumSessionMinutes, preferredTrainingMinuteOfDay,
                    '', gymType, currentPlanNotes, updatedAtMillis
                FROM training_profile
            """.trimIndent())
            db.execSQL("DROP TABLE training_profile")
            db.execSQL("ALTER TABLE training_profile_new RENAME TO training_profile")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `restrictions_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `restrictionType` TEXT NOT NULL,
                    `bodyRegion` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `professionalGuidance` TEXT NOT NULL,
                    `exerciseCode` TEXT,
                    `movementPattern` TEXT,
                    `temporary` INTEGER NOT NULL,
                    `active` INTEGER NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO restrictions_new (
                    id, restrictionType, bodyRegion, description, professionalGuidance,
                    exerciseCode, movementPattern, temporary, active, createdAtMillis
                )
                SELECT id, 'GENERAL', bodyRegion, description, professionalGuidance,
                    NULL, NULL, 0, active, createdAtMillis
                FROM restrictions
            """.trimIndent())
            db.execSQL("DROP TABLE restrictions")
            db.execSQL("ALTER TABLE restrictions_new RENAME TO restrictions")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_restrictions_exerciseCode ON restrictions(exerciseCode)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_restrictions_movementPattern ON restrictions(movementPattern)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `equipment_availability` (
                    `equipmentCode` TEXT NOT NULL,
                    `available` INTEGER NOT NULL,
                    `note` TEXT NOT NULL,
                    `updatedAtMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`equipmentCode`),
                    FOREIGN KEY(`equipmentCode`) REFERENCES `equipment`(`code`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_equipment_availability_equipmentCode ON equipment_availability(equipmentCode)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `workout_substitutions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `sessionId` INTEGER NOT NULL,
                    `workoutExerciseId` INTEGER NOT NULL,
                    `plannedExerciseCode` TEXT NOT NULL,
                    `executedExerciseCode` TEXT NOT NULL,
                    `reason` TEXT NOT NULL,
                    `createdAtMillis` INTEGER NOT NULL,
                    FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`workoutExerciseId`) REFERENCES `workout_exercises`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_substitutions_sessionId ON workout_substitutions(sessionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_substitutions_workoutExerciseId ON workout_substitutions(workoutExerciseId)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS subjects (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, active INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS study_topics (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, subjectId INTEGER NOT NULL, name TEXT NOT NULL, notes TEXT NOT NULL, active INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL, FOREIGN KEY(subjectId) REFERENCES subjects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_study_topics_subjectId ON study_topics(subjectId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS study_goals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, subjectId INTEGER NOT NULL, topicId INTEGER, sessionsPerWeek INTEGER NOT NULL, targetMinutesPerSession INTEGER NOT NULL, preferredMinuteOfDay INTEGER, active INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL, FOREIGN KEY(subjectId) REFERENCES subjects(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_study_goals_subjectId ON study_goals(subjectId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS study_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, subjectId INTEGER NOT NULL, topicId INTEGER, plannedMinutes INTEGER NOT NULL, actualMinutes INTEGER NOT NULL, startedAtMillis INTEGER NOT NULL, finishedAtMillis INTEGER NOT NULL, difficulty TEXT NOT NULL, notes TEXT NOT NULL, FOREIGN KEY(subjectId) REFERENCES subjects(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(topicId) REFERENCES study_topics(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_study_sessions_subjectId ON study_sessions(subjectId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_study_sessions_topicId ON study_sessions(topicId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_study_sessions_startedAtMillis ON study_sessions(startedAtMillis)")
            db.execSQL("CREATE TABLE IF NOT EXISTS review_questions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, topicId INTEGER NOT NULL, prompt TEXT NOT NULL, answer TEXT NOT NULL, sourceLabel TEXT NOT NULL, active INTEGER NOT NULL, nextReviewEpochDay INTEGER NOT NULL, intervalDays INTEGER NOT NULL, reviewCount INTEGER NOT NULL, lastRating TEXT, createdAtMillis INTEGER NOT NULL, FOREIGN KEY(topicId) REFERENCES study_topics(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_review_questions_topicId ON review_questions(topicId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_review_questions_nextReviewEpochDay ON review_questions(nextReviewEpochDay)")
            db.execSQL("CREATE TABLE IF NOT EXISTS review_attempts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, questionId INTEGER NOT NULL, rating TEXT NOT NULL, previousIntervalDays INTEGER NOT NULL, nextIntervalDays INTEGER NOT NULL, reviewedAtMillis INTEGER NOT NULL, FOREIGN KEY(questionId) REFERENCES review_questions(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_review_attempts_questionId ON review_attempts(questionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_review_attempts_reviewedAtMillis ON review_attempts(reviewedAtMillis)")
            db.execSQL("CREATE TABLE IF NOT EXISTS focus_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, subjectId INTEGER, topicId INTEGER, label TEXT NOT NULL, plannedMinutes INTEGER NOT NULL, actualMinutes INTEGER NOT NULL, interruptions INTEGER NOT NULL, completed INTEGER NOT NULL, startedAtMillis INTEGER NOT NULL, finishedAtMillis INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_startedAtMillis ON focus_sessions(startedAtMillis)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_topicId ON focus_sessions(topicId)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS notification_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dedupeKey TEXT NOT NULL, type TEXT NOT NULL, title TEXT NOT NULL, body TEXT NOT NULL, sourceRef TEXT NOT NULL, plannedAtMillis INTEGER NOT NULL, plannedMinuteOfDay INTEGER NOT NULL, status TEXT NOT NULL, deliveredAtMillis INTEGER, openedAtMillis INTEGER, dismissedAtMillis INTEGER, createdAtMillis INTEGER NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notification_events_dedupeKey ON notification_events(dedupeKey)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_events_type ON notification_events(type)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_events_plannedAtMillis ON notification_events(plannedAtMillis)")
            db.execSQL("CREATE TABLE IF NOT EXISTS focus_gate_rules (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, appLabel TEXT NOT NULL, packageName TEXT NOT NULL, conditionType TEXT NOT NULL, threshold INTEGER NOT NULL, emergencyMinutes INTEGER NOT NULL, active INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_focus_gate_rules_packageName ON focus_gate_rules(packageName)")
            db.execSQL("CREATE TABLE IF NOT EXISTS focus_gate_overrides (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ruleId INTEGER NOT NULL, untilMillis INTEGER NOT NULL, reason TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, FOREIGN KEY(ruleId) REFERENCES focus_gate_rules(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_gate_overrides_ruleId ON focus_gate_overrides(ruleId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_gate_overrides_untilMillis ON focus_gate_overrides(untilMillis)")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS foods (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, kcalPer100g REAL NOT NULL, proteinPer100g REAL NOT NULL, carbsPer100g REAL NOT NULL, fatPer100g REAL NOT NULL, defaultMeasureName TEXT NOT NULL, defaultMeasureGrams REAL NOT NULL, sourceCode TEXT NOT NULL, sourceLabel TEXT NOT NULL, active INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_foods_name ON foods(name)")
            db.execSQL("CREATE TABLE IF NOT EXISTS nutrition_goals (id INTEGER NOT NULL, dailyCalories INTEGER, dailyProteinGrams INTEGER, dailyCarbsGrams INTEGER, dailyFatGrams INTEGER, updatedAtMillis INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("CREATE TABLE IF NOT EXISTS meals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, epochDay INTEGER NOT NULL, mealType TEXT NOT NULL, minuteOfDay INTEGER, notes TEXT NOT NULL, createdAtMillis INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meals_epochDay ON meals(epochDay)")
            db.execSQL("CREATE TABLE IF NOT EXISTS food_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mealId INTEGER NOT NULL, foodId INTEGER, foodNameSnapshot TEXT NOT NULL, amountValue REAL NOT NULL, amountUnit TEXT NOT NULL, estimatedGrams REAL NOT NULL, kcal REAL NOT NULL, proteinGrams REAL NOT NULL, carbsGrams REAL NOT NULL, fatGrams REAL NOT NULL, confidence TEXT NOT NULL, notes TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, FOREIGN KEY(mealId) REFERENCES meals(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_food_entries_mealId ON food_entries(mealId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_food_entries_foodId ON food_entries(foodId)")
        }
    }

    val ALL = arrayOf(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}
