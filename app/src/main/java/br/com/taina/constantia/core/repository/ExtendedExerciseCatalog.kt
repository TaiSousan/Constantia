package br.com.taina.constantia.core.repository

import br.com.taina.constantia.core.database.ExerciseEntity
import br.com.taina.constantia.core.database.ExerciseMuscleEntity

/**
 * Catálogo complementar curado para o RC3.1.
 *
 * A lista amplia alternativas entre máquinas, pesos livres, peso corporal,
 * faixas, kettlebells e TRX. As instruções são autorais e deliberadamente
 * curtas. O app não incorpora imagens/GIFs de terceiros; referências externas
 * ficam documentadas em EXERCISE_SOURCES.md.
 */
object ExtendedExerciseCatalog {
    private data class Def(
        val code: String,
        val slug: String,
        val name: String,
        val equipment: String,
        val pattern: String,
        val primary: String,
        val secondary: List<Pair<String, Double>> = emptyList(),
        val timed: Boolean = false
    )

    private val defs = listOf(
        Def("HACK_SQUAT_MACHINE", "hack-squat", "Hack squat na máquina", "HACK_SQUAT", "SQUAT", "QUADS", listOf("GLUTES" to 0.75), false),
        Def("HORIZONTAL_LEG_PRESS", "leg-press-horizontal", "Leg press horizontal", "HORIZONTAL_LEG_PRESS", "KNEE_HIP_EXTENSION", "QUADS", listOf("GLUTES" to 0.5), false),
        Def("SINGLE_LEG_PRESS", "leg-press-unilateral", "Leg press unilateral", "LEG_PRESS", "KNEE_HIP_EXTENSION", "QUADS", listOf("GLUTES" to 0.5), false),
        Def("SMITH_REVERSE_LUNGE", "afundo-reverso-smith", "Afundo reverso no Smith", "SMITH", "LUNGE", "QUADS", listOf("GLUTES" to 0.75), false),
        Def("SMITH_BULGARIAN_SPLIT_SQUAT", "agachamento-bulgaro-smith", "Agachamento búlgaro no Smith", "SMITH", "LUNGE", "QUADS", listOf("GLUTES" to 0.75), false),
        Def("DB_WALKING_LUNGE", "passada-halteres", "Passada caminhando com halteres", "DUMBBELLS", "LUNGE", "QUADS", listOf("GLUTES" to 0.75), false),
        Def("BARBELL_REVERSE_LUNGE", "afundo-reverso-barra", "Afundo reverso com barra", "BARBELL", "LUNGE", "QUADS", listOf("GLUTES" to 0.75), false),
        Def("BARBELL_FRONT_SQUAT", "agachamento-frontal-barra", "Agachamento frontal com barra", "BARBELL", "SQUAT", "QUADS", listOf("GLUTES" to 0.5), false),
        Def("DB_SUMO_SQUAT", "agachamento-sumo-halter", "Agachamento sumô com halter", "DUMBBELLS", "SQUAT", "GLUTES", listOf("QUADS" to 0.75, "ADDUCTORS" to 0.75), false),
        Def("HEEL_ELEVATED_GOBLET_SQUAT", "goblet-calcanhar-elevado", "Goblet squat com calcanhares elevados", "DUMBBELLS", "SQUAT", "QUADS", listOf("GLUTES" to 0.5), false),
        Def("BARBELL_BOX_SQUAT", "box-squat-barra", "Box squat com barra", "BARBELL", "SQUAT", "QUADS", listOf("GLUTES" to 0.75), false),
        Def("LEG_EXTENSION_UNILATERAL", "extensora-unilateral", "Cadeira extensora unilateral", "LEG_EXTENSION", "KNEE_EXTENSION", "QUADS", emptyList(), false),
        Def("LEG_CURL_UNILATERAL", "flexora-unilateral", "Flexora unilateral", "LEG_CURL", "KNEE_FLEXION", "HAMSTRINGS", emptyList(), false),
        Def("NORDIC_CURL_ASSISTED", "nordic-assistido", "Nordic curl assistido", "BODYWEIGHT", "KNEE_FLEXION", "HAMSTRINGS", emptyList(), false),
        Def("SLIDING_LEG_CURL", "flexora-deslizante", "Flexão de joelhos deslizante", "BODYWEIGHT", "KNEE_FLEXION", "HAMSTRINGS", listOf("GLUTES" to 0.25), false),
        Def("BARBELL_GOOD_MORNING", "good-morning-barra", "Good morning com barra", "BARBELL", "HIP_HINGE", "HAMSTRINGS", listOf("GLUTES" to 0.75, "LOWER_BACK" to 0.5), false),
        Def("DB_SINGLE_LEG_RDL", "rdl-unilateral-halteres", "Levantamento romeno unilateral com halteres", "DUMBBELLS", "HIP_HINGE", "HAMSTRINGS", listOf("GLUTES" to 0.75), false),
        Def("CABLE_PULL_THROUGH", "pull-through-polia", "Pull-through na polia", "CABLE", "HIP_HINGE", "GLUTES", listOf("HAMSTRINGS" to 0.75), false),
        Def("KETTLEBELL_DEADLIFT", "terra-kettlebell", "Levantamento terra com kettlebell", "KETTLEBELL", "HIP_HINGE", "GLUTES", listOf("HAMSTRINGS" to 0.75, "LOWER_BACK" to 0.25), false),
        Def("BARBELL_SUMO_DEADLIFT", "terra-sumo-barra", "Levantamento terra sumô", "BARBELL", "HIP_HINGE", "GLUTES", listOf("HAMSTRINGS" to 0.75, "ADDUCTORS" to 0.75, "QUADS" to 0.5), false),
        Def("BARBELL_CONVENTIONAL_DEADLIFT", "terra-convencional", "Levantamento terra convencional", "BARBELL", "HIP_HINGE", "HAMSTRINGS", listOf("GLUTES" to 0.75, "LOWER_BACK" to 0.5), false),
        Def("HIP_THRUST_MACHINE", "hip-thrust-maquina", "Hip thrust na máquina", "HIP_THRUST_MACHINE", "HIP_EXTENSION", "GLUTES", listOf("HAMSTRINGS" to 0.25), false),
        Def("BARBELL_GLUTE_BRIDGE", "ponte-gluteos-barra", "Ponte de glúteos com barra", "BARBELL", "HIP_EXTENSION", "GLUTES", listOf("HAMSTRINGS" to 0.25), false),
        Def("FROG_PUMP", "frog-pump", "Frog pump", "BODYWEIGHT", "HIP_EXTENSION", "GLUTES", emptyList(), false),
        Def("CABLE_GLUTE_KICKBACK", "coice-polia", "Extensão de quadril na polia", "CABLE", "HIP_EXTENSION", "GLUTES", listOf("HAMSTRINGS" to 0.25), false),
        Def("DONKEY_KICK", "coice-peso-corporal", "Coice de glúteo no solo", "BODYWEIGHT", "HIP_EXTENSION", "GLUTES", emptyList(), false),
        Def("FIRE_HYDRANT", "fire-hydrant", "Abdução de quadril em quatro apoios", "BODYWEIGHT", "HIP_ABDUCTION", "GLUTES", emptyList(), false),
        Def("BAND_LATERAL_WALK", "caminhada-lateral-elastico", "Caminhada lateral com faixa elástica", "BAND", "HIP_ABDUCTION", "GLUTES", emptyList(), false),
        Def("COPENHAGEN_PLANK", "prancha-copenhagen", "Prancha Copenhagen", "BODYWEIGHT", "ISOMETRIC_ADDUCTION", "ADDUCTORS", listOf("CORE" to 0.5), true),
        Def("WALL_SIT", "cadeira-parede", "Isometria na parede (wall sit)", "BODYWEIGHT", "ISOMETRIC_LOWER", "QUADS", listOf("GLUTES" to 0.5), true),
        Def("SPLIT_SQUAT_ISOMETRIC", "afundo-isometrico", "Afundo isométrico", "BODYWEIGHT", "ISOMETRIC_LOWER", "QUADS", listOf("GLUTES" to 0.5), true),
        Def("SEATED_CALF_MACHINE", "panturrilha-sentada-maquina", "Panturrilha sentada na máquina", "SEATED_CALF", "PLANTAR_FLEXION", "CALVES", emptyList(), false),
        Def("SMITH_CALF_RAISE", "panturrilha-smith", "Panturrilha em pé no Smith", "SMITH", "PLANTAR_FLEXION", "CALVES", emptyList(), false),
        Def("SINGLE_LEG_CALF_RAISE", "panturrilha-unilateral", "Panturrilha unilateral com peso corporal", "BODYWEIGHT", "PLANTAR_FLEXION", "CALVES", emptyList(), false),
        Def("TIBIALIS_RAISE", "elevacao-tibial", "Elevação do tibial anterior", "BODYWEIGHT", "DORSIFLEXION", "CALVES", emptyList(), false),
        Def("BARBELL_INCLINE_BENCH", "supino-inclinado-barra", "Supino inclinado com barra", "BARBELL", "INCLINE_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.75), false),
        Def("BARBELL_DECLINE_BENCH", "supino-declinado-barra", "Supino declinado com barra", "BARBELL", "HORIZONTAL_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.25), false),
        Def("DB_DECLINE_PRESS", "supino-declinado-halteres", "Supino declinado com halteres", "DUMBBELLS", "HORIZONTAL_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.25), false),
        Def("DB_FLOOR_PRESS", "floor-press-halteres", "Floor press com halteres", "DUMBBELLS", "HORIZONTAL_PUSH", "CHEST", listOf("TRICEPS" to 0.75, "FRONT_DELTS" to 0.25), false),
        Def("BARBELL_FLOOR_PRESS", "floor-press-barra", "Floor press com barra", "BARBELL", "HORIZONTAL_PUSH", "CHEST", listOf("TRICEPS" to 0.75, "FRONT_DELTS" to 0.25), false),
        Def("CABLE_CHEST_FLY", "crucifixo-polia", "Crucifixo na polia", "CABLE", "HORIZONTAL_ADDUCTION", "CHEST", emptyList(), false),
        Def("CABLE_LOW_HIGH_FLY", "crucifixo-baixo-alto", "Crucifixo na polia de baixo para cima", "CABLE", "HORIZONTAL_ADDUCTION", "CHEST", listOf("FRONT_DELTS" to 0.25), false),
        Def("CABLE_HIGH_LOW_FLY", "crucifixo-alto-baixo", "Crucifixo na polia de cima para baixo", "CABLE", "HORIZONTAL_ADDUCTION", "CHEST", emptyList(), false),
        Def("DB_FLY", "crucifixo-reto-halteres", "Crucifixo reto com halteres", "DUMBBELLS", "HORIZONTAL_ADDUCTION", "CHEST", emptyList(), false),
        Def("INCLINE_DB_FLY", "crucifixo-inclinado-halteres", "Crucifixo inclinado com halteres", "DUMBBELLS", "HORIZONTAL_ADDUCTION", "CHEST", listOf("FRONT_DELTS" to 0.25), false),
        Def("INCLINE_PUSH_UP", "flexao-inclinada", "Flexão inclinada", "BODYWEIGHT", "HORIZONTAL_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.5), false),
        Def("DECLINE_PUSH_UP", "flexao-declinada", "Flexão declinada", "BODYWEIGHT", "INCLINE_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.75), false),
        Def("WIDE_PUSH_UP", "flexao-aberta", "Flexão com mãos afastadas", "BODYWEIGHT", "HORIZONTAL_PUSH", "CHEST", listOf("TRICEPS" to 0.25, "FRONT_DELTS" to 0.5), false),
        Def("NEUTRAL_GRIP_PULLDOWN", "puxada-neutra", "Puxada frontal com pegada neutra", "LAT_PULLDOWN", "VERTICAL_PULL", "LATS", listOf("BICEPS" to 0.5, "UPPER_BACK" to 0.25), false),
        Def("UNDERHAND_PULLDOWN", "puxada-supinada", "Puxada frontal supinada", "LAT_PULLDOWN", "VERTICAL_PULL", "LATS", listOf("BICEPS" to 0.75), false),
        Def("SINGLE_ARM_PULLDOWN", "puxada-unilateral", "Puxada unilateral na polia", "CABLE", "VERTICAL_PULL", "LATS", listOf("BICEPS" to 0.5), false),
        Def("CHEST_SUPPORTED_ROW_MACHINE", "remada-maquina-peito-apoiado", "Remada máquina com peito apoiado", "CHEST_SUPPORTED_ROW", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5, "REAR_DELTS" to 0.25), false),
        Def("CHEST_SUPPORTED_DB_ROW", "remada-halteres-peito-apoiado", "Remada com halteres e peito apoiado", "DUMBBELLS", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5, "REAR_DELTS" to 0.25), false),
        Def("T_BAR_ROW", "remada-t-bar", "Remada T-bar", "LANDMINE", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5, "REAR_DELTS" to 0.25), false),
        Def("CABLE_ONE_ARM_ROW", "remada-unilateral-polia", "Remada unilateral na polia", "CABLE", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5), false),
        Def("WIDE_GRIP_SEATED_ROW", "remada-aberta-polia", "Remada sentada com pegada aberta", "SEATED_ROW", "HORIZONTAL_PULL", "UPPER_BACK", listOf("REAR_DELTS" to 0.5, "BICEPS" to 0.25), false),
        Def("STRAIGHT_ARM_PULLDOWN", "pulldown-bracos-estendidos", "Pulldown com braços estendidos", "CABLE", "SHOULDER_EXTENSION", "LATS", listOf("TRICEPS" to 0.15), false),
        Def("CHIN_UP", "barra-fixa-supinada", "Barra fixa supinada (chin-up)", "PULLUP_BAR", "VERTICAL_PULL", "LATS", listOf("BICEPS" to 0.75, "UPPER_BACK" to 0.5), false),
        Def("ASSISTED_PULL_UP", "barra-fixa-assistida", "Barra fixa assistida", "ASSISTED_PULLUP", "VERTICAL_PULL", "LATS", listOf("BICEPS" to 0.5, "UPPER_BACK" to 0.5), false),
        Def("SCAPULAR_PULL_UP", "barra-escapular", "Elevação escapular na barra fixa", "PULLUP_BAR", "SCAPULAR_DEPRESSION", "LATS", listOf("UPPER_BACK" to 0.75), false),
        Def("PRONE_Y_RAISE", "elevacao-y", "Elevação em Y deitada", "BODYWEIGHT", "SCAPULAR_CONTROL", "UPPER_BACK", listOf("REAR_DELTS" to 0.5), false),
        Def("BACK_EXTENSION_45", "extensao-lombar-45", "Extensão de tronco no banco 45°", "BACK_EXTENSION", "HIP_EXTENSION", "LOWER_BACK", listOf("GLUTES" to 0.5, "HAMSTRINGS" to 0.5), false),
        Def("SUPERMAN", "superman", "Superman no solo", "BODYWEIGHT", "SPINAL_EXTENSION", "LOWER_BACK", listOf("GLUTES" to 0.25), false),
        Def("DEAD_HANG", "dead-hang", "Suspensão passiva na barra", "PULLUP_BAR", "GRIP_ISOMETRIC", "FOREARMS", listOf("LATS" to 0.25), true),
        Def("ARNOLD_PRESS", "arnold-press", "Arnold press com halteres", "DUMBBELLS", "VERTICAL_PUSH", "FRONT_DELTS", listOf("SIDE_DELTS" to 0.5, "TRICEPS" to 0.5), false),
        Def("SEATED_DB_PRESS", "desenvolvimento-halteres-sentado", "Desenvolvimento sentado com halteres", "DUMBBELLS", "VERTICAL_PUSH", "FRONT_DELTS", listOf("SIDE_DELTS" to 0.5, "TRICEPS" to 0.5), false),
        Def("CABLE_LATERAL_RAISE", "elevacao-lateral-polia", "Elevação lateral na polia", "CABLE", "SHOULDER_ABDUCTION", "SIDE_DELTS", emptyList(), false),
        Def("LATERAL_RAISE_MACHINE", "elevacao-lateral-maquina", "Elevação lateral na máquina", "LATERAL_RAISE_MACHINE", "SHOULDER_ABDUCTION", "SIDE_DELTS", emptyList(), false),
        Def("PLATE_FRONT_RAISE", "elevacao-frontal-anilha", "Elevação frontal com anilha", "PLATE", "SHOULDER_FLEXION", "FRONT_DELTS", emptyList(), false),
        Def("DB_FRONT_RAISE", "elevacao-frontal-halteres", "Elevação frontal com halteres", "DUMBBELLS", "SHOULDER_FLEXION", "FRONT_DELTS", emptyList(), false),
        Def("REVERSE_PEC_DECK", "voador-inverso", "Voador inverso / peck deck reverso", "REVERSE_PEC_DECK", "SHOULDER_HORIZONTAL_ABDUCTION", "REAR_DELTS", listOf("UPPER_BACK" to 0.5), false),
        Def("CABLE_REAR_DELT_FLY", "crucifixo-inverso-polia", "Crucifixo inverso na polia", "CABLE", "SHOULDER_HORIZONTAL_ABDUCTION", "REAR_DELTS", listOf("UPPER_BACK" to 0.5), false),
        Def("CABLE_UPRIGHT_ROW", "remada-alta-polia", "Remada alta na polia", "CABLE", "SHOULDER_ABDUCTION", "SIDE_DELTS", listOf("TRAPS" to 0.5), false),
        Def("LANDMINE_PRESS", "landmine-press", "Desenvolvimento landmine unilateral", "LANDMINE", "VERTICAL_PUSH", "FRONT_DELTS", listOf("TRICEPS" to 0.5, "CHEST" to 0.25), false),
        Def("INCLINE_DB_CURL", "rosca-inclinada-halteres", "Rosca inclinada com halteres", "DUMBBELLS", "ELBOW_FLEXION", "BICEPS", emptyList(), false),
        Def("CONCENTRATION_CURL", "rosca-concentrada", "Rosca concentrada", "DUMBBELLS", "ELBOW_FLEXION", "BICEPS", emptyList(), false),
        Def("CABLE_CURL", "rosca-polia", "Rosca na polia", "CABLE", "ELBOW_FLEXION", "BICEPS", emptyList(), false),
        Def("BAYESIAN_CABLE_CURL", "rosca-bayesiana", "Rosca bayesiana na polia", "CABLE", "ELBOW_FLEXION", "BICEPS", emptyList(), false),
        Def("EZ_BAR_CURL", "rosca-barra-w", "Rosca direta com barra W", "EZ_BAR", "ELBOW_FLEXION", "BICEPS", listOf("FOREARMS" to 0.25), false),
        Def("REVERSE_BARBELL_CURL", "rosca-inversa-barra", "Rosca inversa com barra", "BARBELL", "ELBOW_FLEXION", "BICEPS", listOf("FOREARMS" to 0.75), false),
        Def("CROSS_BODY_HAMMER_CURL", "rosca-martelo-cruzada", "Rosca martelo cruzada", "DUMBBELLS", "ELBOW_FLEXION", "BICEPS", listOf("FOREARMS" to 0.5), false),
        Def("OVERHEAD_CABLE_TRICEPS", "triceps-polia-acima-cabeca", "Tríceps acima da cabeça na polia", "CABLE", "ELBOW_EXTENSION", "TRICEPS", emptyList(), false),
        Def("EZ_SKULL_CRUSHER", "triceps-testa-barra-w", "Tríceps testa com barra W", "EZ_BAR", "ELBOW_EXTENSION", "TRICEPS", emptyList(), false),
        Def("DB_TRICEPS_KICKBACK", "coice-triceps-halter", "Coice de tríceps com halter", "DUMBBELLS", "ELBOW_EXTENSION", "TRICEPS", emptyList(), false),
        Def("BENCH_DIP", "mergulho-banco", "Mergulho no banco", "BODYWEIGHT", "ELBOW_EXTENSION", "TRICEPS", listOf("CHEST" to 0.25, "FRONT_DELTS" to 0.25), false),
        Def("ASSISTED_DIP", "paralelas-assistidas", "Paralelas assistidas", "DIP_ASSIST", "ELBOW_EXTENSION", "TRICEPS", listOf("CHEST" to 0.5, "FRONT_DELTS" to 0.25), false),
        Def("CLOSE_GRIP_BENCH_PRESS", "supino-pegada-fechada", "Supino com pegada fechada", "BARBELL", "HORIZONTAL_PUSH", "TRICEPS", listOf("CHEST" to 0.75, "FRONT_DELTS" to 0.25), false),
        Def("SIDE_PLANK", "prancha-lateral", "Prancha lateral", "BODYWEIGHT", "CORE_STABILITY", "CORE", emptyList(), true),
        Def("HOLLOW_BODY_HOLD", "hollow-hold", "Hollow body hold", "BODYWEIGHT", "CORE_STABILITY", "CORE", emptyList(), true),
        Def("BIRD_DOG", "bird-dog", "Bird dog", "BODYWEIGHT", "CORE_STABILITY", "CORE", listOf("GLUTES" to 0.25, "LOWER_BACK" to 0.25), false),
        Def("MOUNTAIN_CLIMBER", "mountain-climber", "Mountain climber", "BODYWEIGHT", "CORE_DYNAMIC", "CORE", listOf("HIP_FLEXORS" to 0.25), false),
        Def("BICYCLE_CRUNCH", "abdominal-bicicleta", "Abdominal bicicleta", "BODYWEIGHT", "TRUNK_FLEXION", "CORE", emptyList(), false),
        Def("BODYWEIGHT_CRUNCH", "abdominal-crunch", "Abdominal crunch", "BODYWEIGHT", "TRUNK_FLEXION", "CORE", emptyList(), false),
        Def("HANGING_KNEE_RAISE", "elevacao-joelhos-barra", "Elevação de joelhos na barra", "PULLUP_BAR", "HIP_FLEXION_CORE", "CORE", emptyList(), false),
        Def("HANGING_LEG_RAISE", "elevacao-pernas-barra", "Elevação de pernas na barra", "PULLUP_BAR", "HIP_FLEXION_CORE", "CORE", emptyList(), false),
        Def("PALLOF_PRESS", "pallof-press", "Pallof press na polia", "CABLE", "ANTI_ROTATION", "CORE", emptyList(), false),
        Def("CABLE_WOODCHOP", "woodchop-polia", "Woodchop na polia", "CABLE", "TRUNK_ROTATION", "CORE", emptyList(), false),
        Def("DB_FARMER_CARRY", "farmer-carry-halteres", "Farmer carry com halteres", "DUMBBELLS", "LOADED_CARRY", "FOREARMS", listOf("TRAPS" to 0.75, "CORE" to 0.5), true),
        Def("DB_SUITCASE_CARRY", "suitcase-carry-halter", "Suitcase carry unilateral", "DUMBBELLS", "LOADED_CARRY", "CORE", listOf("FOREARMS" to 0.75, "TRAPS" to 0.5), true),
        Def("DB_SIDE_BEND", "inclinacao-lateral-halter", "Inclinação lateral com halter", "DUMBBELLS", "LATERAL_FLEXION", "CORE", emptyList(), false),
        Def("DB_WRIST_CURL", "flexao-punho-halteres", "Flexão de punho com halteres", "DUMBBELLS", "WRIST_FLEXION", "FOREARMS", emptyList(), false),
        Def("DB_REVERSE_WRIST_CURL", "extensao-punho-halteres", "Extensão de punho com halteres", "DUMBBELLS", "WRIST_EXTENSION", "FOREARMS", emptyList(), false),
        Def("BARBELL_WRIST_CURL", "flexao-punho-barra", "Flexão de punho com barra", "BARBELL", "WRIST_FLEXION", "FOREARMS", emptyList(), false),
        Def("PLATE_PINCH_HOLD", "pinch-anilha", "Pinça isométrica com anilhas", "PLATE", "GRIP_ISOMETRIC", "FOREARMS", emptyList(), true),
        Def("BAND_FACE_PULL", "face-pull-faixa", "Face pull com faixa elástica", "BAND", "SHOULDER_HORIZONTAL_ABDUCTION", "REAR_DELTS", listOf("UPPER_BACK" to 0.5), false),
        Def("BAND_PULL_APART", "band-pull-apart", "Band pull-apart", "BAND", "SHOULDER_HORIZONTAL_ABDUCTION", "REAR_DELTS", listOf("UPPER_BACK" to 0.5), false),
        Def("BAND_CHEST_PRESS", "supino-faixa", "Chest press com faixa elástica", "BAND", "HORIZONTAL_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.5), false),
        Def("BAND_ROW", "remada-faixa", "Remada com faixa elástica", "BAND", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5), false),
        Def("BAND_BICEPS_CURL", "rosca-faixa", "Rosca bíceps com faixa elástica", "BAND", "ELBOW_FLEXION", "BICEPS", emptyList(), false),
        Def("BAND_TRICEPS_PRESSDOWN", "triceps-faixa", "Tríceps com faixa elástica", "BAND", "ELBOW_EXTENSION", "TRICEPS", emptyList(), false),
        Def("KETTLEBELL_SWING", "swing-kettlebell", "Kettlebell swing", "KETTLEBELL", "HIP_HINGE", "GLUTES", listOf("HAMSTRINGS" to 0.75, "CORE" to 0.25), false),
        Def("KETTLEBELL_GOBLET_SQUAT", "goblet-kettlebell", "Goblet squat com kettlebell", "KETTLEBELL", "SQUAT", "QUADS", listOf("GLUTES" to 0.75), false),
        Def("KETTLEBELL_RDL", "rdl-kettlebell", "Levantamento romeno com kettlebell", "KETTLEBELL", "HIP_HINGE", "HAMSTRINGS", listOf("GLUTES" to 0.75), false),
        Def("KETTLEBELL_ONE_ARM_ROW", "remada-kettlebell", "Remada unilateral com kettlebell", "KETTLEBELL", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5), false),
        Def("KETTLEBELL_OHP", "desenvolvimento-kettlebell", "Desenvolvimento com kettlebell", "KETTLEBELL", "VERTICAL_PUSH", "FRONT_DELTS", listOf("TRICEPS" to 0.5, "SIDE_DELTS" to 0.25), false),
        Def("TRX_ROW", "remada-trx", "Remada no TRX", "TRX", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5), false),
        Def("TRX_CHEST_PRESS", "flexao-trx", "Chest press / flexão no TRX", "TRX", "HORIZONTAL_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.5), false),
        Def("TRX_SQUAT", "agachamento-trx", "Agachamento assistido no TRX", "TRX", "SQUAT", "QUADS", listOf("GLUTES" to 0.5), false),
        Def("TRX_HAMSTRING_CURL", "flexora-trx", "Flexão de joelhos no TRX", "TRX", "KNEE_FLEXION", "HAMSTRINGS", listOf("GLUTES" to 0.25), false)
    )

    val exercises: List<ExerciseEntity> = defs.map { d ->
        ExerciseEntity(
            code = d.code,
            slug = d.slug,
            name = d.name,
            equipmentCode = d.equipment,
            movementPattern = d.pattern,
            instructions = instructionFor(d),
            commonErrors = errorsFor(d),
            estimatedSetSeconds = if (d.timed) 40 else 45
        )
    }

    val exerciseMuscles: List<ExerciseMuscleEntity> = defs.flatMap { d ->
        buildList {
            add(ExerciseMuscleEntity(d.code, d.primary, 1.0, "PRIMARY"))
            d.secondary.forEach { (muscle, contribution) ->
                add(ExerciseMuscleEntity(d.code, muscle, contribution, "SECONDARY"))
            }
        }
    }

    val timedExerciseCodes: Set<String> =
        defs.filter { it.timed }.mapTo(linkedSetOf()) { it.code }

    private fun instructionFor(d: Def): String = when (d.pattern) {
        "SQUAT", "KNEE_HIP_EXTENSION" ->
            "Mantenha os pés firmes, joelhos acompanhando a direção dos pés e execute a descida e a subida com controle, usando a amplitude que consegue sustentar sem perder posição."
        "LUNGE" ->
            "Mantenha base estável e controle quadril e joelho durante a descida. Retorne pressionando o chão sem usar impulso excessivo."
        "KNEE_EXTENSION" ->
            "Ajuste o equipamento quando aplicável, estenda o joelho de forma controlada e retorne sem deixar a carga despencar."
        "KNEE_FLEXION" ->
            "Flexione os joelhos mantendo quadril e tronco estáveis. Controle a fase de retorno e evite reduzir a amplitude para mover mais carga."
        "HIP_HINGE" ->
            "Leve o quadril para trás com coluna estável e carga próxima ao corpo. Retorne estendendo o quadril sem hiperestender a lombar."
        "HIP_EXTENSION" ->
            "Estenda o quadril contraindo os glúteos e retorne com controle. Termine em posição neutra, sem exagerar a extensão lombar."
        "HIP_ABDUCTION" ->
            "Afaste a perna ou os joelhos contra a resistência mantendo a pelve e o tronco estáveis. Retorne lentamente."
        "HIP_ADDUCTION", "ISOMETRIC_ADDUCTION" ->
            "Mantenha a pelve estável e aproxime ou sustente as pernas em uma posição confortável, interrompendo se houver dor articular."
        "PLANTAR_FLEXION" ->
            "Eleve os calcanhares com controle, faça uma breve contração no topo e desça em amplitude confortável sem quicar."
        "DORSIFLEXION" ->
            "Eleve a ponta dos pés mantendo os calcanhares apoiados e controle o retorno."
        "HORIZONTAL_PUSH", "INCLINE_PUSH" ->
            "Mantenha escápulas e tronco estáveis, desça com controle até uma amplitude confortável e empurre sem perder a posição dos ombros."
        "HORIZONTAL_ADDUCTION" ->
            "Aproxime os braços à frente do corpo mantendo os ombros controlados. Retorne lentamente sem forçar amplitude."
        "VERTICAL_PULL" ->
            "Mantenha o tronco estável e puxe levando os cotovelos para baixo. Controle a volta sem usar balanço."
        "HORIZONTAL_PULL" ->
            "Puxe a carga em direção ao tronco mantendo a coluna estável e as escápulas sob controle. Retorne sem soltar a carga."
        "SHOULDER_EXTENSION" ->
            "Leve os braços para baixo mantendo os cotovelos quase estendidos e o tronco estável. Evite transformar o movimento em balanço."
        "VERTICAL_PUSH" ->
            "Empurre a carga acima da cabeça mantendo abdômen e glúteos firmes. Use amplitude confortável sem compensar com a lombar."
        "SHOULDER_ABDUCTION", "SHOULDER_FLEXION", "SHOULDER_HORIZONTAL_ABDUCTION" ->
            "Mova os braços com controle, sem impulso do tronco, até a amplitude confortável para o ombro e retorne lentamente."
        "ELBOW_FLEXION" ->
            "Mantenha o tronco estável e flexione os cotovelos sem usar balanço. Controle a descida."
        "ELBOW_EXTENSION" ->
            "Estenda os cotovelos mantendo o braço estável e retorne de forma controlada, evitando compensações do tronco."
        "CORE_STABILITY", "ISOMETRIC_LOWER", "GRIP_ISOMETRIC" ->
            "Sustente a posição com respiração normal e tensão controlada. Interrompa a série antes de perder claramente a posição."
        "CORE_DYNAMIC", "TRUNK_FLEXION", "HIP_FLEXION_CORE", "ANTI_ROTATION", "TRUNK_ROTATION", "LATERAL_FLEXION" ->
            "Mantenha o tronco sob controle durante toda a repetição e evite usar impulso para completar a amplitude."
        "SCAPULAR_DEPRESSION", "SCAPULAR_CONTROL" ->
            "Mova as escápulas de forma controlada mantendo braços e tronco estáveis. Evite encolher os ombros sem necessidade."
        "SPINAL_EXTENSION" ->
            "Estenda o tronco até uma posição neutra e retorne com controle, evitando hiperextensão lombar."
        "LOADED_CARRY" ->
            "Caminhe com passos controlados, tronco estável e carga segura. Mantenha respiração normal e encerre antes de perder a postura."
        "WRIST_FLEXION", "WRIST_EXTENSION" ->
            "Movimente o punho de forma controlada, com o antebraço apoiado quando possível e sem usar impulso."
        else ->
            "Execute com amplitude confortável, controle durante toda a repetição e carga compatível com a técnica."
    }

    private fun errorsFor(d: Def): String = when (d.pattern) {
        "SQUAT", "KNEE_HIP_EXTENSION", "LUNGE" ->
            "Perder o apoio dos pés; joelhos colapsarem; usar amplitude ou carga que desorganiza a posição."
        "HIP_HINGE" ->
            "Arredondar a coluna; afastar a carga do corpo; transformar o movimento em agachamento; buscar amplitude além do controle."
        "HORIZONTAL_PUSH", "INCLINE_PUSH", "VERTICAL_PUSH" ->
            "Perder estabilidade dos ombros; usar impulso; compensar com a lombar; insistir em amplitude dolorosa."
        "VERTICAL_PULL", "HORIZONTAL_PULL", "SHOULDER_EXTENSION" ->
            "Balançar o tronco; puxar apenas com os braços; perder controle na volta; usar carga excessiva."
        "CORE_STABILITY", "ISOMETRIC_LOWER", "ISOMETRIC_ADDUCTION", "GRIP_ISOMETRIC" ->
            "Prender a respiração; sustentar depois de perder a posição; transformar desconforto articular em meta de tempo."
        "ELBOW_FLEXION", "ELBOW_EXTENSION", "WRIST_FLEXION", "WRIST_EXTENSION" ->
            "Usar balanço; mover articulações que deveriam permanecer estáveis; deixar a carga cair na volta."
        else ->
            "Usar impulso; perder estabilidade; reduzir o controle para mover mais carga; insistir em dor articular."
    }
}
