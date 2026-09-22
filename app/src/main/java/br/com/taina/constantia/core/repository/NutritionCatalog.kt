package br.com.taina.constantia.core.repository

import br.com.taina.constantia.core.database.FoodEntity

/**
 * Catálogo inicial offline. Valores por 100 g da parte comestível.
 * Fontes TBCA quando há código disponível; alimentos de rótulo variável ficam
 * explicitamente marcados como estimativa genérica.
 */
object NutritionCatalog {
    val starterFoods = listOf(
        food("Arroz branco cozido, sem óleo", 131.0, 2.38, 30.0, 0.41, "colher de sopa cheia", 20.0, "BRC0018A", "TBCA"),
        food("Peito de frango grelhado, sem pele e sem óleo", 149.0, 31.8, 0.0, 2.46, "filé médio", 110.0, "BRC0230F", "TBCA"),
        food("Ovo de galinha cozido", 125.0, 10.4, 1.38, 8.70, "unidade média", 50.0, "BRC0010J", "TBCA"),
        food("Banana prata", 107.0, 1.11, 25.9, 0.28, "unidade/fatia média", 40.0, "BRC0011C", "TBCA"),
        food("Queijo Minas frescal", 243.0, 15.9, 3.02, 18.6, "fatia média", 30.0, "BRC0052G", "TBCA"),
        food("Queijo prato", 346.0, 24.2, 0.20, 27.6, "fatia média", 20.0, "BRC0064G", "TBCA"),
        food("Pão francês de padaria", 300.0, 9.83, 61.6, 2.12, "unidade média", 50.0, "BRC0002A", "TBCA"),
        food("Açúcar cristal", 400.0, 0.32, 99.6, 0.0, "colher de chá cheia", 5.0, "BRC0005K", "TBCA"),
        food("Café preto, infusão 10%, sem açúcar", 10.0, 0.67, 1.68, 0.07, "xícara de chá", 200.0, "BRC0007H", "TBCA"),
        food("Tapioca sem manteiga e sem recheio", 289.0, 0.36, 71.9, 0.0, "colher de sopa de goma", 15.0, "BRC0906B", "TBCA · aproximação para goma hidratada"),
        food("Leite de vaca desnatado", 34.0, 3.4, 5.0, 0.1, "copo", 200.0, "BRC0070G", "TBCA"),
        food("Iogurte natural desnatado", 41.0, 4.1, 5.8, 0.4, "pote", 170.0, "REF-Iogurte", "Referência genérica; conferir rótulo"),
        food("Aveia em flocos", 394.0, 13.9, 66.6, 8.5, "colher de sopa", 15.0, "REF-Aveia", "Referência genérica"),
        food("Whey protein em pó", 400.0, 80.0, 8.0, 6.0, "medidor", 30.0, "REF-Whey", "Genérico; substituir pelo rótulo da marca"),
        food("Alface crua", 14.0, 1.3, 2.4, 0.2, "folha média", 10.0, "REF-Alface", "Referência genérica")
    )

    private fun food(
        name: String, kcal: Double, protein: Double, carbs: Double, fat: Double,
        measureName: String, measureGrams: Double, sourceCode: String, sourceLabel: String
    ) = FoodEntity(
        name = name,
        kcalPer100g = kcal,
        proteinPer100g = protein,
        carbsPer100g = carbs,
        fatPer100g = fat,
        defaultMeasureName = measureName,
        defaultMeasureGrams = measureGrams,
        sourceCode = sourceCode,
        sourceLabel = sourceLabel
    )
}
