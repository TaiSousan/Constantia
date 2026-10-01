package br.com.taina.constantia.engine

enum class ReadingProgressMode { PAGES, PERCENT }
enum class ReadingStatus { WANT_TO_READ, READING, PAUSED, COMPLETED }

data class ReadingProgressResult(
    val currentPage: Int?,
    val totalPages: Int?,
    val progressPercent: Int,
    val status: ReadingStatus
)

class ReadingProgressEngine {
    fun normalize(
        mode: ReadingProgressMode,
        currentPage: Int?,
        totalPages: Int?,
        percent: Int,
        requestedStatus: ReadingStatus
    ): ReadingProgressResult {
        return when (mode) {
            ReadingProgressMode.PAGES -> normalizePages(currentPage, totalPages, requestedStatus)
            ReadingProgressMode.PERCENT -> normalizePercent(percent, requestedStatus)
        }
    }

    private fun normalizePages(
        currentPage: Int?,
        totalPages: Int?,
        requestedStatus: ReadingStatus
    ): ReadingProgressResult {
        val total = (totalPages ?: 0).coerceAtLeast(1)
        var current = (currentPage ?: 0).coerceIn(0, total)
        var progress = ((current.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        var status = requestedStatus

        if (status == ReadingStatus.COMPLETED) {
            current = total
            progress = 100
        } else if (progress >= 100) {
            status = ReadingStatus.COMPLETED
        } else if (progress > 0 && status == ReadingStatus.WANT_TO_READ) {
            status = ReadingStatus.READING
        }

        return ReadingProgressResult(current, total, progress, status)
    }

    private fun normalizePercent(percent: Int, requestedStatus: ReadingStatus): ReadingProgressResult {
        var progress = percent.coerceIn(0, 100)
        var status = requestedStatus

        if (status == ReadingStatus.COMPLETED) {
            progress = 100
        } else if (progress >= 100) {
            status = ReadingStatus.COMPLETED
        } else if (progress > 0 && status == ReadingStatus.WANT_TO_READ) {
            status = ReadingStatus.READING
        }

        return ReadingProgressResult(null, null, progress, status)
    }
}
