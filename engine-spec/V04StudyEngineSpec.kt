import br.com.taina.constantia.engine.PomodoroAdaptationEngine
import br.com.taina.constantia.engine.StudyReviewEngine
import br.com.taina.constantia.engine.StudyScheduleEngine

fun main() {
    val review = StudyReviewEngine()
    check(review.nextInterval(0, 0, StudyReviewEngine.Rating.AGAIN).nextIntervalDays == 1)
    check(review.nextInterval(0, 0, StudyReviewEngine.Rating.GOOD).nextIntervalDays == 3)
    check(review.nextInterval(3, 1, StudyReviewEngine.Rating.GOOD).nextIntervalDays == 6)
    check(review.nextInterval(6, 2, StudyReviewEngine.Rating.EASY).nextIntervalDays == 18)

    val schedule = StudyScheduleEngine()
    check(schedule.isDueToday(3, 0, 1))
    check(!schedule.isDueToday(3, 1, 2))
    check(schedule.isDueToday(3, 1, 3))
    check(!schedule.isDueToday(3, 3, 7))

    val pomo = PomodoroAdaptationEngine()
    check(pomo.evaluate(25, List(10) { true }).action == PomodoroAdaptationEngine.Action.INCREASE)
    check(pomo.evaluate(30, List(10) { it < 4 }).action == PomodoroAdaptationEngine.Action.DECREASE)
    check(pomo.evaluate(25, List(5) { true }).action == PomodoroAdaptationEngine.Action.MAINTAIN)

    println("Constantia v0.4 study engine tests: OK")
}
