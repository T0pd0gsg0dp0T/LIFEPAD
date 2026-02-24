package com.lifepad.app.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.InsightRepository
import com.lifepad.app.domain.finance.FinancialInsight
import com.lifepad.app.domain.finance.InsightSeverity
import com.lifepad.app.domain.finance.InsightType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    }

    fun onPeriodChange(period: FinanceStatsPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
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

            val hasMonthlyData = monthlyComparison.any { it.income > 0f || it.expense > 0f }
            val shouldUseDemo = totalIncome == 0.0 &&
                totalExpenses == 0.0 &&
                categorySpending.isEmpty() &&
                spendingTrend.isEmpty() &&
                insights.isEmpty() &&
                !hasMonthlyData

            val resolved = if (shouldUseDemo) {
                buildDemoStats(period)
            } else {
                FinanceStatsUiState(
                    selectedPeriod = period,
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    categorySpending = categorySpending,
                    monthlyComparison = monthlyComparison,
                    spendingTrend = spendingTrend,
                    insights = insights,
                    isLoading = false
                )
            }

            _uiState.update {
                resolved.copy(
                    selectedPeriod = period,
                    isLoading = false
                )
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

    private fun buildDemoStats(period: FinanceStatsPeriod): FinanceStatsUiState {
        val monthLabels = buildDemoMonthLabels(period.months)
        val demoMonthly = monthLabels.mapIndexed { index, label ->
            val income = 3200f + (index * 180f)
            val expense = 2100f + (index * 140f)
            MonthlyComparison(
                label = label,
                income = income,
                expense = expense
            )
        }
        val demoCategorySpending = listOf(
            CategorySpending("Housing", 1450.0, 38f),
            CategorySpending("Food", 620.0, 16f),
            CategorySpending("Transport", 420.0, 11f),
            CategorySpending("Utilities", 310.0, 8f),
            CategorySpending("Entertainment", 260.0, 7f),
            CategorySpending("Other", 770.0, 20f)
        )
        val demoTrend = buildDemoTrend()
        val demoInsights = listOf(
            FinancialInsight(
                type = InsightType.SPENDING_TREND,
                title = "Spending cooled down",
                body = "Your weekly average dropped 12% compared to the previous period.",
                severity = InsightSeverity.POSITIVE,
                value = -12.0
            ),
            FinancialInsight(
                type = InsightType.TOP_CATEGORY,
                title = "Top category: Housing",
                body = "Housing is 38% of total spend this period.",
                severity = InsightSeverity.INFO,
                value = 38.0
            ),
            FinancialInsight(
                type = InsightType.SAVINGS_ACHIEVED,
                title = "Savings rate",
                body = "You saved about $850 this period.",
                severity = InsightSeverity.POSITIVE,
                value = 850.0
            )
        )
        return FinanceStatsUiState(
            selectedPeriod = period,
            totalIncome = 3650.0,
            totalExpenses = 2800.0,
            categorySpending = demoCategorySpending,
            monthlyComparison = demoMonthly,
            spendingTrend = demoTrend,
            insights = demoInsights,
            isLoading = false
        )
    }

    private fun buildDemoMonthLabels(months: Int): List<String> {
        val labels = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -(months - 1))
        val format = SimpleDateFormat("MMM", Locale.getDefault())
        repeat(months) {
            labels.add(format.format(cal.time))
            cal.add(Calendar.MONTH, 1)
        }
        return labels
    }

    private fun buildDemoTrend(): List<SpendingTrendPoint> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val format = SimpleDateFormat("M/d", Locale.getDefault())
        return (0 until 7).map { index ->
            val amount = 120f + (index * 35f) + if (index % 2 == 0) 40f else -10f
            val label = format.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            SpendingTrendPoint(
                dayIndex = index.toFloat(),
                amount = amount.coerceAtLeast(40f),
                label = label
            )
        }
    }
}
