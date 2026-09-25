package br.com.taina.constantia.core.repository

import br.com.taina.constantia.core.database.ExerciseEntity
import br.com.taina.constantia.core.database.ExerciseMuscleEntity

/**
 * Exercícios promovidos da biblioteca externa para o catálogo inteligente.
 *
 * Critérios RC3.2:
 * - nome/movimento inequívocos;
 * - equipamento já modelado pelo Constantia;
 * - músculo primário e padrão de movimento mapeáveis com alta confiança;
 * - sem depender de mídia de terceiros;
 * - instruções do app são autorais, não copiadas da base.
 *
 * O ID externo é preservado apenas para rastreabilidade e para marcar o item
 * correspondente na Biblioteca ampliada.
 */
object CuratedDatasetExerciseCatalog {
    private data class Def(
        val code: String,
        val sourceId: String,
        val slug: String,
        val name: String,
        val equipment: String,
        val pattern: String,
        val primary: String,
        val secondary: List<Pair<String, Double>>
    )

    private val defs = listOf(
        Def("DS_1008", "1008", "dataset-1008-step-up-com-faixa-elastica", "Step-up com faixa elástica", "BAND", "LUNGE", "QUADS", listOf("GLUTES" to 0.75, "HAMSTRINGS" to 0.75)),
        Def("DS_1009", "1009", "dataset-1009-levantamento-romeno-com-faixa-elastica", "Levantamento romeno com faixa elástica", "BAND", "HIP_HINGE", "HAMSTRINGS", listOf("GLUTES" to 0.75, "LOWER_BACK" to 0.5)),
        Def("DS_1410", "1410", "dataset-1410-afundo-lateral-com-barra", "Afundo lateral com barra", "BARBELL", "LUNGE", "GLUTES", listOf("QUADS" to 0.75, "ADDUCTORS" to 0.5)),
        Def("DS_0054", "0054", "dataset-0054-afundo-a-frente-com-barra", "Afundo à frente com barra", "BARBELL", "LUNGE", "QUADS", listOf("GLUTES" to 0.75)),
        Def("DS_0114", "0114", "dataset-0114-step-up-com-barra", "Step-up com barra", "BARBELL", "LUNGE", "QUADS", listOf("GLUTES" to 0.75)),
        Def("DS_0168", "0168", "dataset-0168-aducao-de-quadril-na-polia", "Adução de quadril na polia", "CABLE", "HIP_ADDUCTION", "ADDUCTORS", emptyList()),
        Def("DS_0228", "0228", "dataset-0228-extensao-de-quadril-em-pe-na-polia", "Extensão de quadril em pé na polia", "CABLE", "HIP_EXTENSION", "GLUTES", listOf("HAMSTRINGS" to 0.75)),
        Def("DS_0336", "0336", "dataset-0336-afundo-a-frente-com-halteres", "Afundo à frente com halteres", "DUMBBELLS", "LUNGE", "QUADS", listOf("GLUTES" to 0.75)),
        Def("DS_0410", "0410", "dataset-0410-agachamento-dividido-com-halteres", "Agachamento dividido com halteres", "DUMBBELLS", "LUNGE", "QUADS", listOf("GLUTES" to 0.75)),
        Def("DS_0533", "0533", "dataset-0533-agachamento-frontal-com-kettlebells", "Agachamento frontal com kettlebells", "KETTLEBELL", "SQUAT", "QUADS", listOf("GLUTES" to 0.75)),
        Def("DS_0582", "0582", "dataset-0582-flexora-ajoelhada-na-maquina", "Flexora ajoelhada na máquina", "LEG_CURL", "KNEE_FLEXION", "HAMSTRINGS", emptyList()),
        Def("DS_0738", "0738", "dataset-0738-panturrilha-no-leg-press-45", "Panturrilha no leg press 45°", "LEG_PRESS", "PLANTAR_FLEXION", "CALVES", emptyList()),
        Def("DS_0749", "0749", "dataset-0749-good-morning-no-smith", "Good morning no Smith", "SMITH", "HIP_HINGE", "HAMSTRINGS", listOf("GLUTES" to 0.75, "LOWER_BACK" to 0.5)),
        Def("DS_0752", "0752", "dataset-0752-levantamento-terra-no-smith", "Levantamento terra no Smith", "SMITH", "HIP_HINGE", "HAMSTRINGS", listOf("GLUTES" to 0.75, "LOWER_BACK" to 0.5)),
        Def("DS_3142", "3142", "dataset-3142-agachamento-sumo-no-smith", "Agachamento sumô no Smith", "SMITH", "SQUAT", "GLUTES", listOf("QUADS" to 0.75, "ADDUCTORS" to 0.5)),
        Def("DS_3006", "3006", "dataset-3006-abducao-de-quadril-sentada-com-faixa-elastica", "Abdução de quadril sentada com faixa elástica", "BAND", "HIP_ABDUCTION", "GLUTES", emptyList()),
        Def("DS_1379", "1379", "dataset-1379-panturrilha-sentada-com-halteres", "Panturrilha sentada com halteres", "DUMBBELLS", "PLANTAR_FLEXION", "CALVES", emptyList()),
        Def("DS_0999", "0999", "dataset-0999-panturrilha-unilateral-com-faixa-elastica", "Panturrilha unilateral com faixa elástica", "BAND", "PLANTAR_FLEXION", "CALVES", emptyList()),
        Def("DS_0151", "0151", "dataset-0151-supino-reto-na-polia", "Supino reto na polia", "CABLE", "HORIZONTAL_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.5)),
        Def("DS_0169", "0169", "dataset-0169-supino-inclinado-na-polia", "Supino inclinado na polia", "CABLE", "INCLINE_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.75)),
        Def("DS_1298", "1298", "dataset-1298-floor-press-unilateral-com-kettlebell", "Floor press unilateral com kettlebell", "KETTLEBELL", "HORIZONTAL_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.5)),
        Def("DS_0748", "0748", "dataset-0748-supino-reto-no-smith", "Supino reto no Smith", "SMITH", "HORIZONTAL_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.5)),
        Def("DS_0757", "0757", "dataset-0757-supino-inclinado-no-smith", "Supino inclinado no Smith", "SMITH", "INCLINE_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.75)),
        Def("DS_1299", "1299", "dataset-1299-supino-inclinado-na-maquina", "Supino inclinado na máquina", "CHEST_PRESS", "INCLINE_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.75)),
        Def("DS_0659", "0659", "dataset-0659-flexao-de-bracos-na-parede", "Flexão de braços na parede", "BODYWEIGHT", "HORIZONTAL_PUSH", "CHEST", listOf("TRICEPS" to 0.5, "FRONT_DELTS" to 0.5)),
        Def("DS_0974", "0974", "dataset-0974-puxada-pegada-fechada-com-faixa", "Puxada pegada fechada com faixa", "BAND", "VERTICAL_PULL", "LATS", listOf("BICEPS" to 0.5, "UPPER_BACK" to 0.5)),
        Def("DS_0983", "0983", "dataset-0983-puxada-unilateral-ajoelhada-com-faixa", "Puxada unilateral ajoelhada com faixa", "BAND", "VERTICAL_PULL", "LATS", listOf("BICEPS" to 0.5, "UPPER_BACK" to 0.5)),
        Def("DS_3017", "3017", "dataset-3017-remada-pendlay", "Remada Pendlay", "BARBELL", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5, "REAR_DELTS" to 0.25)),
        Def("DS_0049", "0049", "dataset-0049-remada-com-barra-no-banco-inclinado", "Remada com barra no banco inclinado", "BARBELL", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5, "REAR_DELTS" to 0.25)),
        Def("DS_0213", "0213", "dataset-0213-remada-alta-sentada-na-polia", "Remada alta sentada na polia", "CABLE", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5, "REAR_DELTS" to 0.25)),
        Def("DS_0180", "0180", "dataset-0180-remada-baixa-sentada-na-polia", "Remada baixa sentada na polia", "CABLE", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5)),
        Def("DS_1345", "1345", "dataset-1345-remada-bilateral-com-kettlebells", "Remada bilateral com kettlebells", "KETTLEBELL", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5)),
        Def("DS_1359", "1359", "dataset-1359-remada-curvada-no-smith", "Remada curvada no Smith", "SMITH", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5, "REAR_DELTS" to 0.25)),
        Def("DS_1360", "1360", "dataset-1360-remada-unilateral-no-smith", "Remada unilateral no Smith", "SMITH", "HORIZONTAL_PULL", "UPPER_BACK", listOf("LATS" to 0.5, "BICEPS" to 0.5)),
        Def("DS_1013", "1013", "dataset-1013-puxada-supinada-com-faixa", "Puxada supinada com faixa", "BAND", "VERTICAL_PULL", "LATS", listOf("BICEPS" to 0.5)),
        Def("DS_0041", "0041", "dataset-0041-elevacao-frontal-com-barra", "Elevação frontal com barra", "BARBELL", "SHOULDER_FLEXION", "FRONT_DELTS", emptyList()),
        Def("DS_0076", "0076", "dataset-0076-remada-para-deltoide-posterior-com-barra", "Remada para deltoide posterior com barra", "BARBELL", "SHOULDER_HORIZONTAL_ABDUCTION", "REAR_DELTS", listOf("UPPER_BACK" to 0.5)),
        Def("DS_0120", "0120", "dataset-0120-remada-alta-com-barra", "Remada alta com barra", "BARBELL", "SHOULDER_ABDUCTION", "SIDE_DELTS", listOf("TRAPS" to 0.5)),
        Def("DS_0203", "0203", "dataset-0203-remada-para-deltoide-posterior-na-polia", "Remada para deltoide posterior na polia", "CABLE", "SHOULDER_HORIZONTAL_ABDUCTION", "REAR_DELTS", listOf("UPPER_BACK" to 0.5)),
        Def("DS_0219", "0219", "dataset-0219-desenvolvimento-de-ombros-na-polia", "Desenvolvimento de ombros na polia", "CABLE", "VERTICAL_PUSH", "FRONT_DELTS", listOf("SIDE_DELTS" to 0.5, "TRICEPS" to 0.5)),
        Def("DS_0437", "0437", "dataset-0437-remada-alta-com-halteres", "Remada alta com halteres", "DUMBBELLS", "SHOULDER_ABDUCTION", "SIDE_DELTS", listOf("TRAPS" to 0.5)),
        Def("DS_0997", "0997", "dataset-0997-desenvolvimento-com-faixa", "Desenvolvimento com faixa", "BAND", "VERTICAL_PUSH", "FRONT_DELTS", listOf("SIDE_DELTS" to 0.5, "TRICEPS" to 0.5)),
        Def("DS_0993", "0993", "dataset-0993-crucifixo-inverso-com-faixa", "Crucifixo inverso com faixa", "BAND", "SHOULDER_HORIZONTAL_ABDUCTION", "REAR_DELTS", listOf("UPPER_BACK" to 0.5)),
        Def("DS_0766", "0766", "dataset-0766-desenvolvimento-de-ombros-no-smith", "Desenvolvimento de ombros no Smith", "SMITH", "VERTICAL_PUSH", "FRONT_DELTS", listOf("SIDE_DELTS" to 0.5, "TRICEPS" to 0.5)),
        Def("DS_0762", "0762", "dataset-0762-remada-para-deltoide-posterior-no-smith", "Remada para deltoide posterior no Smith", "SMITH", "SHOULDER_HORIZONTAL_ABDUCTION", "REAR_DELTS", listOf("UPPER_BACK" to 0.5)),
        Def("DS_0038", "0038", "dataset-0038-rosca-drag-com-barra", "Rosca drag com barra", "BARBELL", "ELBOW_FLEXION", "BICEPS", listOf("FOREARMS" to 0.5)),
        Def("DS_0060", "0060", "dataset-0060-triceps-testa-com-barra-reta", "Tríceps testa com barra reta", "BARBELL", "ELBOW_EXTENSION", "TRICEPS", emptyList()),
        Def("DS_0165", "0165", "dataset-0165-rosca-martelo-com-corda-na-polia", "Rosca martelo com corda na polia", "CABLE", "ELBOW_FLEXION", "BICEPS", listOf("FOREARMS" to 0.5)),
        Def("DS_0190", "0190", "dataset-0190-rosca-unilateral-na-polia", "Rosca unilateral na polia", "CABLE", "ELBOW_FLEXION", "BICEPS", emptyList()),
        Def("DS_0206", "0206", "dataset-0206-rosca-inversa-na-polia", "Rosca inversa na polia", "CABLE", "ELBOW_FLEXION", "BICEPS", listOf("FOREARMS" to 0.5)),
        Def("DS_1723", "1723", "dataset-1723-triceps-unilateral-na-polia", "Tríceps unilateral na polia", "CABLE", "ELBOW_EXTENSION", "TRICEPS", emptyList()),
        Def("DS_0439", "0439", "dataset-0439-rosca-zottman-com-halteres", "Rosca Zottman com halteres", "DUMBBELLS", "ELBOW_FLEXION", "BICEPS", listOf("FOREARMS" to 0.5)),
        Def("DS_0454", "0454", "dataset-0454-rosca-spider-com-barra-w", "Rosca spider com barra W", "EZ_BAR", "ELBOW_FLEXION", "BICEPS", emptyList()),
        Def("DS_0351", "0351", "dataset-0351-triceps-testa-com-halteres", "Tríceps testa com halteres", "DUMBBELLS", "ELBOW_EXTENSION", "TRICEPS", emptyList()),
        Def("DS_0979", "0979", "dataset-0979-pallof-press-com-faixa-elastica", "Pallof press com faixa elástica", "BAND", "ANTI_ROTATION", "CORE", emptyList()),
        Def("DS_1015", "1015", "dataset-1015-pallof-press-vertical-com-faixa", "Pallof press vertical com faixa", "BAND", "ANTI_ROTATION", "CORE", emptyList()),
        Def("DS_0873", "0873", "dataset-0873-abdominal-reverso-na-polia", "Abdominal reverso na polia", "CABLE", "TRUNK_FLEXION", "CORE", emptyList()),
        Def("DS_1761", "1761", "dataset-1761-elevacao-obliqua-de-joelhos-na-barra", "Elevação oblíqua de joelhos na barra", "PULLUP_BAR", "HIP_FLEXION_CORE", "CORE", listOf("HIP_FLEXORS" to 0.25)),
        Def("DS_0620", "0620", "dataset-0620-elevacao-de-pernas-deitada-no-banco", "Elevação de pernas deitada no banco", "BODYWEIGHT", "HIP_FLEXION_CORE", "CORE", listOf("HIP_FLEXORS" to 0.25)),
        Def("DS_0687", "0687", "dataset-0687-russian-twist", "Russian twist", "BODYWEIGHT", "TRUNK_ROTATION", "CORE", emptyList()),
        Def("DS_0006", "0006", "dataset-0006-toque-alternado-nos-calcanhares", "Toque alternado nos calcanhares", "BODYWEIGHT", "LATERAL_FLEXION", "CORE", emptyList()),
        Def("DS_0222", "0222", "dataset-0222-inclinacao-lateral-na-polia", "Inclinação lateral na polia", "CABLE", "LATERAL_FLEXION", "CORE", emptyList()),
        Def("DS_0464", "0464", "dataset-0464-prancha-frontal-com-rotacao", "Prancha frontal com rotação", "BODYWEIGHT", "CORE_DYNAMIC", "CORE", emptyList()),
        Def("DS_0406", "0406", "dataset-0406-encolhimento-com-halteres", "Encolhimento com halteres", "DUMBBELLS", "SCAPULAR_ELEVATION", "TRAPS", emptyList()),
        Def("DS_0095", "0095", "dataset-0095-encolhimento-com-barra", "Encolhimento com barra", "BARBELL", "SCAPULAR_ELEVATION", "TRAPS", emptyList()),
        Def("DS_0220", "0220", "dataset-0220-encolhimento-na-polia", "Encolhimento na polia", "CABLE", "SCAPULAR_ELEVATION", "TRAPS", emptyList()),
        Def("DS_0767", "0767", "dataset-0767-encolhimento-no-smith", "Encolhimento no Smith", "SMITH", "SCAPULAR_ELEVATION", "TRAPS", emptyList()),
        Def("DS_0082", "0082", "dataset-0082-extensao-de-punho-com-barra", "Extensão de punho com barra", "BARBELL", "WRIST_EXTENSION", "FOREARMS", emptyList()),
        Def("DS_0210", "0210", "dataset-0210-extensao-de-punho-na-polia", "Extensão de punho na polia", "CABLE", "WRIST_EXTENSION", "FOREARMS", emptyList())
    )

    val sourceIds: Set<String> = defs.mapTo(linkedSetOf()) { it.sourceId }

    val exercises: List<ExerciseEntity> = defs.map { d ->
        ExerciseEntity(
            code = d.code,
            slug = d.slug,
            name = d.name,
            equipmentCode = d.equipment,
            movementPattern = d.pattern,
            instructions = instructionFor(d.pattern),
            commonErrors = errorsFor(d.pattern),
            estimatedSetSeconds = 45
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

    private fun instructionFor(pattern: String): String = when (pattern) {
        "SQUAT" ->
            "Mantenha os pés firmes e os joelhos acompanhando a direção dos pés. Desça com controle e retorne sem perder a posição do tronco."
        "LUNGE" ->
            "Estabeleça uma base estável, controle quadril e joelho durante a descida e retorne sem usar impulso excessivo."
        "KNEE_FLEXION" ->
            "Mantenha quadril e tronco estáveis, flexione o joelho de forma controlada e controle totalmente a volta."
        "HIP_HINGE" ->
            "Leve o quadril para trás com a coluna estável e a carga próxima ao corpo. Retorne estendendo o quadril sem hiperestender a lombar."
        "HIP_EXTENSION" ->
            "Estenda o quadril contraindo os glúteos e retorne lentamente, mantendo pelve e tronco estáveis."
        "HIP_ABDUCTION" ->
            "Afaste a perna ou os joelhos contra a resistência mantendo a pelve estável e controle o retorno."
        "HIP_ADDUCTION" ->
            "Aproxime a perna da linha média com controle, mantendo pelve e tronco estáveis e sem forçar a amplitude."
        "PLANTAR_FLEXION" ->
            "Eleve os calcanhares com controle, faça breve contração no topo e desça sem quicar."
        "HORIZONTAL_PUSH", "INCLINE_PUSH" ->
            "Mantenha o tronco e as escápulas estáveis, desça com controle até uma amplitude confortável e empurre sem perder a posição dos ombros."
        "VERTICAL_PULL" ->
            "Mantenha o tronco estável e puxe levando os cotovelos para baixo. Controle o retorno sem balanço."
        "HORIZONTAL_PULL" ->
            "Puxe a carga em direção ao tronco mantendo a coluna estável e as escápulas sob controle. Retorne sem soltar a carga."
        "VERTICAL_PUSH" ->
            "Empurre a carga acima da cabeça mantendo tronco, abdômen e glúteos estáveis e sem compensar com a lombar."
        "SHOULDER_FLEXION", "SHOULDER_ABDUCTION", "SHOULDER_HORIZONTAL_ABDUCTION" ->
            "Mova os braços com controle, sem impulso do tronco, até uma amplitude confortável para o ombro."
        "ELBOW_FLEXION" ->
            "Mantenha o braço e o tronco estáveis enquanto flexiona os cotovelos. Controle a descida e evite balanço."
        "ELBOW_EXTENSION" ->
            "Estenda os cotovelos mantendo o braço estável e retorne de forma controlada, sem compensar com o tronco."
        "ANTI_ROTATION" ->
            "Mantenha a pelve e o tronco firmes contra a tendência de rotação e execute o movimento respirando normalmente."
        "TRUNK_FLEXION", "HIP_FLEXION_CORE", "TRUNK_ROTATION", "LATERAL_FLEXION", "CORE_DYNAMIC" ->
            "Mantenha o tronco sob controle durante toda a repetição e evite usar impulso para completar a amplitude."
        "SCAPULAR_ELEVATION" ->
            "Eleve os ombros de forma controlada sem flexionar os cotovelos ou balançar o tronco. Desça lentamente."
        "WRIST_EXTENSION" ->
            "Com o antebraço estável, estenda o punho de forma controlada e retorne sem deixar a carga cair."
        else ->
            "Execute com amplitude confortável, controle durante toda a repetição e carga compatível com a técnica."
    }

    private fun errorsFor(pattern: String): String = when (pattern) {
        "SQUAT", "LUNGE" ->
            "Perder o apoio dos pés; joelhos colapsarem; usar amplitude ou carga que desorganiza a posição."
        "HIP_HINGE" ->
            "Arredondar a coluna; afastar a carga do corpo; usar impulso; buscar amplitude além do controle."
        "HORIZONTAL_PUSH", "INCLINE_PUSH", "VERTICAL_PUSH" ->
            "Perder estabilidade dos ombros; compensar com a lombar; usar impulso; insistir em amplitude dolorosa."
        "VERTICAL_PULL", "HORIZONTAL_PULL" ->
            "Balançar o tronco; perder controle na volta; elevar os ombros sem necessidade; usar carga excessiva."
        "ELBOW_FLEXION", "ELBOW_EXTENSION", "WRIST_EXTENSION" ->
            "Usar balanço; mover articulações que deveriam permanecer estáveis; deixar a carga cair na volta."
        "ANTI_ROTATION", "TRUNK_FLEXION", "HIP_FLEXION_CORE", "TRUNK_ROTATION", "LATERAL_FLEXION", "CORE_DYNAMIC" ->
            "Perder a posição do tronco; prender a respiração; usar impulso para completar a repetição."
        else ->
            "Usar impulso; perder estabilidade; reduzir o controle para mover mais carga; insistir em dor articular."
    }
}
