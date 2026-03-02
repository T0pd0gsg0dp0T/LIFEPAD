package com.lifepad.app.finance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.CategoryEntity
import com.lifepad.app.data.local.entity.CategoryType
import com.lifepad.app.data.local.entity.HashtagEntity
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.local.entity.TransactionEntity
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.HashtagRepository
import com.lifepad.app.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionEditorUiState(
    val transactionId: Long? = null,
    val amount: String = "",
    val description: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long? = null,
    val transactionDate: Long = System.currentTimeMillis(),
    val linkedEntryId: Long? = null,
    val linkedEntryPreview: JournalEntryEntity? = null,
    val showEntryPicker: Boolean = false,
    val availableEntries: List<JournalEntryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hashtags: List<HashtagEntity> = emptyList(),
    val hashtagSuggestions: List<HashtagEntity> = emptyList(),
    val showHashtagSuggestions: Boolean = false,
    val showCategorySelector: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository,
    private val hashtagRepository: HashtagRepository,
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val transactionId: Long? = savedStateHandle.get<Long>("transactionId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(TransactionEditorUiState(transactionId = transactionId))
    val uiState: StateFlow<TransactionEditorUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = financeRepository.getActiveCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            try {
                if (transactionId != null) {
                    val transaction = financeRepository.getTransactionById(transactionId)
                    if (transaction != null) {
                        val linkedEntry = transaction.linkedEntryId?.let {
                            journalRepository.getEntryById(it)
                        }
                        _uiState.update {
                            it.copy(
                                amount = transaction.amount.toString(),
                                description = transaction.description,
                                type = transaction.type,
                                categoryId = transaction.categoryId,
                                transactionDate = transaction.transactionDate,
                                linkedEntryId = transaction.linkedEntryId,
                                linkedEntryPreview = linkedEntry,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load transaction: ${e.message}"
                    )
                }
            }
        }

        // Launch separate coroutine for flow collection to avoid blocking
        if (transactionId != null) {
            viewModelScope.launch {
                financeRepository.getHashtagsForTransaction(transactionId).collect { hashtags ->
                    _uiState.update { it.copy(hashtags = hashtags) }
                }
            }
        }
    }

    fun onAmountChange(amount: String) {
        // Only allow valid decimal numbers
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(amount = amount) }
        }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
        checkForHashtagSuggestions(description)
    }

    fun onTypeChange(type: TransactionType) {
        val currentCategory = _uiState.value.categoryId
        val expectedCategoryType = if (type == TransactionType.INCOME) {
            CategoryType.INCOME
        } else {
            CategoryType.EXPENSE
        }
        val validCategory = categories.value.firstOrNull { it.id == currentCategory && it.type == expectedCategoryType }
        _uiState.update {
            it.copy(
                type = type,
                categoryId = validCategory?.id
            )
        }
    }

    fun onDateChange(dateMillis: Long) {
        val currentCal = java.util.Calendar.getInstance()
        currentCal.timeInMillis = _uiState.value.transactionDate
        val hour = currentCal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = currentCal.get(java.util.Calendar.MINUTE)
        val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        utcCal.timeInMillis = dateMillis
        currentCal.set(
            utcCal.get(java.util.Calendar.YEAR),
            utcCal.get(java.util.Calendar.MONTH),
            utcCal.get(java.util.Calendar.DAY_OF_MONTH),
            hour, minute, 0
        )
        currentCal.set(java.util.Calendar.MILLISECOND, 0)
        _uiState.update { it.copy(transactionDate = currentCal.timeInMillis) }
    }

    fun onTimeChange(hour: Int, minute: Int) {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = _uiState.value.transactionDate
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
        cal.set(java.util.Calendar.MINUTE, minute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        _uiState.update { it.copy(transactionDate = cal.timeInMillis) }
    }

    fun onCategorySelected(categoryId: Long?) {
        _uiState.update {
            it.copy(
                categoryId = categoryId,
                showCategorySelector = false
            )
        }
    }

    fun toggleCategorySelector() {
        _uiState.update { it.copy(showCategorySelector = !it.showCategorySelector) }
    }

    private fun checkForHashtagSuggestions(content: String) {
        viewModelScope.launch {
            val hashtagMatch = Regex("""#(\w*)$""").find(content)
            if (hashtagMatch != null) {
                val prefix = hashtagMatch.groupValues[1]
                if (prefix.isNotEmpty()) {
                    val suggestions = hashtagRepository.searchHashtags(prefix)
                    _uiState.update {
                        it.copy(
                            hashtagSuggestions = suggestions,
                            showHashtagSuggestions = suggestions.isNotEmpty()
                        )
                    }
                } else {
                    _uiState.update { it.copy(showHashtagSuggestions = false) }
                }
            } else {
                _uiState.update { it.copy(showHashtagSuggestions = false) }
            }
        }
    }

    fun insertHashtag(hashtag: HashtagEntity) {
        val currentDescription = _uiState.value.description
        val newDescription = currentDescription.replace(
            Regex("""#\w*$"""),
            "#${hashtag.name} "
        )
        _uiState.update {
            it.copy(
                description = newDescription,
                showHashtagSuggestions = false
            )
        }
    }

    fun dismissSuggestions() {
        _uiState.update { it.copy(showHashtagSuggestions = false) }
    }

    fun toggleEntryPicker() {
        val showing = !_uiState.value.showEntryPicker
        if (showing) {
            viewModelScope.launch {
                val entries = journalRepository.getRecentEntries()
                _uiState.update {
                    it.copy(
                        showEntryPicker = true,
                        availableEntries = entries
                    )
                }
            }
        } else {
            _uiState.update { it.copy(showEntryPicker = false) }
        }
    }

    fun onEntryLinked(entryId: Long) {
        viewModelScope.launch {
            val entry = journalRepository.getEntryById(entryId)
            _uiState.update {
                it.copy(
                    linkedEntryId = entryId,
                    linkedEntryPreview = entry,
                    showEntryPicker = false
                )
            }
        }
    }

    fun unlinkEntry() {
        _uiState.update {
            it.copy(
                linkedEntryId = null,
                linkedEntryPreview = null
            )
        }
    }

    suspend fun saveTransaction(): Long? {
        val state = _uiState.value
        val amountSaveRegex = Regex("^\\d+(\\.\\d{0,2})?$")
        if (!state.amount.matches(amountSaveRegex)) {
            _uiState.update { it.copy(errorMessage = "Invalid amount") }
            return null
        }
        val amount = state.amount.toDoubleOrNull()
        if (amount == null) {
            _uiState.update { it.copy(errorMessage = "Invalid amount") }
            return null
        }

        _uiState.update { it.copy(isSaving = true) }

        return try {
            val transaction = TransactionEntity(
                id = state.transactionId ?: 0L,
                amount = amount,
                type = state.type,
                categoryId = state.categoryId,
                description = state.description,
                transactionDate = state.transactionDate,
                linkedEntryId = state.linkedEntryId
            )

            val savedId = financeRepository.saveTransaction(transaction)
            _uiState.update {
                it.copy(
                    transactionId = savedId,
                    isSaving = false
                )
            }
            savedId
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    errorMessage = "Failed to save transaction: ${e.message}"
                )
            }
            null
        }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            try {
                transactionId?.let { financeRepository.deleteTransaction(it) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to delete transaction: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
