package com.lifepad.app.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.InsightRepository
import com.lifepad.app.domain.finance.FinancialInsight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class FinanceStatsPeriod(val months: Int, val label: String) {
    ONE_MONTH(1, "1M"),
    THREE_MONTHS(3, "3M"),
    SIX_MONTHS(6, "6M"),
    ONE_YEAR(12, "1Y")
}

data class CategorySpending(
    val categoryName: String,
    val amount: Double,
    val percentage: Float
)

data class MonthlyComparison(
    val label: String,
    val income: Float,
    val expense: Float
)

data class SpendingTrendPoint(
    val dayIndex: Float,
    val amount: Float,
    val label: String
)

data class FinanceStatsUiState(
    val selectedPeriod: FinanceStatsPeriod = FinanceStatsPeriod.THREE_MONTHS,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val categorySpending: List<CategorySpending> = emptyList(),
    val monthlyComparison: List<MonthlyComparison> = emptyList(),
    val spendingTrend: List<SpendingTrendPoint> = emptyList(),
    val insights: List<FinancialInsight> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FinanceStatsViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val insightRepository: InsightRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceStatsUiState())
    val uiState: StateFlow<FinanceStatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
        observeTransactions()
    }

    fun onPeriodChange(period: FinanceStatsPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadStats()
    }

    fun refresh() {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val period = _uiState.value.selectedPeriod
            val cal = Calendar.getInstance()
            val endDate = cal.timeInMillis
            cal.add(Calendar.MONTH, -period.months)
            val startDate = cal.timeInMillis

            // Totals
            val totalIncome = financeRepository.getTotalByType(TransactionType.INCOME, startDate, endDate)
            val totalExpenses = financeRepository.getTotalByType(TransactionType.EXPENSE, startDate, endDate)

            // Spending by category
            val categoryRows = financeRepository.getSpendingByCategory(startDate, endDate)
            val totalSpending = categoryRows.sumOf { it.total }
            val categorySpending = if (totalSpending > 0) {
                categoryRows.map {
                    CategorySpending(
                        categoryName = it.categoryName,
                        amount = it.total,
                        percentage = (it.total / totalSpending * 100).toFloat()
                    )
                }
            } else {
                emptyList()
            }

            // Monthly income vs expense comparison
            val monthlyComparison = buildMonthlyComparison(startDate, endDate, period.months)

            // Daily spending trend
            val dailyRows = financeRepository.getDailySpending(startDate, endDate)
            val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
            val spendingTrend = dailyRows.mapIndexed { index, row ->
                SpendingTrendPoint(
                    dayIndex = index.toFloat(),
                    amount = row.total.toFloat(),
                    label = dateFormat.format(Date(row.day))
                )
            }

            // Insights
            val insights = try {
                insightRepository.generateInsights()
            } catch (_: Exception) {
                emptyList()
            }

            _uiState.update {
                it.copy(
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    categorySpending = categorySpending,
                    monthlyComparison = monthlyComparison,
                    spendingTrend = spendingTrend,
                    insights = insights,
                    isLoading = false
                )
            }
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            financeRepository.getAllTransactions()
                .map { transactions ->
                    val lastUpdated = transactions.maxOfOrNull { it.updatedAt } ?: 0L
                    lastUpdated to transactions.size
                }
                .distinctUntilChanged()
                .debounce(300)
                .collectLatest {
                    loadStats()
                }
        }
    }

    private suspend fun buildMonthlyComparison(startDate: Long, endDate: Long, months: Int): List<MonthlyComparison> {
        val result = mutableListOf<MonthlyComparison>()
        val cal = Calendar.getInstance()
        cal.timeInMillis = startDate
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val labelFormat = SimpleDateFormat("MMM", Locale.getDefault())

        for (i in 0 until months) {
            val monthStart = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val monthEnd = cal.timeInMillis

            if (monthStart > endDate) break

            val income = financeRepository.getTotalByType(TransactionType.INCOME, monthStart, monthEnd)
            val expense = financeRepository.getTotalByType(TransactionType.EXPENSE, monthStart, monthEnd)

            result.add(
                MonthlyComparison(
                    label = labelFormat.format(Date(monthStart)),
                    income = income.toFloat(),
                    expense = expense.toFloat()
                )
            )
        }
        return result
    }

}
