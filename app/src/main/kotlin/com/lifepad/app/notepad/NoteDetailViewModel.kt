package com.lifepad.app.notepad

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.lifepad.app.data.local.entity.AttachmentEntity
import com.lifepad.app.data.repository.AttachmentRepository
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    noteRepository: NoteRepository,
    attachmentRepository: AttachmentRepository,
    private val journalRepository: JournalRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {
    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L
    private val repository = noteRepository

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val note: StateFlow<com.lifepad.app.data.local.entity.NoteEntity?> =
        noteRepository.observeNote(noteId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val attachments: StateFlow<List<AttachmentEntity>> =
        attachmentRepository.getAttachments(noteId, ITEM_TYPE_NOTE).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun openWikilink(
        target: String,
        onNoteResolved: (Long) -> Unit,
        onJournalResolved: (Long) -> Unit,
        onTransactionResolved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val prefix = target.substringBefore(":", "")
            val value = target.substringAfter(":", target).trim()
            when (prefix.lowercase()) {
                "journal", "entry" -> resolveJournalLink(value, onJournalResolved)
                "finance", "transaction" -> resolveTransactionLink(value, onTransactionResolved)
                else -> {
                    val note = repository.getNoteByTitleIgnoreCase(target)
                    if (note != null) {
                        onNoteResolved(note.id)
                    } else {
                        _errorMessage.value = "Linked note not found: $target"
                    }
                }
            }
        }
    }

    private suspend fun resolveJournalLink(value: String, onResolved: (Long) -> Unit) {
        val id = value.toLongOrNull()
        if (id != null) {
            val entry = journalRepository.getEntryById(id)
            if (entry != null) {
                onResolved(entry.id)
            } else {
                _errorMessage.value = "Journal entry not found: $value"
            }
            return
        }
        val results = journalRepository.searchEntries(value)
        if (results.isEmpty()) {
            _errorMessage.value = "No journal entries match: $value"
        } else {
            val entry = results.maxByOrNull { it.updatedAt } ?: results.first()
            onResolved(entry.id)
            if (results.size > 1) {
                _errorMessage.value = "Multiple journal entries matched \"$value\". Opened most recent."
            }
        }
    }

    private suspend fun resolveTransactionLink(value: String, onResolved: (Long) -> Unit) {
        val id = value.toLongOrNull()
        if (id != null) {
            val transaction = financeRepository.getTransactionById(id)
            if (transaction != null) {
                onResolved(transaction.id)
            } else {
                _errorMessage.value = "Transaction not found: $value"
            }
            return
        }
        val results = financeRepository.searchTransactions(value)
        if (results.isEmpty()) {
            _errorMessage.value = "No transactions match: $value"
        } else {
            val transaction = results.maxByOrNull { it.updatedAt } ?: results.first()
            onResolved(transaction.id)
            if (results.size > 1) {
                _errorMessage.value = "Multiple transactions matched \"$value\". Opened most recent."
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        private const val ITEM_TYPE_NOTE = "note"
    }
}
