package br.com.taina.constantia.engine

/**
 * Feedback curto e informacional após conclusão. As frases são autorais,
 * sem atribuições fictícias e sem recompensas tangíveis.
 */
object CompletionFeedbackLibrary {
    private val workout = listOf(
        "Treino concluído. Você cumpriu o combinado de hoje.",
        "Sessão registrada. Consistência acumulada vale mais do que um treino perfeito.",
        "Treino feito. Agora recuperação também faz parte do plano.",
        "Você terminou a sessão e deixou um registro útil para a próxima progressão."
    )
    private val study = listOf(
        "Sessão de estudo concluída. Mais um bloco consistente.",
        "Estudo registrado. O progresso fica mais claro quando você volta e repete.",
        "Bloco concluído. Agora o Constantia tem mais um dado real para organizar suas revisões.",
        "Você fechou a sessão de hoje. Pequenos blocos somados constroem continuidade."
    )
    private val activity = listOf(
        "Concluído. Mais uma tarefa registrada.",
        "Feito. Seu progresso de hoje já mudou.",
        "Atividade concluída. Continue no próximo passo, não na perfeição.",
        "Registro salvo. Consistência também é marcar o que realmente aconteceu."
    )

    fun workout(seed: Long = System.currentTimeMillis()): String = pick(workout, seed)
    fun study(seed: Long = System.currentTimeMillis()): String = pick(study, seed)
    fun activity(seed: Long = System.currentTimeMillis()): String = pick(activity, seed)

    private fun pick(values: List<String>, seed: Long): String =
        values[((seed xor (seed ushr 32)).toInt() and Int.MAX_VALUE) % values.size]
}
