package com.lifepad.app.finance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.AccountEntity
import com.lifepad.app.data.local.entity.BillFrequency
import com.lifepad.app.data.local.entity.CategoryEntity
import com.lifepad.app.data.local.entity.RecurringBillEntity
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.RecurringBillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillEditorUiState(
    val billId: Long? = null,
    val name: String = "",
    val amount: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val frequency: BillFrequency = BillFrequency.MONTHLY,
    val nextDueDate: Long = System.currentTimeMillis(),
    val enableReminder: Boolean = true,
    val notes: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val showCategorySelector: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class RecurringBillEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recurringBillRepository: RecurringBillRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val billId: Long? = savedStateHandle.get<Long>("billId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(BillEditorUiState(billId = billId))
    val uiState: StateFlow<BillEditorUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = financeRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = financeRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadBill()
    }

    private fun loadBill() {
        viewModelScope.launch {
            try {
                if (billId != null) {
                    val bill = recurringBillRepository.getBillById(billId)
                    if (bill != null) {
                        _uiState.update {
                            it.copy(
                                name = bill.name,
                                amount = bill.amount.toString(),
                                transactionType = bill.transactionType,
                                categoryId = bill.categoryId,
                                accountId = bill.accountId,
                                frequency = bill.frequency,
                                nextDueDate = bill.nextDueDate,
                                enableReminder = bill.reminderId != null,
                                notes = bill.notes,
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
                    it.copy(isLoading = false, errorMessage = "Failed to load: ${e.message}")
                }
            }
        }
    }

    fun onNameChange(name: String) { _uiState.update { it.copy(name = name) } }
    fun onAmountChange(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(amount = amount) }
        }
    }
    fun onTypeChange(type: TransactionType) { _uiState.update { it.copy(transactionType = type) } }
    fun onCategorySelected(id: Long?) { _uiState.update { it.copy(categoryId = id, showCategorySelector = false) } }
    fun onAccountSelected(id: Long?) { _uiState.update { it.copy(accountId = id) } }
    fun onFrequencyChange(freq: BillFrequency) { _uiState.update { it.copy(frequency = freq) } }
    fun onDateChange(date: Long) { _uiState.update { it.copy(nextDueDate = date) } }
    fun onReminderToggle(enabled: Boolean) { _uiState.update { it.copy(enableReminder = enabled) } }
    fun onNotesChange(notes: String) { _uiState.update { it.copy(notes = notes) } }
    fun toggleCategorySelector() { _uiState.update { it.copy(showCategorySelector = !it.showCategorySelector) } }

    suspend fun saveBill(): Long? {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a name") }
            return null
        }
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
            return null
        }

        _uiState.update { it.copy(isSaving = true) }

        return try {
            val bill = RecurringBillEntity(
                id = state.billId ?: 0L,
                name = state.name,
                amount = amount,
                transactionType = state.transactionType,
                categoryId = state.categoryId,
                accountId = state.accountId,
                frequency = state.frequency,
                nextDueDate = state.nextDueDate,
                isConfirmed = true,
                isEnabled = true,
                notes = state.notes
            )
            val savedId = recurringBillRepository.saveBill(bill)
            _uiState.update { it.copy(billId = savedId, isSaving = false) }
            savedId
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isSaving = false, errorMessage = "Failed to save: ${e.message}")
            }
            null
        }
    }

    fun deleteBill() {
        viewModelScope.launch {
            try {
                billId?.let { recurringBillRepository.deleteBill(it) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
}
