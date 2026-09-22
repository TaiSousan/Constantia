package br.com.taina.constantia.engine

/** Converte decisões já calculadas em texto claro; não toma decisões novas. */
object SuggestionExplainer {
    fun training(action: String, rationale: String): String = when (action) {
        "INCREASE_LOAD" -> "Aumentar a carga foi sugerido porque $rationale"
        "REDUCE_LOAD" -> "Reduzir a carga foi sugerido porque $rationale"
        "MAINTAIN" -> "Manter a carga faz sentido porque $rationale"
        else -> rationale
    }

    fun adherence(area: String, completed: Int, planned: Int): String {
        if (planned <= 0) return "Ainda não há volume suficiente de dados em $area para sugerir uma mudança."
        val pct = completed.toDouble() / planned
        return when {
            pct >= 0.85 -> "Sua aderência em $area está consistente ($completed de $planned). Não há motivo para aumentar a complexidade agora."
            pct >= 0.60 -> "Você realizou $completed de $planned em $area. Antes de aumentar a meta, vale manter a estrutura e observar mais uma semana."
            else -> "Você realizou $completed de $planned em $area. A prioridade é revisar horário, duração ou encaixe antes de aumentar a exigência."
        }
    }
}
