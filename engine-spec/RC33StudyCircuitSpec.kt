import br.com.taina.constantia.core.database.RestrictionEntity
import br.com.taina.constantia.engine.CircuitEquipment
import br.com.taina.constantia.engine.CircuitPurpose
import br.com.taina.constantia.engine.CircuitWorkoutEngine
import br.com.taina.constantia.engine.PomodoroCycleEngine
import br.com.taina.constantia.engine.PomodoroPhase

fun main() {
    val pomodoro = PomodoroCycleEngine()
    val afterStudy = pomodoro.completePhase(PomodoroPhase.STUDY, 50, 10, true)
    check(afterStudy.next == PomodoroPhase.BREAK)
    check(afterStudy.nextSeconds == 600)
    check(afterStudy.autoStartNext)

    val afterBreak = pomodoro.completePhase(PomodoroPhase.BREAK, 50, 10, true)
    check(afterBreak.next == PomodoroPhase.STUDY)
    check(afterBreak.nextSeconds == 3000)
    check(!afterBreak.autoStartNext)

    val circuit = CircuitWorkoutEngine().build(
        minutes = 15,
        equipment = setOf(CircuitEquipment.BODYWEIGHT, CircuitEquipment.DUMBBELLS),
        purpose = CircuitPurpose.CARDIO_REPLACEMENT,
        restrictions = listOf(
            RestrictionEntity(
                restrictionType = "MOVEMENT_AVOID",
                description = "Evitar padrão de agachamento",
                movementPattern = "SQUAT"
            )
        )
    )
    check(circuit.minutes == 15)
    check(circuit.moves.isNotEmpty())
    check(circuit.moves.none { it.movementPattern == "SQUAT" })
    check(circuit.moves.all { it.equipment in setOf(CircuitEquipment.BODYWEIGHT, CircuitEquipment.DUMBBELLS) })
    check(circuit.purpose == CircuitPurpose.CARDIO_REPLACEMENT)

    println("Constantia RC3.3 study/pomodoro/circuit tests: OK")
}
