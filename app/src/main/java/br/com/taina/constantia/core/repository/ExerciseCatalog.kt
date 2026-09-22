package br.com.taina.constantia.core.repository

import br.com.taina.constantia.core.database.EquipmentEntity
import br.com.taina.constantia.core.database.ExerciseEntity
import br.com.taina.constantia.core.database.ExerciseMuscleEntity
import br.com.taina.constantia.core.database.MuscleEntity

object ExerciseCatalog {
    val muscles = listOf(
        MuscleEntity("QUADS", "Quadríceps", "INFERIOR"),
        MuscleEntity("HAMSTRINGS", "Posteriores de coxa", "INFERIOR"),
        MuscleEntity("GLUTES", "Glúteos", "INFERIOR"),
        MuscleEntity("CALVES", "Panturrilhas", "INFERIOR"),
        MuscleEntity("CHEST", "Peitoral", "SUPERIOR"),
        MuscleEntity("LATS", "Latíssimo do dorso", "SUPERIOR"),
        MuscleEntity("UPPER_BACK", "Costas superiores", "SUPERIOR"),
        MuscleEntity("FRONT_DELTS", "Deltoide anterior", "SUPERIOR"),
        MuscleEntity("SIDE_DELTS", "Deltoide lateral", "SUPERIOR"),
        MuscleEntity("TRICEPS", "Tríceps", "SUPERIOR"),
        MuscleEntity("BICEPS", "Bíceps", "SUPERIOR"),
        MuscleEntity("CORE", "Core", "TRONCO")
    )

    val equipment = listOf(
        EquipmentEntity("LEG_PRESS", "Leg press", "MACHINE", true, 10.0),
        EquipmentEntity("LEG_EXTENSION", "Cadeira extensora", "MACHINE", true, 5.0),
        EquipmentEntity("LEG_CURL", "Cadeira/mesa flexora", "MACHINE", true, 5.0),
        EquipmentEntity("ABDUCTOR", "Cadeira abdutora", "MACHINE", true, 5.0),
        EquipmentEntity("ADDUCTOR", "Cadeira adutora", "MACHINE", true, 5.0),
        EquipmentEntity("SMITH", "Smith / máquina guiada", "MACHINE", true, 5.0),
        EquipmentEntity("CALF_MACHINE", "Máquina de panturrilha", "MACHINE", true, 5.0),
        EquipmentEntity("CHEST_PRESS", "Supino máquina", "MACHINE", true, 5.0),
        EquipmentEntity("PECK_DECK", "Peck deck / voador", "MACHINE", true, 5.0),
        EquipmentEntity("LAT_PULLDOWN", "Puxador / pulley frente", "MACHINE", true, 5.0),
        EquipmentEntity("SEATED_ROW", "Remada sentada", "MACHINE", true, 5.0),
        EquipmentEntity("SHOULDER_PRESS", "Desenvolvimento máquina", "MACHINE", true, 5.0),
        EquipmentEntity("CABLE", "Cross over / polia", "CABLE", true, 2.5),
        EquipmentEntity("SCOTT", "Banco Scott", "MACHINE", true, 2.5),
        EquipmentEntity("DUMBBELLS", "Halteres", "FREE_WEIGHT", true, 2.0),
        EquipmentEntity("BARBELL", "Barra e anilhas", "FREE_WEIGHT", true, 2.5),
        EquipmentEntity("BENCH", "Banco ajustável", "FREE_WEIGHT", true, null),
        EquipmentEntity("BODYWEIGHT", "Peso corporal", "BODYWEIGHT", true, null)
    )

    val exercises = listOf(
        ExerciseEntity(
            code = "LEG_PRESS_45", slug = "leg-press-45", name = "Leg press 45°", equipmentCode = "LEG_PRESS",
            movementPattern = "KNEE_HIP_EXTENSION",
            instructions = "Apoie toda a lombar no encosto, mantenha os pés firmes na plataforma e desça com controle até a amplitude confortável. Empurre sem travar os joelhos.",
            commonErrors = "Retirar o quadril do banco; deixar os joelhos colapsarem para dentro; reduzir demais a amplitude; travar os joelhos no topo."
        ),
        ExerciseEntity(
            code = "LEG_EXTENSION_MACHINE", slug = "cadeira-extensora", name = "Cadeira extensora", equipmentCode = "LEG_EXTENSION",
            movementPattern = "KNEE_EXTENSION",
            instructions = "Ajuste o eixo da máquina próximo ao joelho e o apoio sobre a canela. Estenda os joelhos de forma controlada e retorne sem deixar a carga despencar.",
            commonErrors = "Banco mal ajustado; impulso com o tronco; bater a pilha de pesos; usar amplitude dolorosa."
        ),
        ExerciseEntity(
            code = "SEATED_LEG_CURL", slug = "cadeira-flexora", name = "Cadeira flexora", equipmentCode = "LEG_CURL",
            movementPattern = "KNEE_FLEXION",
            instructions = "Mantenha quadril e tronco apoiados. Flexione os joelhos levando o rolo para baixo e para trás, depois retorne devagar.",
            commonErrors = "Levantar o quadril; encurtar muito a amplitude; soltar a carga na volta."
        ),
        ExerciseEntity(
            code = "LYING_LEG_CURL", slug = "mesa-flexora", name = "Mesa flexora", equipmentCode = "LEG_CURL",
            movementPattern = "KNEE_FLEXION",
            instructions = "Mantenha o quadril apoiado e flexione os joelhos até a amplitude confortável. Controle a extensão na volta.",
            commonErrors = "Elevar o quadril; exagerar na carga; acelerar a fase de retorno."
        ),
        ExerciseEntity(
            code = "HIP_ABDUCTOR", slug = "cadeira-abdutora", name = "Cadeira abdutora", equipmentCode = "ABDUCTOR",
            movementPattern = "HIP_ABDUCTION",
            instructions = "Mantenha o tronco estável e abra os joelhos contra a resistência. Retorne com controle.",
            commonErrors = "Balançar o tronco; bater a carga; usar amplitude desconfortável."
        ),
        ExerciseEntity(
            code = "HIP_ADDUCTOR", slug = "cadeira-adutora", name = "Cadeira adutora", equipmentCode = "ADDUCTOR",
            movementPattern = "HIP_ADDUCTION",
            instructions = "Comece em uma abertura confortável e aproxime as pernas sem impulso. Retorne de forma controlada.",
            commonErrors = "Forçar amplitude excessiva; usar impulso; soltar a carga."
        ),
        ExerciseEntity(
            code = "SMITH_SQUAT", slug = "agachamento-smith", name = "Agachamento no Smith", equipmentCode = "SMITH",
            movementPattern = "SQUAT",
            instructions = "Posicione a barra de forma confortável, mantenha pés estáveis e desça com joelhos acompanhando a direção dos pés. Suba mantendo o tronco controlado.",
            commonErrors = "Posição dos pés inadequada; joelhos colapsando; perder apoio dos pés; amplitude dolorosa."
        ),
        ExerciseEntity(
            code = "DB_RDL", slug = "stiff-halteres", name = "Stiff / levantamento romeno com halteres", equipmentCode = "DUMBBELLS",
            movementPattern = "HIP_HINGE",
            instructions = "Com joelhos levemente flexionados, leve o quadril para trás mantendo a coluna estável e os halteres próximos às pernas. Retorne contraindo glúteos.",
            commonErrors = "Arredondar a lombar; transformar o movimento em agachamento; afastar os halteres do corpo; buscar amplitude além do controle."
        ),
        ExerciseEntity(
            code = "CALF_MACHINE", slug = "panturrilha-maquina", name = "Panturrilha na máquina", equipmentCode = "CALF_MACHINE",
            movementPattern = "PLANTAR_FLEXION",
            instructions = "Eleve os calcanhares até uma contração confortável e desça de forma controlada, usando amplitude sem perder estabilidade.",
            commonErrors = "Quicar; fazer repetições muito curtas; usar impulso do corpo."
        ),
        ExerciseEntity(
            code = "CHEST_PRESS_MACHINE", slug = "supino-maquina", name = "Supino máquina", equipmentCode = "CHEST_PRESS",
            movementPattern = "HORIZONTAL_PUSH",
            instructions = "Ajuste o banco para as pegadas ficarem aproximadamente na linha do peito. Empurre mantendo escápulas apoiadas e retorne com controle.",
            commonErrors = "Ombros projetados para frente; banco mal ajustado; travar cotovelos; perder controle na volta."
        ),
        ExerciseEntity(
            code = "INCLINE_DB_PRESS", slug = "supino-inclinado-halteres", name = "Supino inclinado com halteres", equipmentCode = "DUMBBELLS",
            movementPattern = "INCLINE_PUSH",
            instructions = "No banco inclinado, mantenha pés firmes e escápulas estáveis. Desça os halteres com controle e empurre sem bater um no outro.",
            commonErrors = "Inclinação excessiva do banco; ombros soltos; amplitude desconfortável; arqueamento exagerado."
        ),
        ExerciseEntity(
            code = "PECK_DECK", slug = "peck-deck", name = "Peck deck / voador", equipmentCode = "PECK_DECK",
            movementPattern = "HORIZONTAL_ADDUCTION",
            instructions = "Ajuste o banco para os braços ficarem confortáveis. Aproxime as alças sem tirar as costas do apoio e retorne lentamente.",
            commonErrors = "Alongar além do conforto do ombro; usar impulso; perder contato com o encosto."
        ),
        ExerciseEntity(
            code = "LAT_PULLDOWN", slug = "puxada-frontal", name = "Puxada frontal", equipmentCode = "LAT_PULLDOWN",
            movementPattern = "VERTICAL_PULL",
            instructions = "Segure a barra com pegada confortável, mantenha o tronco estável e puxe em direção à parte alta do peito. Controle a subida.",
            commonErrors = "Puxar atrás da nuca; balançar o tronco; usar impulso; elevar exageradamente os ombros."
        ),
        ExerciseEntity(
            code = "SEATED_ROW", slug = "remada-sentada", name = "Remada sentada", equipmentCode = "SEATED_ROW",
            movementPattern = "HORIZONTAL_PULL",
            instructions = "Mantenha o tronco estável e puxe a alça em direção ao abdômen, aproximando as escápulas sem exagero. Retorne controlando.",
            commonErrors = "Balançar o tronco; arredondar a lombar; puxar apenas com os braços; soltar a carga na volta."
        ),
        ExerciseEntity(
            code = "SHOULDER_PRESS_MACHINE", slug = "desenvolvimento-maquina", name = "Desenvolvimento máquina", equipmentCode = "SHOULDER_PRESS",
            movementPattern = "VERTICAL_PUSH",
            instructions = "Ajuste o banco para as pegadas começarem em posição confortável. Empurre acima da cabeça sem perder contato do tronco com o encosto.",
            commonErrors = "Banco baixo demais; hiperextensão lombar; amplitude dolorosa; travar cotovelos com força."
        ),
        ExerciseEntity(
            code = "DB_LATERAL_RAISE", slug = "elevacao-lateral", name = "Elevação lateral com halteres", equipmentCode = "DUMBBELLS",
            movementPattern = "SHOULDER_ABDUCTION",
            instructions = "Com cotovelos levemente flexionados, eleve os braços lateralmente até a amplitude confortável. Desça devagar.",
            commonErrors = "Impulso do tronco; elevar muito acima do necessário; carga excessiva; encolher os ombros."
        ),
        ExerciseEntity(
            code = "CABLE_TRICEPS_PRESSDOWN", slug = "triceps-pulley", name = "Tríceps no pulley", equipmentCode = "CABLE",
            movementPattern = "ELBOW_EXTENSION",
            instructions = "Mantenha os cotovelos próximos ao tronco e estenda os braços contra a polia. Retorne sem deixar os cotovelos avançarem demais.",
            commonErrors = "Balançar o corpo; abrir os cotovelos; usar carga que obriga a inclinar excessivamente o tronco."
        ),
        ExerciseEntity(
            code = "SCOTT_CURL", slug = "rosca-scott", name = "Rosca Scott", equipmentCode = "SCOTT",
            movementPattern = "ELBOW_FLEXION",
            instructions = "Apoie completamente os braços no banco e flexione os cotovelos sem retirar o braço do apoio. Desça de forma controlada.",
            commonErrors = "Tirar os cotovelos do apoio; usar impulso; relaxar completamente no fim da descida."
        ),
        ExerciseEntity(
            code = "DB_BICEPS_CURL", slug = "rosca-halteres", name = "Rosca com halteres", equipmentCode = "DUMBBELLS",
            movementPattern = "ELBOW_FLEXION",
            instructions = "Mantenha o tronco estável e flexione os cotovelos sem projetá-los para frente. Retorne controlando a carga.",
            commonErrors = "Balançar o tronco; elevar os ombros; avançar os cotovelos; usar impulso."
        ),
        ExerciseEntity(
            code = "CABLE_CRUNCH", slug = "abdominal-polia", name = "Abdominal na polia", equipmentCode = "CABLE",
            movementPattern = "TRUNK_FLEXION",
            instructions = "Mantenha quadril relativamente estável e flexione o tronco aproximando costelas e pelve. Retorne de forma controlada.",
            commonErrors = "Puxar apenas com os braços; transformar o movimento em flexão de quadril; usar impulso."
        )
    )

    val exerciseMuscles = listOf(
        m("LEG_PRESS_45", "QUADS", 1.0, "PRIMARY"), m("LEG_PRESS_45", "GLUTES", 0.5, "SECONDARY"),
        m("LEG_EXTENSION_MACHINE", "QUADS", 1.0, "PRIMARY"),
        m("SEATED_LEG_CURL", "HAMSTRINGS", 1.0, "PRIMARY"),
        m("LYING_LEG_CURL", "HAMSTRINGS", 1.0, "PRIMARY"),
        m("HIP_ABDUCTOR", "GLUTES", 1.0, "PRIMARY"),
        m("HIP_ADDUCTOR", "GLUTES", 0.5, "SECONDARY"),
        m("SMITH_SQUAT", "QUADS", 1.0, "PRIMARY"), m("SMITH_SQUAT", "GLUTES", 0.75, "SECONDARY"),
        m("DB_RDL", "HAMSTRINGS", 1.0, "PRIMARY"), m("DB_RDL", "GLUTES", 0.75, "SECONDARY"),
        m("CALF_MACHINE", "CALVES", 1.0, "PRIMARY"),
        m("CHEST_PRESS_MACHINE", "CHEST", 1.0, "PRIMARY"), m("CHEST_PRESS_MACHINE", "TRICEPS", 0.5, "SECONDARY"), m("CHEST_PRESS_MACHINE", "FRONT_DELTS", 0.5, "SECONDARY"),
        m("INCLINE_DB_PRESS", "CHEST", 1.0, "PRIMARY"), m("INCLINE_DB_PRESS", "TRICEPS", 0.5, "SECONDARY"), m("INCLINE_DB_PRESS", "FRONT_DELTS", 0.5, "SECONDARY"),
        m("PECK_DECK", "CHEST", 1.0, "PRIMARY"),
        m("LAT_PULLDOWN", "LATS", 1.0, "PRIMARY"), m("LAT_PULLDOWN", "BICEPS", 0.5, "SECONDARY"),
        m("SEATED_ROW", "UPPER_BACK", 1.0, "PRIMARY"), m("SEATED_ROW", "LATS", 0.5, "SECONDARY"), m("SEATED_ROW", "BICEPS", 0.5, "SECONDARY"),
        m("SHOULDER_PRESS_MACHINE", "FRONT_DELTS", 1.0, "PRIMARY"), m("SHOULDER_PRESS_MACHINE", "TRICEPS", 0.5, "SECONDARY"),
        m("DB_LATERAL_RAISE", "SIDE_DELTS", 1.0, "PRIMARY"),
        m("CABLE_TRICEPS_PRESSDOWN", "TRICEPS", 1.0, "PRIMARY"),
        m("SCOTT_CURL", "BICEPS", 1.0, "PRIMARY"),
        m("DB_BICEPS_CURL", "BICEPS", 1.0, "PRIMARY"),
        m("CABLE_CRUNCH", "CORE", 1.0, "PRIMARY")
    )

    private fun m(exercise: String, muscle: String, contribution: Double, role: String) =
        ExerciseMuscleEntity(exercise, muscle, contribution, role)
}
