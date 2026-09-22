package br.com.taina.constantia.engine

/**
 * Técnicas avançadas são sugestões opcionais, nunca alterações automáticas da ficha.
 * O motor evita colocar métodos de alta fadiga em levantamentos livres prioritários.
 */
enum class TrainingTechniqueType {
    BI_SET,
    TRI_SET,
    ISOMETRY,
    DROP_SET,
    REST_PAUSE,
    TEMPO_CONTROLLED
}

data class TechniqueExerciseInput(
    val code: String,
    val name: String,
    val movementPattern: String,
    val equipmentCode: String,
    val priorityScore: Int,
    val timed: Boolean = false
)

data class TrainingTechniqueSuggestion(
    val type: TrainingTechniqueType,
    val title: String,
    val exerciseCodes: List<String>,
    val instruction: String,
    val rationale: String
)

class TrainingTechniqueEngine {
    private val isolationPatterns = setOf(
        "KNEE_EXTENSION", "KNEE_FLEXION", "SHOULDER_ABDUCTION",
        "SHOULDER_HORIZONTAL_ABDUCTION", "ELBOW_FLEXION", "ELBOW_EXTENSION",
        "HORIZONTAL_ADDUCTION", "HIP_ABDUCTION", "HIP_ADDUCTION",
        "PLANTAR_FLEXION", "WRIST_FLEXION", "WRIST_EXTENSION"
    )
    private val machineLike = setOf(
        "CABLE", "LEG_EXTENSION", "LEG_CURL", "ABDUCTOR", "ADDUCTOR",
        "CALF_MACHINE", "SEATED_CALF", "PECK_DECK", "REVERSE_PEC_DECK",
        "SCOTT", "LATERAL_RAISE_MACHINE", "CHEST_SUPPORTED_ROW"
    )

    fun suggest(
        exercises: List<TechniqueExerciseInput>,
        experienceLevel: String
    ): List<TrainingTechniqueSuggestion> {
        if (exercises.isEmpty()) return emptyList()
        val result = mutableListOf<TrainingTechniqueSuggestion>()
        val advanced = experienceLevel.uppercase() in setOf("INTERMEDIATE", "ADVANCED")
        val accessories = exercises.filter { it.priorityScore < 90 }

        exercises.firstOrNull { it.timed }?.let { timed ->
            result += TrainingTechniqueSuggestion(
                type = TrainingTechniqueType.ISOMETRY,
                title = "Isometria",
                exerciseCodes = listOf(timed.code),
                instruction = "${timed.name}: sustente a posição pelo tempo prescrito, respirando normalmente e encerrando antes de perder a técnica.",
                rationale = "A isometria entra como exercício próprio e é registrada em segundos."
            )
        }

        findAntagonistPair(accessories)?.let { (a, b) ->
            result += TrainingTechniqueSuggestion(
                type = TrainingTechniqueType.BI_SET,
                title = "Bi-set / superset antagonista",
                exerciseCodes = listOf(a.code, b.code),
                instruction = "${a.name} → ${b.name}, com pouca ou nenhuma pausa entre os exercícios; descanse 60–120 s após completar o par.",
                rationale = "Pode economizar tempo sem exigir que os exercícios compostos principais sejam acelerados."
            )
        } ?: accessories.windowed(2, 1).firstOrNull { pair ->
            pair.map { it.movementPattern }.distinct().size == 2 &&
                pair.none { it.priorityScore >= 90 }
        }?.let { pair ->
            result += TrainingTechniqueSuggestion(
                type = TrainingTechniqueType.BI_SET,
                title = "Bi-set de acessórios",
                exerciseCodes = pair.map { it.code },
                instruction = "${pair[0].name} → ${pair[1].name}; complete ambos antes do descanso.",
                rationale = "Agrupa acessórios para reduzir tempo de sessão mantendo a ficha original."
            )
        }

        if (advanced) {
            val tri = accessories
                .filter { it.movementPattern in isolationPatterns }
                .take(3)
            if (tri.size == 3) {
                result += TrainingTechniqueSuggestion(
                    type = TrainingTechniqueType.TRI_SET,
                    title = "Tri-set opcional",
                    exerciseCodes = tri.map { it.code },
                    instruction = tri.joinToString(" → ") { it.name } + ". Faça uma rodada completa e então descanse 90–150 s.",
                    rationale = "Útil quando o objetivo é condensar acessórios; aumenta a fadiga percebida, por isso não é padrão obrigatório."
                )
            }

            val drop = accessories.firstOrNull {
                it.movementPattern in isolationPatterns &&
                    (it.equipmentCode in machineLike || it.equipmentCode == "DUMBBELLS")
            }
            drop?.let {
                result += TrainingTechniqueSuggestion(
                    type = TrainingTechniqueType.DROP_SET,
                    title = "Drop-set opcional",
                    exerciseCodes = listOf(it.code),
                    instruction = "${it.name}: somente na última série, reduza aproximadamente 20–30% da carga e continue com repetições controladas. Uma queda de carga é suficiente.",
                    rationale = "É uma opção de economia de tempo; não é necessária para progredir e não deve substituir a progressão normal."
                )
            }

            val rp = accessories.firstOrNull {
                it.movementPattern in isolationPatterns && it.equipmentCode in machineLike && it.code != drop?.code
            }
            rp?.let {
                result += TrainingTechniqueSuggestion(
                    type = TrainingTechniqueType.REST_PAUSE,
                    title = "Rest-pause opcional",
                    exerciseCodes = listOf(it.code),
                    instruction = "${it.name}: após a última série, descanse 10–20 s e faça um pequeno bloco adicional de 3–5 repetições com técnica preservada.",
                    rationale = "Método de intensificação para praticantes experientes; use no máximo em um exercício da sessão."
                )
            }
        }

        exercises.firstOrNull {
            it.priorityScore < 90 && !it.timed &&
                it.movementPattern !in setOf("HIP_HINGE", "SQUAT", "LUNGE")
        }?.let {
            result += TrainingTechniqueSuggestion(
                type = TrainingTechniqueType.TEMPO_CONTROLLED,
                title = "Tempo controlado",
                exerciseCodes = listOf(it.code),
                instruction = "${it.name}: experimente 3 s na fase de retorno/alongamento e execução controlada na fase de esforço, sem alterar a carga apenas para cumprir o tempo.",
                rationale = "Varia a execução sem exigir técnicas de falha ou mudanças estruturais no treino."
            )
        }

        return result.distinctBy { it.type }.take(5)
    }

    private fun findAntagonistPair(items: List<TechniqueExerciseInput>): Pair<TechniqueExerciseInput, TechniqueExerciseInput>? {
        fun first(pattern: String) = items.firstOrNull { it.movementPattern == pattern }
        val arm = first("ELBOW_EXTENSION")?.let { ext -> first("ELBOW_FLEXION")?.let { flex -> ext to flex } }
        if (arm != null) return arm
        val leg = first("KNEE_EXTENSION")?.let { ext -> first("KNEE_FLEXION")?.let { flex -> ext to flex } }
        if (leg != null) return leg
        val shoulder = first("SHOULDER_ABDUCTION")?.let { lat ->
            first("SHOULDER_HORIZONTAL_ABDUCTION")?.let { rear -> lat to rear }
        }
        return shoulder
    }
}
