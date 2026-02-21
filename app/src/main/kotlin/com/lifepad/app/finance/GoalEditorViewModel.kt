package com.lifepad.app.finance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.AccountEntity
import com.lifepad.app.data.local.entity.GoalEntity
import com.lifepad.app.data.local.entity.GoalType
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalEditorUiState(
    val goalId: Long? = null,
    val name: String = "",
    val type: GoalType = GoalType.SAVINGS,
    val targetAmount: String = "",
    val currentAmount: String = "0",
    val monthlyContribution: String = "0",
    val deadline: Long? = null,
    val accountId: Long? = null,
    val notes: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class GoalEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val goalRepository: GoalRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val goalId: Long? = savedStateHandle.get<Long>("goalId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(GoalEditorUiState(goalId = goalId))
    val uiState: StateFlow<GoalEditorUiState> = _uiState.asStateFlow()

    val accounts: StateFlow<List<AccountEntity>> = financeRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadGoal()
    }

    private fun loadGoal() {
        viewModelScope.launch {
            try {
                if (goalId != null) {
                    val goal = goalRepository.getGoalById(goalId)
                    if (goal != null) {
                        _uiState.update {
                            it.copy(
                                name = goal.name,
                                type = goal.type,
                                targetAmount = goal.targetAmount.toString(),
                                currentAmount = goal.currentAmount.toString(),
                                monthlyContribution = goal.monthlyContribution.toString(),
                                deadline = goal.deadline,
                                accountId = goal.accountId,
                                notes = goal.notes,
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
    fun onTypeChange(type: GoalType) { _uiState.update { it.copy(type = type) } }
    fun onTargetAmountChange(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(targetAmount = amount) }
        }
    }
    fun onCurrentAmountChange(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(currentAmount = amount) }
        }
    }
    fun onMonthlyContributionChange(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(monthlyContribution = amount) }
        }
    }
    fun onDeadlineChange(deadline: Long?) { _uiState.update { it.copy(deadline = deadline) } }
    fun onAccountSelected(id: Long?) { _uiState.update { it.copy(accountId = id) } }
    fun onNotesChange(notes: String) { _uiState.update { it.copy(notes = notes) } }

    suspend fun saveGoal(): Long? {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a name") }
            return null
        }
        val target = state.targetAmount.toDoubleOrNull()
        if (target == null || target <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid target amount") }
            return null
        }

        _uiState.update { it.copy(isSaving = true) }

        return try {
            val goal = GoalEntity(
                id = state.goalId ?: 0L,
                name = state.name,
                type = state.type,
                targetAmount = target,
                currentAmount = state.currentAmount.toDoubleOrNull() ?: 0.0,
                monthlyContribution = state.monthlyContribution.toDoubleOrNull() ?: 0.0,
                deadline = state.deadline,
                accountId = state.accountId,
                notes = state.notes
            )
            val savedId = goalRepository.saveGoal(goal)
            _uiState.update { it.copy(goalId = savedId, isSaving = false) }
            savedId
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isSaving = false, errorMessage = "Failed to save: ${e.message}")
            }
            null
        }
    }

    fun deleteGoal() {
        viewModelScope.launch {
            try {
                goalId?.let { goalRepository.deleteGoal(it) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
}
