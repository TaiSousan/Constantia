package br.com.taina.constantia.engine

import br.com.taina.constantia.core.database.FoodEntity
import java.text.Normalizer
import kotlin.math.round

/**
 * Parser local-first para descrições simples de refeições em português.
 * Ele nunca inventa um alimento ausente do catálogo: trechos não reconhecidos
 * permanecem em [MealParseResult.unmatchedFragments] para revisão humana.
 */
data class ParsedMealItem(
    val food: FoodEntity,
    val amount: Double,
    val unitMode: String,
    val displayAmount: String,
    val estimatedGrams: Double,
    val minGrams: Double,
    val maxGrams: Double,
    val confidence: NutritionConfidence
)

data class MealNutritionRange(
    val minKcal: Double,
    val maxKcal: Double,
    val minProtein: Double,
    val maxProtein: Double,
    val minCarbs: Double,
    val maxCarbs: Double,
    val minFat: Double,
    val maxFat: Double
)

data class MealParseResult(
    val items: List<ParsedMealItem>,
    val unmatchedFragments: List<String>,
    val range: MealNutritionRange
)

class SmartMealParser {
    private data class AliasRule(val aliases: List<String>, val foodNameContains: String)

    private val rules = listOf(
        AliasRule(listOf("arroz", "arroz branco"), "arroz branco"),
        AliasRule(listOf("frango", "peito de frango", "file de frango"), "peito de frango"),
        AliasRule(listOf("ovo", "ovos"), "ovo de galinha"),
        AliasRule(listOf("banana", "banana prata"), "banana prata"),
        AliasRule(listOf("queijo minas", "minas frescal", "queijo"), "queijo minas"),
        AliasRule(listOf("pao frances", "pao", "frances"), "pão francês")
    )

    fun parse(text: String, foods: List<FoodEntity>): MealParseResult {
        if (text.isBlank()) return MealParseResult(emptyList(), emptyList(), zeroRange())
        val fragments = splitFragments(text)
        val parsed = mutableListOf<ParsedMealItem>()
        val unmatched = mutableListOf<String>()

        for (fragment in fragments) {
            val normalized = normalize(fragment)
            val rule = rules.firstOrNull { r -> r.aliases.any { normalizeAlias -> normalized.contains(normalize(normalizeAlias)) } }
            val food = rule?.let { r -> foods.firstOrNull { normalize(it.name).contains(normalize(r.foodNameContains)) } }
            if (food == null) {
                if (fragment.isNotBlank()) unmatched += fragment.trim()
                continue
            }
            parsed += parseAmount(fragment, food)
        }

        return MealParseResult(parsed, unmatched, calculateRange(parsed))
    }

    private fun splitFragments(text: String): List<String> = text
        .replace(";", ",")
        .split(Regex(",|\\s+e\\s+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    private fun parseAmount(fragment: String, food: FoodEntity): ParsedMealItem {
        val normalized = normalize(fragment)
        val gramsMatch = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:g|grama|gramas)\\b", RegexOption.IGNORE_CASE).find(fragment)
        if (gramsMatch != null) {
            val grams = gramsMatch.groupValues[1].replace(',', '.').toDoubleOrNull()?.coerceAtLeast(1.0) ?: food.defaultMeasureGrams
            return item(food, grams, "GRAMS", "${trimNumber(grams)} g", grams, grams, grams, NutritionConfidence.HIGH)
        }

        val number = Regex("\\b(\\d+(?:[.,]\\d+)?)\\b").find(fragment)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
            ?: wordNumber(normalized)
        val count = (number ?: 1.0).coerceIn(0.25, 20.0)
        val measureWords = listOf("colher", "colheres", "fatia", "fatias", "unidade", "unidades", "file", "files")
        val hasMeasure = measureWords.any { normalized.contains(it) } || food.defaultMeasureName.split(' ').any { normalize(it).length > 3 && normalized.contains(normalize(it)) }

        return if (hasMeasure || number != null) {
            val grams = count * food.defaultMeasureGrams
            item(
                food, count, "MEASURE", "${trimNumber(count)} ${food.defaultMeasureName}", grams,
                grams * 0.85, grams * 1.15, NutritionConfidence.MEDIUM
            )
        } else {
            val grams = food.defaultMeasureGrams
            item(
                food, grams, "FREE", "~${trimNumber(grams)} g", grams,
                grams * 0.70, grams * 1.30, NutritionConfidence.LOW
            )
        }
    }

    private fun item(
        food: FoodEntity,
        amount: Double,
        unitMode: String,
        display: String,
        grams: Double,
        min: Double,
        max: Double,
        confidence: NutritionConfidence
    ) = ParsedMealItem(food, amount, unitMode, display, round1(grams), round1(min), round1(max), confidence)

    private fun calculateRange(items: List<ParsedMealItem>): MealNutritionRange {
        fun sumFor(selector: (FoodEntity) -> Double, useMax: Boolean): Double = items.sumOf { item ->
            selector(item.food) * (if (useMax) item.maxGrams else item.minGrams) / 100.0
        }
        return MealNutritionRange(
            minKcal = round1(sumFor({ it.kcalPer100g }, false)),
            maxKcal = round1(sumFor({ it.kcalPer100g }, true)),
            minProtein = round1(sumFor({ it.proteinPer100g }, false)),
            maxProtein = round1(sumFor({ it.proteinPer100g }, true)),
            minCarbs = round1(sumFor({ it.carbsPer100g }, false)),
            maxCarbs = round1(sumFor({ it.carbsPer100g }, true)),
            minFat = round1(sumFor({ it.fatPer100g }, false)),
            maxFat = round1(sumFor({ it.fatPer100g }, true))
        )
    }

    private fun wordNumber(normalized: String): Double? = when {
        Regex("\\b(um|uma)\\b").containsMatchIn(normalized) -> 1.0
        Regex("\\b(dois|duas)\\b").containsMatchIn(normalized) -> 2.0
        Regex("\\btres\\b").containsMatchIn(normalized) -> 3.0
        Regex("\\bquatro\\b").containsMatchIn(normalized) -> 4.0
        else -> null
    }

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")

    private fun trimNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else round1(value).toString()
    private fun round1(value: Double): Double = round(value * 10.0) / 10.0
    private fun zeroRange() = MealNutritionRange(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
}
