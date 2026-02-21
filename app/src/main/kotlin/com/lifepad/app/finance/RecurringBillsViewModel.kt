package com.lifepad.app.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.dao.RecurringBillWithCategory
import com.lifepad.app.data.local.entity.BillFrequency
import com.lifepad.app.data.local.entity.RecurringBillEntity
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.RecurringBillRepository
import com.lifepad.app.domain.finance.DetectedBill
import com.lifepad.app.domain.finance.RecurringBillDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringBillsUiState(
    val bills: List<RecurringBillWithCategory> = emptyList(),
    val detectedCandidates: List<DetectedBill> = emptyList(),
    val isDetecting: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class RecurringBillsViewModel @Inject constructor(
    private val recurringBillRepository: RecurringBillRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringBillsUiState())
    val uiState: StateFlow<RecurringBillsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recurringBillRepository.getBillsWithCategories().collect { bills ->
                _uiState.update { it.copy(bills = bills, isLoading = false) }
            }
        }
    }

    fun detectRecurring() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetecting = true) }
            try {
                // Use first() to get a single snapshot — collect() would re-run detection
                // on every DB change and never terminate the coroutine.
                val transactions = financeRepository.getAllTransactions().first()
                val candidates = RecurringBillDetector.detect(transactions)
                val existingNames = _uiState.value.bills.map { it.name.lowercase() }.toSet()
                val newCandidates = candidates.filter { it.name.lowercase() !in existingNames }
                _uiState.update { it.copy(detectedCandidates = newCandidates, isDetecting = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isDetecting = false, errorMessage = "Detection failed: ${e.message}")
                }
            }
        }
    }

    fun confirmBill(detected: DetectedBill) {
        viewModelScope.launch {
            try {
                val bill = RecurringBillEntity(
                    name = detected.name,
                    amount = detected.amount,
                    transactionType = TransactionType.EXPENSE,
                    categoryId = detected.categoryId,
                    frequency = detected.frequency,
                    nextDueDate = detected.nextDueDate,
                    isConfirmed = true,
                    isEnabled = true,
                    detectedFromCount = detected.detectedFromCount
                )
                recurringBillRepository.confirmBill(bill)
                _uiState.update { state ->
                    state.copy(detectedCandidates = state.detectedCandidates.filter { it.name != detected.name })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to confirm bill: ${e.message}") }
            }
        }
    }

    fun dismissCandidate(detected: DetectedBill) {
        _uiState.update { state ->
            state.copy(detectedCandidates = state.detectedCandidates.filter { it.name != detected.name })
        }
    }

    fun deleteBill(id: Long) {
        viewModelScope.launch {
            try {
                recurringBillRepository.deleteBill(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete bill: ${e.message}") }
            }
        }
    }

    fun toggleEnabled(id: Long) {
        viewModelScope.launch {
            try {
                recurringBillRepository.toggleEnabled(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to toggle bill: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
