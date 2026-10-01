package br.com.taina.constantia.core.repository

/** Estrutura mínima do semestre; o texto integral continua vindo dos PDFs importados pelo usuário. */
object SemesterStudyCatalog {
    data class SubjectSeed(val name: String, val topics: List<String>)

    val subjects = listOf(
        SubjectSeed(
            "Produção Agrícola",
            listOf(
                "Fundamentos da produção agrícola",
                "Manejo do solo e da água",
                "Manejo agrícola: tratos culturais, fitossanitários e mecanização agrícola",
                "Cultivos agrícolas de importância no Brasil",
                "Colheita, beneficiamento, armazenamento e integração na produção agrícola"
            )
        ),
        SubjectSeed(
            "Produção Pecuária",
            listOf(
                "Fundamentos da Produção Pecuária",
                "Manejo Sustentável e Saúde Animal",
                "Gestão de Recursos Naturais",
                "Tecnologia e Inovação",
                "Sustentabilidade, Mercado e Regulação"
            )
        ),
        SubjectSeed(
            "Agroindústria Vegetal",
            listOf(
                "Princípios da agroindústria vegetal",
                "Beneficiamento e tecnologias na agroindústria vegetal",
                "Sustentabilidade e segurança na agroindústria vegetal"
            )
        ),
        SubjectSeed(
            "Agroindústria Animal",
            listOf(
                "Princípios e Normas da Agroindústria Animal",
                "Processamento e Beneficiamento de Produtos Animais",
                "Sustentabilidade e Gestão na Agroindústria Animal"
            )
        ),
        SubjectSeed(
            "Cadeia Produtiva Agropecuária",
            listOf(
                "Fundamentos da cadeia produtiva agropecuária",
                "Produção agropecuária integrada e sustentável",
                "Pós-produção e integração ao mercado"
            )
        )
    )
}
