package br.com.taina.constantia.feature.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.taina.constantia.core.repository.ReadingBook
import br.com.taina.constantia.core.repository.ReadingLibraryStore
import br.com.taina.constantia.engine.ReadingProgressMode
import br.com.taina.constantia.engine.ReadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReadingViewModel(private val store: ReadingLibraryStore) : ViewModel() {
    val books: StateFlow<List<ReadingBook>> = store.books

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun saveBook(
        id: String?,
        title: String,
        author: String,
        mode: ReadingProgressMode,
        currentPage: Int?,
        totalPages: Int?,
        percent: Int,
        status: ReadingStatus
    ) {
        viewModelScope.launch {
            runCatching {
                store.saveBook(id, title, author, mode, currentPage, totalPages, percent, status)
            }.onSuccess {
                _message.value = if (id == null) "Livro adicionado à biblioteca." else "Livro atualizado."
            }.onFailure {
                _message.value = it.message ?: "Não foi possível salvar o livro."
            }
        }
    }

    fun updateProgress(book: ReadingBook, value: Int) {
        viewModelScope.launch {
            runCatching { store.updateProgress(book.id, value) }
                .onSuccess { updated ->
                    _message.value = if (updated?.status == ReadingStatus.COMPLETED) {
                        "Leitura concluída."
                    } else {
                        "Progresso atualizado."
                    }
                }
                .onFailure { _message.value = it.message ?: "Não foi possível atualizar o progresso." }
        }
    }

    fun deleteBook(book: ReadingBook) {
        viewModelScope.launch {
            runCatching { store.deleteBook(book.id) }
                .onSuccess { _message.value = "Livro removido da biblioteca." }
                .onFailure { _message.value = it.message ?: "Não foi possível remover o livro." }
        }
    }

    fun clearMessage() { _message.value = null }

    class Factory(private val store: ReadingLibraryStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ReadingViewModel(store) as T
    }
}
