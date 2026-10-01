import br.com.taina.constantia.engine.ReadingProgressEngine
import br.com.taina.constantia.engine.ReadingProgressMode
import br.com.taina.constantia.engine.ReadingStatus

private fun checkReading(condition: Boolean, message: String) {
    if (!condition) error("RC3.3 reading spec failed: $message")
}

fun main() {
    val engine = ReadingProgressEngine()

    val pages = engine.normalize(
        mode = ReadingProgressMode.PAGES,
        currentPage = 126,
        totalPages = 420,
        percent = 0,
        requestedStatus = ReadingStatus.WANT_TO_READ
    )
    checkReading(pages.progressPercent == 30, "126/420 deve resultar em 30%")
    checkReading(pages.status == ReadingStatus.READING, "progresso positivo deve iniciar leitura")

    val finishedPages = engine.normalize(
        mode = ReadingProgressMode.PAGES,
        currentPage = 420,
        totalPages = 420,
        percent = 0,
        requestedStatus = ReadingStatus.READING
    )
    checkReading(finishedPages.progressPercent == 100, "última página deve concluir")
    checkReading(finishedPages.status == ReadingStatus.COMPLETED, "100% deve marcar concluído")

    val percent = engine.normalize(
        mode = ReadingProgressMode.PERCENT,
        currentPage = null,
        totalPages = null,
        percent = 45,
        requestedStatus = ReadingStatus.READING
    )
    checkReading(percent.progressPercent == 45, "modo percentual deve preservar 45%")
    checkReading(percent.currentPage == null && percent.totalPages == null, "modo percentual não deve inventar páginas")

    val explicitComplete = engine.normalize(
        mode = ReadingProgressMode.PERCENT,
        currentPage = null,
        totalPages = null,
        percent = 20,
        requestedStatus = ReadingStatus.COMPLETED
    )
    checkReading(explicitComplete.progressPercent == 100, "conclusão explícita deve fechar progresso")

    println("RC3.3 reading spec OK")
}
