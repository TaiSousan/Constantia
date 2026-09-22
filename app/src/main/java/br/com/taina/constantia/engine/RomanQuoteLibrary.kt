package br.com.taina.constantia.engine

/**
 * Citações clássicas curtas, armazenadas com referência. A tradução em português
 * é livre e feita para o Constantia; o original permanece disponível para evitar
 * atribuições inventadas.
 */
data class RomanQuote(
    val original: String,
    val portuguese: String,
    val author: String,
    val source: String
)

object RomanQuoteLibrary {
    private val quotes = listOf(
        RomanQuote(
            original = "Carpe diem, quam minimum credula postero.",
            portuguese = "Aproveite o dia; confie o mínimo possível no amanhã.",
            author = "Horácio",
            source = "Odes 1.11"
        ),
        RomanQuote(
            original = "Mens sana in corpore sano.",
            portuguese = "Uma mente saudável em um corpo saudável.",
            author = "Juvenal",
            source = "Sátiras 10.356"
        ),
        RomanQuote(
            original = "Homo sum, humani nihil a me alienum puto.",
            portuguese = "Sou humano; nada do que é humano considero alheio a mim.",
            author = "Terêncio",
            source = "Heauton Timorumenos 77"
        ),
        RomanQuote(
            original = "Plura sunt quae nos terrent quam quae premunt.",
            portuguese = "Mais coisas nos assustam do que realmente nos oprimem.",
            author = "Sêneca",
            source = "Cartas a Lucílio 13.4"
        )
    )

    fun forSeed(seed: Int): RomanQuote = quotes[Math.floorMod(seed, quotes.size)]
}
