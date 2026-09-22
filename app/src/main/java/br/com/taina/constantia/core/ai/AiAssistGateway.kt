package br.com.taina.constantia.core.ai

/**
 * Contrato para uma camada remota opcional futura.
 * O MVP não inclui chave de API dentro do APK. Um provedor remoto deverá ser
 * chamado por um backend/proxy controlado, nunca por segredo embutido no cliente.
 */
interface AiAssistGateway {
    suspend fun interpretMeal(text: String): RemoteMealDraft?
    suspend fun generateStudyQuestions(material: String, maxQuestions: Int): List<RemoteQuestionDraft>
    suspend fun explainDecision(decisionType: String, facts: Map<String, String>): String?
}

data class RemoteMealDraft(
    val summary: String,
    val confidenceNote: String
)

data class RemoteQuestionDraft(
    val prompt: String,
    val answer: String,
    val sourceExcerpt: String
)

object DisabledAiAssistGateway : AiAssistGateway {
    override suspend fun interpretMeal(text: String): RemoteMealDraft? = null
    override suspend fun generateStudyQuestions(material: String, maxQuestions: Int): List<RemoteQuestionDraft> = emptyList()
    override suspend fun explainDecision(decisionType: String, facts: Map<String, String>): String? = null
}
