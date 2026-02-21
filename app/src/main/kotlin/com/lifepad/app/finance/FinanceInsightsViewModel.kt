package com.lifepad.app.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.repository.InsightRepository
import com.lifepad.app.domain.finance.FinancialInsight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val insights: List<FinancialInsight> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FinanceInsightsViewModel @Inject constructor(
    private val insightRepository: InsightRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    fun loadInsights() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val insights = insightRepository.generateInsights()
                _uiState.update { it.copy(insights = insights, isLoading = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
