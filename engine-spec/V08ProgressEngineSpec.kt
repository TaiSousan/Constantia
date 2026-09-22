import br.com.taina.constantia.engine.*

fun main() {
    val engine = ProgressEngine()

    check(AdherenceMetric(3, 4).percent == 75)
    check(AdherenceMetric(6, 4).percent == 100)
    check(AdherenceMetric(0, 0).percent == null)
    check(FocusMetric(4, 5).percent == 80)

    val failures = engine.aggregateFailureReasons(
        listOf("Procrastinei", "Fiquei sem tempo", "Procrastinei", "Imprevisto")
    )
    check(failures.first().reason == "Procrastinei")
    check(failures.first().count == 2)

    val lowTraining = engine.suggestions(
        training = AdherenceMetric(1, 4),
        study = AdherenceMetric(3, 4),
        activities = AdherenceMetric(6, 7),
        focus = FocusMetric(7, 8),
        failureReasons = failures
    )
    check(lowTraining.any { it.area == "Treino" })

    val lowFocus = engine.suggestions(
        training = AdherenceMetric(4, 4),
        study = AdherenceMetric(4, 4),
        activities = AdherenceMetric(7, 7),
        focus = FocusMetric(2, 6),
        failureReasons = emptyList()
    )
    check(lowFocus.any { it.area == "Foco" })

    val consistent = engine.suggestions(
        training = AdherenceMetric(4, 4),
        study = AdherenceMetric(5, 5),
        activities = AdherenceMetric(7, 7),
        focus = FocusMetric(8, 9),
        failureReasons = emptyList()
    )
    check(consistent.any { it.title.contains("Manter") })

    check(engine.average(listOf(86.0, 85.5, 85.0)) == 85.5)
    println("Constantia v0.8 progress engine tests: OK")
}
