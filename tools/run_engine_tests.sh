#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KOTLINC="${KOTLINC:-kotlinc}"
JAVA="${JAVA:-java}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
ENGINE="$ROOT/app/src/main/java/br/com/taina/constantia/engine"

cat > "$TMP/SmartStubs.kt" <<'EOF'
package br.com.taina.constantia.core.database

data class FoodEntity(
    val id: Long = 0,
    val name: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val defaultMeasureName: String = "g",
    val defaultMeasureGrams: Double = 1.0,
    val sourceCode: String = "CUSTOM",
    val sourceLabel: String = "Personalizado",
    val active: Boolean = true,
    val createdAtMillis: Long = 0
)

data class ExerciseEntity(
    val code: String,
    val slug: String = code.lowercase(),
    val name: String,
    val equipmentCode: String,
    val movementPattern: String,
    val instructions: String = "",
    val commonErrors: String = "",
    val demoUrl: String? = null,
    val estimatedSetSeconds: Int = 45,
    val active: Boolean = true
)

data class ExerciseMuscleEntity(
    val exerciseCode: String,
    val muscleCode: String,
    val contribution: Double,
    val role: String
)

data class RestrictionEntity(
    val id: Long = 0,
    val restrictionType: String,
    val bodyRegion: String = "",
    val description: String = "",
    val professionalGuidance: String = "",
    val exerciseCode: String? = null,
    val movementPattern: String? = null,
    val active: Boolean = true
)
EOF
cat > "$TMP/TrainingStubs.kt" <<'EOF'
package br.com.taina.constantia.core.database

data class UserProfileEntity(val primaryGoal: String)
data class TrainingProfileEntity(
    val availableDaysPerWeek: Int,
    val experienceLevel: String,
    val normalSessionMinutes: Int
)
EOF
cat > "$TMP/ExperienceLevelStub.kt" <<'EOF'
package br.com.taina.constantia.core.model
enum class ExperienceLevel { BEGINNER, RETURNING, INTERMEDIATE, ADVANCED }
EOF

cat > "$TMP/NutritionConfidenceStub.kt" <<'EOF'
package br.com.taina.constantia.engine
enum class NutritionConfidence { HIGH, MEDIUM, LOW }
EOF

"$KOTLINC" \
  "$TMP/SmartStubs.kt" "$TMP/TrainingStubs.kt" "$TMP/ExperienceLevelStub.kt" "$TMP/NutritionConfidenceStub.kt" \
  "$ENGINE/PomodoroAdaptationEngine.kt" "$ENGINE/StudyReviewEngine.kt" "$ENGINE/StudyScheduleEngine.kt" \
  "$ENGINE/NotificationPlannerEngine.kt" "$ENGINE/RomanQuoteLibrary.kt" "$ENGINE/FocusGateEngine.kt" \
  "$ENGINE/FocusGateNetworkPlanEngine.kt" "$ENGINE/ProgressEngine.kt" \
  "$ENGINE/SmartMealParser.kt" "$ENGINE/StudyQuestionGenerator.kt" "$ENGINE/ExerciseAdaptationEngine.kt" "$ENGINE/TrainingScheduleEngine.kt" "$ENGINE/TrainingPrescriptionEngine.kt" \
  "$ENGINE/TrainingTechniqueEngine.kt" "$ENGINE/CompletionFeedbackLibrary.kt" \
  "$ROOT/engine-spec/ActivityScheduleSpec.kt" \
  "$ROOT/engine-spec/V04StudyEngineSpec.kt" \
  "$ROOT/engine-spec/V05ContextEngineSpec.kt" \
  "$ROOT/engine-spec/V06FocusGateNetworkSpec.kt" \
  "$ROOT/engine-spec/V07NutritionEngineSpec.kt" \
  "$ROOT/engine-spec/V08ProgressEngineSpec.kt" \
  "$ROOT/engine-spec/V09SmartEngineSpec.kt" "$ROOT/engine-spec/RC3ValidationSpec.kt" "$ROOT/engine-spec/RC31TechniqueSpec.kt" \
  -include-runtime -d "$TMP/engine-tests.jar" >/dev/null

for cls in \
  ActivityScheduleSpecKt \
  V04StudyEngineSpecKt \
  V05ContextEngineSpecKt \
  V06FocusGateNetworkSpecKt \
  V07NutritionEngineSpecKt \
  V08ProgressEngineSpecKt \
  V09SmartEngineSpecKt \
  RC3ValidationSpecKt \
  RC31TechniqueSpecKt; do
  "$JAVA" -cp "$TMP/engine-tests.jar" "$cls"
done

python3 "$ROOT/engine-spec/v09_migration_smoke_test.py"
python3 "$ROOT/tools/preflight.py"
