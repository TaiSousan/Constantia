package br.com.taina.constantia.core.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class StudyMaterialSummary(
    val id: String,
    val title: String,
    val sourceFileName: String,
    val subjectId: Long?,
    val pageCount: Int,
    val charCount: Int,
    val importedAtMillis: Long
)

data class StudyMaterialHit(
    val materialId: String,
    val pageNumber: Int,
    val snippet: String
)

/**
 * Biblioteca local de materiais de estudo.
 *
 * O PDF selecionado nunca é mantido como documento do Constantia. Ele é copiado
 * apenas para cache durante a extração, convertido em texto por página e apagado
 * no bloco finally. O índice textual permanece em filesDir até o usuário removê-lo.
 * Isso mantém o app offline e evita alterar o schema do Room na RC3.3.
 */
class StudyMaterialStore(private val context: Context) {
    private val directory = File(context.filesDir, "study_materials").apply { mkdirs() }

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    suspend fun list(): List<StudyMaterialSummary> = withContext(Dispatchers.IO) {
        directory.listFiles { file -> file.extension == "json" }
            .orEmpty()
            .mapNotNull(::readSummary)
            .sortedByDescending { it.importedAtMillis }
    }

    suspend fun importPdf(uri: Uri, subjectId: Long?): StudyMaterialSummary = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri) ?: "Material de estudo.pdf"
        val temp = File.createTempFile("constantia-study-", ".pdf", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Não foi possível abrir o PDF selecionado." }
                temp.outputStream().buffered().use { output -> input.copyTo(output) }
            }

            PDDocument.load(temp).use { document ->
                val id = UUID.randomUUID().toString()
                val pages = JSONArray()
                var totalChars = 0
                val stripper = PDFTextStripper()
                for (page in 1..document.numberOfPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    val text = cleanExtractedText(stripper.getText(document))
                    totalChars += text.length
                    pages.put(JSONObject().apply {
                        put("page", page)
                        put("text", text)
                    })
                }

                require(totalChars >= 100) {
                    "Este PDF não contém texto pesquisável suficiente. PDFs escaneados ainda precisam de OCR antes da importação."
                }
                val title = displayName.removeSuffix(".pdf").removeSuffix(".PDF").trim().ifBlank { "Material de estudo" }
                val importedAt = System.currentTimeMillis()
                val root = JSONObject().apply {
                    put("version", 1)
                    put("id", id)
                    put("title", title)
                    put("sourceFileName", displayName)
                    if (subjectId != null) put("subjectId", subjectId) else put("subjectId", JSONObject.NULL)
                    put("pageCount", document.numberOfPages)
                    put("charCount", totalChars)
                    put("importedAtMillis", importedAt)
                    put("pages", pages)
                }
                File(directory, "$id.json").writeText(root.toString())
                StudyMaterialSummary(id, title, displayName, subjectId, document.numberOfPages, totalChars, importedAt)
            }
        } finally {
            temp.delete()
        }
    }

    suspend fun delete(materialId: String): Boolean = withContext(Dispatchers.IO) {
        File(directory, "$materialId.json").delete()
    }

    suspend fun search(materialId: String, query: String, limit: Int = 12): List<StudyMaterialHit> = withContext(Dispatchers.IO) {
        val term = query.trim()
        if (term.length < 2) return@withContext emptyList()
        val file = File(directory, "$materialId.json")
        if (!file.exists()) return@withContext emptyList()
        val root = JSONObject(file.readText())
        val pages = root.getJSONArray("pages")
        buildList {
            for (i in 0 until pages.length()) {
                if (size >= limit.coerceIn(1, 30)) break
                val page = pages.getJSONObject(i)
                val text = page.optString("text")
                val index = text.indexOf(term, ignoreCase = true)
                if (index >= 0) {
                    val from = (index - 140).coerceAtLeast(0)
                    val to = (index + term.length + 240).coerceAtMost(text.length)
                    val snippet = text.substring(from, to).replace(Regex("\\s+"), " ").trim()
                    add(StudyMaterialHit(materialId, page.optInt("page", i + 1), snippet))
                }
            }
        }
    }

    private fun readSummary(file: File): StudyMaterialSummary? = runCatching {
        val root = JSONObject(file.readText())
        StudyMaterialSummary(
            id = root.getString("id"),
            title = root.optString("title", root.optString("sourceFileName", "Material")),
            sourceFileName = root.optString("sourceFileName", "material.pdf"),
            subjectId = if (root.isNull("subjectId")) null else root.optLong("subjectId"),
            pageCount = root.optInt("pageCount"),
            charCount = root.optInt("charCount"),
            importedAtMillis = root.optLong("importedAtMillis")
        )
    }.getOrNull()

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return uri.lastPathSegment
    }

    private fun cleanExtractedText(raw: String): String = raw
        .replace('\u0000', ' ')
        .replace("\r\n", "\n")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}
