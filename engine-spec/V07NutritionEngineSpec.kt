import kotlin.math.round

data class FoodSpec(
    val kcal100: Double,
    val protein100: Double,
    val carbs100: Double,
    val fat100: Double,
    val measureGrams: Double
)

data class EstimateSpec(val grams: Double, val kcal: Double, val protein: Double, val carbs: Double, val fat: Double, val confidence: String)

fun estimate(food: FoodSpec, amount: Double, mode: String): EstimateSpec {
    val grams = when (mode) {
        "GRAMS" -> amount
        "MEASURE" -> amount * food.measureGrams
        else -> amount
    }.coerceIn(0.0, 5000.0)
    val factor = grams / 100.0
    fun r(v: Double) = round(v * 10.0) / 10.0
    return EstimateSpec(
        grams = r(grams),
        kcal = r(food.kcal100 * factor),
        protein = r(food.protein100 * factor),
        carbs = r(food.carbs100 * factor),
        fat = r(food.fat100 * factor),
        confidence = when (mode) { "GRAMS" -> "HIGH"; "MEASURE" -> "MEDIUM"; else -> "LOW" }
    )
}

fun movingAverage7Days(points: List<Pair<Long, Double>>, now: Long): Double? {
    val cutoff = now - 7L * 24L * 60L * 60L * 1000L
    val values = points.filter { it.first in cutoff..now && it.second > 0 }.map { it.second }
    if (values.isEmpty()) return null
    return round(values.average() * 10.0) / 10.0
}

fun main() {
    val rice = FoodSpec(131.0, 2.38, 30.0, 0.41, 20.0)
    val twoSpoons = estimate(rice, 2.0, "MEASURE")
    check(twoSpoons.grams == 40.0)
    check(twoSpoons.kcal == 52.4)
    check(twoSpoons.confidence == "MEDIUM")

    val chicken = FoodSpec(149.0, 31.8, 0.0, 2.46, 110.0)
    val weighed = estimate(chicken, 120.0, "GRAMS")
    check(weighed.kcal == 178.8)
    check(weighed.protein == 38.2)
    check(weighed.confidence == "HIGH")

    val estimated = estimate(chicken, 120.0, "ESTIMATE")
    check(estimated.confidence == "LOW")

    val day = 24L * 60L * 60L * 1000L
    val now = 10L * day
    val points = listOf(
        now to 86.0,
        (now - day) to 85.8,
        (now - 2*day) to 85.9,
        (now - 4*day) to 86.1,
        (now - 6*day) to 85.7,
        (now - 8*day) to 90.0
    )
    val avg = movingAverage7Days(points, now)
    check(avg == 85.9) { "avg=$avg" }

    println("Constantia v0.7 nutrition engine tests: OK")
}
