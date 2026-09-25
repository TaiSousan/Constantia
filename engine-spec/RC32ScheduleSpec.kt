import br.com.taina.constantia.engine.TrainingScheduleEngine
import java.time.LocalDate

fun main() {
    val engine = TrainingScheduleEngine()
    val csv = "1,2,3,5,7"

    check(engine.templateIndexFor(LocalDate.of(2026, 9, 23), 5, csv) == 2)
    check(engine.templateIndexFor(LocalDate.of(2026, 9, 24), 5, csv) == null)
    check(engine.templateIndexFor(LocalDate.of(2026, 9, 27), 5, csv) == 4)

    println("Constantia RC3.2 schedule semantics tests: OK")
}
