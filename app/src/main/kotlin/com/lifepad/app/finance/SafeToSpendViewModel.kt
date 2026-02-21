package com.lifepad.app.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.RecurringBillEntity
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.GoalRepository
import com.lifepad.app.data.repository.RecurringBillRepository
import com.lifepad.app.domain.finance.CashflowForecaster
import com.lifepad.app.domain.finance.ForecastPoint
import com.lifepad.app.domain.finance.SafeToSpendCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SafeToSpendUiState(
    val dailyAllowance: Double = 0.0,
    val availableBuffer: Double = 0.0,
    val billsReserved: Double = 0.0,
    val goalsReserved: Double = 0.0,
    val daysRemaining: Int = 0,
    val upcomingBills: List<RecurringBillEntity> = emptyList(),
    val forecastPoints: List<ForecastPoint> = emptyList(),
    val forecastDays: Int = 14,
    val isLoading: Boolean = true
)

@HiltViewModel
class SafeToSpendViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val recurringBillRepository: RecurringBillRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SafeToSpendUiState())
    val uiState: StateFlow<SafeToSpendUiState> = _uiState.asStateFlow()

    // Private flows to combine (3-param combine — safe)
    private val _netBalance = MutableStateFlow(0.0)
    private val _confirmedBills = MutableStateFlow<List<RecurringBillEntity>>(emptyList())
    private val _goalContributions = MutableStateFlow(0.0)

    init {
        // Collect each flow in separate launches to avoid async race
        viewModelScope.launch {
            financeRepository.getNetBalance().collect { _netBalance.value = it }
        }
        viewModelScope.launch {
            recurringBillRepository.getConfirmedBills().collect { _confirmedBills.value = it }
        }
        viewModelScope.launch {
            goalRepository.getTotalMonthlyContributions().collect { _goalContributions.value = it }
        }

        // Combine the 3 scalar flows
        viewModelScope.launch {
            combine(_netBalance, _confirmedBills, _goalContributions) { balance, bills, goals ->
                Triple(balance, bills, goals)
            }.collect { (balance, bills, goals) ->
                computeSafeToSpend(balance, bills, goals)
            }
        }
    }

    fun onForecastDaysChange(days: Int) {
        _uiState.update { it.copy(forecastDays = days) }
        // Recompute forecast with current data
        computeSafeToSpend(_netBalance.value, _confirmedBills.value, _goalContributions.value)
    }

    private fun computeSafeToSpend(
        netBalance: Double,
        confirmedBills: List<RecurringBillEntity>,
        goalContributions: Double
    ) {
        // Get month-end bills for safe-to-spend calc
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val monthEnd = cal.timeInMillis

        val monthBills = confirmedBills.filter { it.nextDueDate <= monthEnd }

        val result = SafeToSpendCalculator.compute(
            currentNetBalance = netBalance,
            upcomingBills = monthBills,
            totalMonthlyGoalContributions = goalContributions
        )

        val forecastDays = _uiState.value.forecastDays
        val forecastPoints = CashflowForecaster.forecast(
            currentBalance = netBalance,
            confirmedBills = confirmedBills,
            forecastDays = forecastDays
        )

        _uiState.update {
            it.copy(
                dailyAllowance = result.dailyAllowance,
                availableBuffer = result.availableBuffer,
                billsReserved = result.billsReserved,
                goalsReserved = result.goalsReserved,
                daysRemaining = result.daysRemaining,
                upcomingBills = monthBills,
                forecastPoints = forecastPoints,
                isLoading = false
            )
        }
    }
}
