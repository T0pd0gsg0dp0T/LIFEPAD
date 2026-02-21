package com.lifepad.app.finance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.BudgetEntity
import com.lifepad.app.data.local.entity.CategoryEntity
import com.lifepad.app.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetEditorUiState(
    val budgetId: Long? = null,
    val categoryId: Long? = null,
    val limitAmount: String = "",
    val period: String = "monthly",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val showCategorySelector: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class BudgetEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val budgetId: Long? = savedStateHandle.get<Long>("budgetId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(BudgetEditorUiState(budgetId = budgetId))
    val uiState: StateFlow<BudgetEditorUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = financeRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadBudget()
    }

    private fun loadBudget() {
        viewModelScope.launch {
            try {
                if (budgetId != null) {
                    val budget = financeRepository.getBudgetById(budgetId)
                    if (budget != null) {
                        _uiState.update {
                            it.copy(
                                categoryId = budget.categoryId,
                                limitAmount = budget.limitAmount.toString(),
                                period = budget.period,
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
                        errorMessage = "Failed to load budget: ${e.message}"
                    )
                }
            }
        }
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

    fun onLimitAmountChange(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(limitAmount = amount) }
        }
    }

    fun onPeriodChange(period: String) {
        _uiState.update { it.copy(period = period) }
    }

    suspend fun saveBudget(): Long? {
        val state = _uiState.value
        if (state.categoryId == null) {
            _uiState.update { it.copy(errorMessage = "Please select a category") }
            return null
        }
        val amount = state.limitAmount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
            return null
        }

        _uiState.update { it.copy(isSaving = true) }

        return try {
            val budget = BudgetEntity(
                id = state.budgetId ?: 0L,
                categoryId = state.categoryId,
                limitAmount = amount,
                period = state.period
            )
            val savedId = financeRepository.saveBudget(budget)
            _uiState.update { it.copy(budgetId = savedId, isSaving = false) }
            savedId
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    errorMessage = "Failed to save budget: ${e.message}"
                )
            }
            null
        }
    }

    fun deleteBudget() {
        viewModelScope.launch {
            try {
                budgetId?.let { financeRepository.deleteBudget(it) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to delete budget: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
