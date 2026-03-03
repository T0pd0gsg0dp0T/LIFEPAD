package com.lifepad.app.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.BudgetEntity
import com.lifepad.app.data.local.entity.CategoryEntity
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.domain.finance.BudgetTemplateEngine
import com.lifepad.app.domain.finance.BudgetTemplateType
import com.lifepad.app.domain.finance.TemplateBudgetItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetTemplateUiState(
    val monthlyIncome: String = "",
    val selectedTemplate: BudgetTemplateType? = null,
    val previewItems: List<TemplateBudgetItem> = emptyList(),
    val isApplying: Boolean = false,
    val applied: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class BudgetTemplateViewModel @Inject constructor(
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetTemplateUiState())
    val uiState: StateFlow<BudgetTemplateUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = financeRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            categories.collect { updatePreview() }
        }
    }

    fun onIncomeChange(income: String) {
        if (income.isEmpty() || income.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(monthlyIncome = income) }
            updatePreview()
        }
    }

    fun onTemplateSelected(template: BudgetTemplateType) {
        _uiState.update { it.copy(selectedTemplate = template) }
        updatePreview()
    }

    private fun updatePreview() {
        val state = _uiState.value
        val income = state.monthlyIncome.toDoubleOrNull() ?: return
        val template = state.selectedTemplate ?: return

        val items = BudgetTemplateEngine.apply(template, income, categories.value)
        _uiState.update { it.copy(previewItems = items) }
    }

    fun applyTemplate() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.previewItems.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Please select a template and enter income") }
                return@launch
            }

            _uiState.update { it.copy(isApplying = true) }

            try {
                state.previewItems.forEach { item ->
                    val budget = BudgetEntity(
                        categoryId = item.categoryId,
                        limitAmount = item.amount,
                        period = item.period
                    )
                    financeRepository.saveBudget(budget)
                }
                _uiState.update { it.copy(isApplying = false, applied = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isApplying = false, errorMessage = "Failed to apply: ${e.message}")
                }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
}
