package com.lifepad.app.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.BudgetEntity
import com.lifepad.app.data.local.entity.CategoryEntity
import com.lifepad.app.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetListUiState(
    val budgets: List<BudgetEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class BudgetListViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BudgetListUiState> = combine(
        financeRepository.getAllBudgets(),
        financeRepository.getAllCategories()
    ) { budgets, categories ->
        BudgetListUiState(
            budgets = budgets,
            categories = categories,
            isLoading = false
        )
    }.combine(_errorMessage) { state, error ->
        state.copy(errorMessage = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetListUiState()
    )

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            try {
                financeRepository.deleteBudget(id)
            } catch (e: Exception) {
                _errorMessage.update { "Failed to delete: ${e.message}" }
            }
        }
    }

    fun clearError() {
        _errorMessage.update { null }
    }
}
