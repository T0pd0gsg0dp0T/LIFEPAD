package com.lifepad.app.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.dao.BudgetWithSpendingRaw
import com.lifepad.app.data.local.entity.CategoryEntity
import com.lifepad.app.data.local.entity.TransactionEntity
import com.lifepad.app.data.repository.FinanceRepository
import java.util.Calendar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FinanceUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val netBalance: Double = 0.0,
    val budgetsWithSpending: List<BudgetWithSpendingRaw> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<TransactionEntity> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val monthStart: Long
    private val monthEnd: Long

    init {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        monthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        monthEnd = cal.timeInMillis
    }

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<FinanceUiState> = combine(
        financeRepository.getAllTransactions(),
        financeRepository.getAllCategories(),
        financeRepository.getNetBalance(),
        financeRepository.getBudgetsWithSpending(monthStart, monthEnd),
        _searchQuery
    ) { transactions, categories, netBalance, budgets, searchQuery ->
        FinanceUiState(
            transactions = transactions,
            categories = categories,
            netBalance = netBalance,
            budgetsWithSpending = budgets,
            searchQuery = searchQuery,
            isSearching = _isSearching.value,
            searchResults = _searchResults.value,
            errorMessage = _errorMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _isSearching.value = false
            _searchResults.value = emptyList()
        } else {
            _isSearching.value = true
            viewModelScope.launch {
                try {
                    _searchResults.value = financeRepository.searchTransactions(query)
                } catch (e: Exception) {
                    _errorMessage.value = "Search failed: ${e.message}"
                }
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _isSearching.value = false
        _searchResults.value = emptyList()
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            try {
                financeRepository.deleteTransaction(transactionId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete transaction: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
