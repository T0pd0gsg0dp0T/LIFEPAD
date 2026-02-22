package com.lifepad.app.finance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.CategoryEntity
import com.lifepad.app.data.local.entity.HashtagEntity
import com.lifepad.app.data.local.entity.TransactionEntity
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class TransactionDetailUiState(
    val transaction: TransactionEntity? = null,
    val category: CategoryEntity? = null,
    val hashtags: List<HashtagEntity> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository,
    private val noteRepository: NoteRepository,
    private val journalRepository: JournalRepository
) : ViewModel() {
    private val transactionId: Long = savedStateHandle.get<Long>("transactionId") ?: 0L

    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TransactionDetailUiState> = combine(
        financeRepository.observeTransaction(transactionId),
        financeRepository.getAllCategories(),
        financeRepository.getHashtagsForTransaction(transactionId),
        _errorMessage
    ) { transaction, categories, hashtags, errorMessage ->
        TransactionDetailUiState(
            transaction = transaction,
            category = categories.firstOrNull { it.id == transaction?.categoryId },
            hashtags = hashtags,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionDetailUiState()
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
                    val note = noteRepository.getNoteByTitleIgnoreCase(target)
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
}
