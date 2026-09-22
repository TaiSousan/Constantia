package br.com.taina.constantia.core.repository

import br.com.taina.constantia.core.database.FoodEntity

/**
 * Catálogo inicial offline com referências TBCA consultadas em setembro/2026.
 * Os valores são por 100 g da parte comestível. O usuário pode criar alimentos
 * personalizados quando rótulo/marca/preparo forem diferentes.
 */
object NutritionCatalog {
    val starterFoods = listOf(
        FoodEntity(
            name = "Arroz branco cozido, sem óleo",
            kcalPer100g = 131.0,
            proteinPer100g = 2.38,
            carbsPer100g = 30.0,
            fatPer100g = 0.41,
            defaultMeasureName = "colher de sopa cheia",
            defaultMeasureGrams = 20.0,
            sourceCode = "BRC0018A",
            sourceLabel = "TBCA"
        ),
        FoodEntity(
            name = "Peito de frango grelhado, sem pele e sem óleo",
            kcalPer100g = 149.0,
            proteinPer100g = 31.8,
            carbsPer100g = 0.0,
            fatPer100g = 2.46,
            defaultMeasureName = "filé médio",
            defaultMeasureGrams = 110.0,
            sourceCode = "BRC0230F",
            sourceLabel = "TBCA"
        ),
        FoodEntity(
            name = "Ovo de galinha cozido",
            kcalPer100g = 125.0,
            proteinPer100g = 10.4,
            carbsPer100g = 1.38,
            fatPer100g = 8.70,
            defaultMeasureName = "unidade média",
            defaultMeasureGrams = 50.0,
            sourceCode = "BRC0010J",
            sourceLabel = "TBCA"
        ),
        FoodEntity(
            name = "Banana prata",
            kcalPer100g = 107.0,
            proteinPer100g = 1.11,
            carbsPer100g = 25.9,
            fatPer100g = 0.28,
            defaultMeasureName = "unidade/fatia média",
            defaultMeasureGrams = 40.0,
            sourceCode = "BRC0011C",
            sourceLabel = "TBCA"
        ),
        FoodEntity(
            name = "Queijo Minas frescal",
            kcalPer100g = 243.0,
            proteinPer100g = 15.9,
            carbsPer100g = 3.02,
            fatPer100g = 18.6,
            defaultMeasureName = "fatia média",
            defaultMeasureGrams = 30.0,
            sourceCode = "BRC0052G",
            sourceLabel = "TBCA"
        ),
        FoodEntity(
            name = "Pão francês de padaria",
            kcalPer100g = 300.0,
            proteinPer100g = 9.83,
            carbsPer100g = 61.6,
            fatPer100g = 2.12,
            defaultMeasureName = "unidade média",
            defaultMeasureGrams = 50.0,
            sourceCode = "BRC0002A",
            sourceLabel = "TBCA"
        )
    )
}
