package br.com.taina.constantia.core.repository

import android.content.Context
import android.util.AtomicFile
import br.com.taina.constantia.engine.ReadingProgressEngine
import br.com.taina.constantia.engine.ReadingProgressMode
import br.com.taina.constantia.engine.ReadingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ReadingBook(
    val id: String,
    val title: String,
    val author: String = "",
    val progressMode: ReadingProgressMode = ReadingProgressMode.PAGES,
    val currentPage: Int? = 0,
    val totalPages: Int? = null,
    val progressPercent: Int = 0,
    val status: ReadingStatus = ReadingStatus.WANT_TO_READ,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long? = null
)

/**
 * Biblioteca de leitura local e independente do Room.
 *
 * O arquivo fica em filesDir e é escrito com AtomicFile, portanto adicionar livros não
 * muda o schema do banco e não coloca em risco o histórico já existente do Constantia.
 */
class ReadingLibraryStore(
    context: Context,
    private val progressEngine: ReadingProgressEngine = ReadingProgressEngine()
) {
    private val atomicFile = AtomicFile(File(context.filesDir, FILE_NAME))
    private val mutex = Mutex()
    private val _books = MutableStateFlow(load())
    val books: StateFlow<List<ReadingBook>> = _books.asStateFlow()

    suspend fun saveBook(
        id: String?,
        title: String,
        author: String,
        mode: ReadingProgressMode,
        currentPage: Int?,
        totalPages: Int?,
        percent: Int,
        status: ReadingStatus
    ): ReadingBook = withContext(Dispatchers.IO) {
        mutex.withLock {
            require(title.isNotBlank()) { "Informe o título do livro." }
            if (mode == ReadingProgressMode.PAGES) {
                require((totalPages ?: 0) > 0) { "Informe o total de páginas." }
            }

            val now = System.currentTimeMillis()
            val existing = id?.let { bookId -> _books.value.firstOrNull { it.id == bookId } }
            val normalized = progressEngine.normalize(mode, currentPage, totalPages, percent, status)
            val completedAt = when {
                normalized.status == ReadingStatus.COMPLETED && existing?.completedAtMillis != null -> existing.completedAtMillis
                normalized.status == ReadingStatus.COMPLETED -> now
                else -> null
            }
            val book = ReadingBook(
                id = existing?.id ?: UUID.randomUUID().toString(),
                title = title.trim(),
                author = author.trim(),
                progressMode = mode,
                currentPage = normalized.currentPage,
                totalPages = normalized.totalPages,
                progressPercent = normalized.progressPercent,
                status = normalized.status,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now,
                completedAtMillis = completedAt
            )
            val next = _books.value.filterNot { it.id == book.id } + book
            persistAndPublish(next)
            book
        }
    }

    suspend fun updateProgress(bookId: String, value: Int): ReadingBook? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val existing = _books.value.firstOrNull { it.id == bookId } ?: return@withLock null
            val statusForProgressUpdate = if (existing.status == ReadingStatus.COMPLETED) ReadingStatus.READING else existing.status
            val normalized = when (existing.progressMode) {
                ReadingProgressMode.PAGES -> progressEngine.normalize(
                    mode = existing.progressMode,
                    currentPage = value,
                    totalPages = existing.totalPages,
                    percent = existing.progressPercent,
                    requestedStatus = statusForProgressUpdate
                )
                ReadingProgressMode.PERCENT -> progressEngine.normalize(
                    mode = existing.progressMode,
                    currentPage = null,
                    totalPages = null,
                    percent = value,
                    requestedStatus = statusForProgressUpdate
                )
            }
            val now = System.currentTimeMillis()
            val updated = existing.copy(
                currentPage = normalized.currentPage,
                totalPages = normalized.totalPages,
                progressPercent = normalized.progressPercent,
                status = normalized.status,
                updatedAtMillis = now,
                completedAtMillis = when {
                    normalized.status == ReadingStatus.COMPLETED -> existing.completedAtMillis ?: now
                    else -> null
                }
            )
            persistAndPublish(_books.value.map { if (it.id == updated.id) updated else it })
            updated
        }
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            persistAndPublish(_books.value.filterNot { it.id == bookId })
        }
    }

    private fun persistAndPublish(items: List<ReadingBook>) {
        val sorted = sortBooks(items)
        val root = JSONObject().put("version", 1).put("books", JSONArray().apply {
            sorted.forEach { book -> put(book.toJson()) }
        })
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(root.toString().toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
            stream = null
            _books.value = sorted
        } catch (error: Throwable) {
            stream?.let { atomicFile.failWrite(it) }
            throw error
        }
    }

    private fun load(): List<ReadingBook> = runCatching {
        val text = atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (text.isBlank()) return@runCatching emptyList()
        val array = JSONObject(text).optJSONArray("books") ?: JSONArray()
        buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.toBook()?.let(::add)
            }
        }
    }.getOrDefault(emptyList()).let(::sortBooks)

    private fun sortBooks(items: List<ReadingBook>): List<ReadingBook> = items.sortedWith(
        compareBy<ReadingBook> { statusOrder(it.status) }
            .thenByDescending { it.updatedAtMillis }
            .thenBy { it.title.lowercase() }
    )

    private fun statusOrder(status: ReadingStatus): Int = when (status) {
        ReadingStatus.READING -> 0
        ReadingStatus.PAUSED -> 1
        ReadingStatus.WANT_TO_READ -> 2
        ReadingStatus.COMPLETED -> 3
    }

    private fun ReadingBook.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("author", author)
        .put("progressMode", progressMode.name)
        .put("currentPage", currentPage ?: JSONObject.NULL)
        .put("totalPages", totalPages ?: JSONObject.NULL)
        .put("progressPercent", progressPercent)
        .put("status", status.name)
        .put("createdAtMillis", createdAtMillis)
        .put("updatedAtMillis", updatedAtMillis)
        .put("completedAtMillis", completedAtMillis ?: JSONObject.NULL)

    private fun JSONObject.toBook(): ReadingBook? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val title = optString("title").takeIf { it.isNotBlank() } ?: return null
        val mode = runCatching { ReadingProgressMode.valueOf(optString("progressMode")) }
            .getOrDefault(ReadingProgressMode.PAGES)
        val status = runCatching { ReadingStatus.valueOf(optString("status")) }
            .getOrDefault(ReadingStatus.WANT_TO_READ)
        return ReadingBook(
            id = id,
            title = title,
            author = optString("author"),
            progressMode = mode,
            currentPage = if (isNull("currentPage")) null else optInt("currentPage", 0),
            totalPages = if (isNull("totalPages")) null else optInt("totalPages", 0).takeIf { it > 0 },
            progressPercent = optInt("progressPercent", 0).coerceIn(0, 100),
            status = status,
            createdAtMillis = optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = optLong("updatedAtMillis", System.currentTimeMillis()),
            completedAtMillis = if (isNull("completedAtMillis")) null else optLong("completedAtMillis")
        )
    }

    private companion object {
        const val FILE_NAME = "reading_library.json"
    }
}
