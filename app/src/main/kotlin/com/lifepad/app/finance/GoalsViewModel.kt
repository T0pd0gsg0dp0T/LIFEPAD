package com.lifepad.app.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.GoalEntity
import com.lifepad.app.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val goals: List<GoalEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            goalRepository.getAllGoals().collect { goals ->
                _uiState.update { it.copy(goals = goals, isLoading = false) }
            }
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            try {
                goalRepository.deleteGoal(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete goal: ${e.message}") }
            }
        }
    }

    fun updateProgress(goalId: Long, newAmount: Double) {
        viewModelScope.launch {
            try {
                goalRepository.updateProgress(goalId, newAmount)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
