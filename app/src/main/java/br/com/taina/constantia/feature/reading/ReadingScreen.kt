@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package br.com.taina.constantia.feature.reading

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.taina.constantia.core.repository.ReadingBook
import br.com.taina.constantia.engine.ReadingProgressMode
import br.com.taina.constantia.engine.ReadingStatus

@Composable
fun ReadingScreen(viewModel: ReadingViewModel, onBack: () -> Unit) {
    val books by viewModel.books.collectAsState()
    val message by viewModel.message.collectAsState()
    var editingBook by remember { mutableStateOf<ReadingBook?>(null) }
    var showNewBook by remember { mutableStateOf(false) }
    var progressBook by remember { mutableStateOf<ReadingBook?>(null) }
    var deletingBook by remember { mutableStateOf<ReadingBook?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewBook = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar livro")
            }
        },
        snackbarHost = {
            if (message != null) {
                Snackbar(action = { TextButton(onClick = viewModel::clearMessage) { Text("OK") } }) {
                    Text(message.orEmpty())
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val reading = books.count { it.status == ReadingStatus.READING }
                val completed = books.count { it.status == ReadingStatus.COMPLETED }
                Text("Leituras", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${books.size} livro(s) · $reading lendo · $completed concluído(s)",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "O progresso fica salvo somente neste aparelho e não altera metas, estudos ou o banco principal do Constantia.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (books.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Sua biblioteca está vazia", style = MaterialTheme.typography.titleMedium)
                            Text("Cadastre um livro e acompanhe a leitura por página ou por porcentagem.")
                            Button(onClick = { showNewBook = true }) { Text("Adicionar primeiro livro") }
                        }
                    }
                }
            } else {
                items(books, key = { it.id }) { book ->
                    ReadingBookCard(
                        book = book,
                        onUpdateProgress = { progressBook = book },
                        onEdit = { editingBook = book },
                        onDelete = { deletingBook = book }
                    )
                }
            }
        }
    }

    if (showNewBook) {
        BookEditorDialog(
            book = null,
            onDismiss = { showNewBook = false },
            onSave = { id, title, author, mode, currentPage, totalPages, percent, status ->
                viewModel.saveBook(id, title, author, mode, currentPage, totalPages, percent, status)
                showNewBook = false
            }
        )
    }

    editingBook?.let { book ->
        BookEditorDialog(
            book = book,
            onDismiss = { editingBook = null },
            onSave = { id, title, author, mode, currentPage, totalPages, percent, status ->
                viewModel.saveBook(id, title, author, mode, currentPage, totalPages, percent, status)
                editingBook = null
            }
        )
    }

    progressBook?.let { book ->
        ProgressDialog(
            book = book,
            onDismiss = { progressBook = null },
            onSave = { value ->
                viewModel.updateProgress(book, value)
                progressBook = null
            }
        )
    }

    deletingBook?.let { book ->
        AlertDialog(
            onDismissRequest = { deletingBook = null },
            title = { Text("Remover livro?") },
            text = { Text("${book.title} será removido da biblioteca local. Esta ação não altera outros dados do Constantia.") },
            confirmButton = {
                Button(onClick = { viewModel.deleteBook(book); deletingBook = null }) { Text("Remover") }
            },
            dismissButton = { TextButton(onClick = { deletingBook = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ReadingBookCard(
    book: ReadingBook,
    onUpdateProgress: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (book.author.isNotBlank()) Text(book.author, style = MaterialTheme.typography.bodySmall)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opções do livro")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuExpanded = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Remover") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { book.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(progressText(book), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                SuggestionChip(onClick = {}, label = { Text(statusLabel(book.status)) }, enabled = false)
            }
            OutlinedButton(onClick = onUpdateProgress, modifier = Modifier.fillMaxWidth()) {
                Text(if (book.status == ReadingStatus.COMPLETED) "Revisar progresso" else "Atualizar progresso")
            }
        }
    }
}

@Composable
private fun BookEditorDialog(
    book: ReadingBook?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, ReadingProgressMode, Int?, Int?, Int, ReadingStatus) -> Unit
) {
    var title by remember(book?.id) { mutableStateOf(book?.title.orEmpty()) }
    var author by remember(book?.id) { mutableStateOf(book?.author.orEmpty()) }
    var mode by remember(book?.id) { mutableStateOf(book?.progressMode ?: ReadingProgressMode.PAGES) }
    var totalPages by remember(book?.id) { mutableStateOf(book?.totalPages?.toString().orEmpty()) }
    var currentPage by remember(book?.id) { mutableStateOf(book?.currentPage?.toString() ?: "0") }
    var percent by remember(book?.id) { mutableStateOf((book?.progressPercent ?: 0).toString()) }
    var status by remember(book?.id) { mutableStateOf(book?.status ?: ReadingStatus.WANT_TO_READ) }
    var modeExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    val valid = title.isNotBlank() && when (mode) {
        ReadingProgressMode.PAGES -> (totalPages.toIntOrNull() ?: 0) > 0
        ReadingProgressMode.PERCENT -> true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (book == null) "Adicionar livro" else "Editar livro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(author, { author = it }, label = { Text("Autor (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = modeExpanded, onExpandedChange = { modeExpanded = !modeExpanded }) {
                    OutlinedTextField(
                        value = if (mode == ReadingProgressMode.PAGES) "Número de páginas" else "Porcentagem",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Acompanhar por") },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                        DropdownMenuItem(text = { Text("Número de páginas") }, onClick = { mode = ReadingProgressMode.PAGES; modeExpanded = false })
                        DropdownMenuItem(text = { Text("Porcentagem") }, onClick = { mode = ReadingProgressMode.PERCENT; modeExpanded = false })
                    }
                }

                if (mode == ReadingProgressMode.PAGES) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentPage,
                            onValueChange = { currentPage = it.filter(Char::isDigit).take(6) },
                            label = { Text("Página atual") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = totalPages,
                            onValueChange = { totalPages = it.filter(Char::isDigit).take(6) },
                            label = { Text("Total") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = percent,
                        onValueChange = { percent = it.filter(Char::isDigit).take(3) },
                        label = { Text("Progresso (%)") },
                        supportingText = { Text("0 a 100") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = !statusExpanded }) {
                    OutlinedTextField(
                        value = statusLabel(status),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        ReadingStatus.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(statusLabel(option)) }, onClick = { status = option; statusExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        book?.id,
                        title,
                        author,
                        mode,
                        currentPage.toIntOrNull(),
                        totalPages.toIntOrNull(),
                        percent.toIntOrNull() ?: 0,
                        status
                    )
                },
                enabled = valid
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ProgressDialog(book: ReadingBook, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val initial = if (book.progressMode == ReadingProgressMode.PAGES) book.currentPage ?: 0 else book.progressPercent
    var value by remember(book.id) { mutableStateOf(initial.toString()) }
    val parsed = value.toIntOrNull()
    val valid = when (book.progressMode) {
        ReadingProgressMode.PAGES -> parsed != null && parsed >= 0 && parsed <= (book.totalPages ?: Int.MAX_VALUE)
        ReadingProgressMode.PERCENT -> parsed != null && parsed in 0..100
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atualizar progresso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(book.title, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter(Char::isDigit).take(6) },
                    label = { Text(if (book.progressMode == ReadingProgressMode.PAGES) "Página atual" else "Progresso (%)") },
                    supportingText = {
                        if (book.progressMode == ReadingProgressMode.PAGES) Text("Total: ${book.totalPages ?: 0} páginas") else Text("0 a 100")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { parsed?.let(onSave) }, enabled = valid) { Text("Atualizar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun progressText(book: ReadingBook): String = when (book.progressMode) {
    ReadingProgressMode.PAGES -> "${book.currentPage ?: 0} / ${book.totalPages ?: 0} páginas · ${book.progressPercent}%"
    ReadingProgressMode.PERCENT -> "${book.progressPercent}% concluído"
}

private fun statusLabel(status: ReadingStatus): String = when (status) {
    ReadingStatus.WANT_TO_READ -> "Quero ler"
    ReadingStatus.READING -> "Lendo"
    ReadingStatus.PAUSED -> "Pausado"
    ReadingStatus.COMPLETED -> "Concluído"
}
