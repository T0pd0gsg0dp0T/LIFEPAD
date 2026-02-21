package com.lifepad.app.domain.finance

import com.lifepad.app.data.local.dao.BudgetWithSpendingRaw
import com.lifepad.app.data.local.entity.GoalEntity
import com.lifepad.app.data.local.entity.RecurringBillEntity
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class CategorySpendingData(
    val categoryName: String,
    val amount: Double
)

object InsightGenerator {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        currency = Currency.getInstance("USD")
    }

    fun generate(
        thisMonthIncome: Double,
        thisMonthExpenses: Double,
        lastMonthIncome: Double,
        lastMonthExpenses: Double,
        categorySpending: List<CategorySpendingData>,
        budgetsWithSpending: List<BudgetWithSpendingRaw>,
        upcomingBills: List<RecurringBillEntity>,
        activeGoals: List<GoalEntity>
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()

        // 1. Budget alerts (highest priority)
        budgetsWithSpending.forEach { budget ->
            if (budget.limitAmount > 0) {
                val pct = budget.spent / budget.limitAmount
                when {
                    pct >= 1.0 -> insights.add(
                        FinancialInsight(
                            type = InsightType.BUDGET_ALERT,
                            title = "Over Budget: ${budget.categoryName}",
                            body = "You've exceeded your ${formatCurrency(budget.limitAmount)} limit by ${formatCurrency(budget.spent - budget.limitAmount)}",
                            severity = InsightSeverity.WARNING,
                            value = pct
                        )
                    )
                    pct >= 0.85 -> insights.add(
                        FinancialInsight(
                            type = InsightType.BUDGET_ALERT,
                            title = "Budget Alert: ${budget.categoryName}",
                            body = "You're at ${(pct * 100).toInt()}% of your ${formatCurrency(budget.limitAmount)} limit",
                            severity = InsightSeverity.WARNING,
                            value = pct
                        )
                    )
                }
            }
        }

        // 2. Bills due soon (within 3 days)
        val now = System.currentTimeMillis()
        val threeDaysMs = TimeUnit.DAYS.toMillis(3)
        upcomingBills
            .filter { it.nextDueDate in now..(now + threeDaysMs) }
            .forEach { bill ->
                val daysUntil = TimeUnit.MILLISECONDS.toDays(bill.nextDueDate - now).toInt()
                val dayLabel = when (daysUntil) {
                    0 -> "today"
                    1 -> "tomorrow"
                    else -> "in $daysUntil days"
                }
                insights.add(
                    FinancialInsight(
                        type = InsightType.BILL_DUE_SOON,
                        title = "Bill Due: ${bill.name}",
                        body = "${formatCurrency(bill.amount)} due $dayLabel",
                        severity = InsightSeverity.WARNING,
                        value = bill.amount
                    )
                )
            }

        // 3. Spending trend vs last month
        if (lastMonthExpenses > 0) {
            val changePercent = ((thisMonthExpenses - lastMonthExpenses) / lastMonthExpenses) * 100
            when {
                changePercent > 10 -> insights.add(
                    FinancialInsight(
                        type = InsightType.SPENDING_TREND,
                        title = "Spending Up",
                        body = "You've spent ${abs(changePercent).toInt()}% more than last month",
                        severity = InsightSeverity.WARNING,
                        value = changePercent
                    )
                )
                changePercent < -10 -> insights.add(
                    FinancialInsight(
                        type = InsightType.SPENDING_TREND,
                        title = "Spending Down",
                        body = "You've spent ${abs(changePercent).toInt()}% less than last month",
                        severity = InsightSeverity.POSITIVE,
                        value = changePercent
                    )
                )
            }
        }

        // 4. Top spending category
        val topCategory = categorySpending.maxByOrNull { it.amount }
        if (topCategory != null && topCategory.amount > 0) {
            insights.add(
                FinancialInsight(
                    type = InsightType.TOP_CATEGORY,
                    title = "Top Category",
                    body = "${topCategory.categoryName} at ${formatCurrency(topCategory.amount)} this month",
                    severity = InsightSeverity.INFO,
                    value = topCategory.amount
                )
            )
        }

        // 5. Savings achieved
        val savings = thisMonthIncome - thisMonthExpenses
        if (savings > 0) {
            insights.add(
                FinancialInsight(
                    type = InsightType.SAVINGS_ACHIEVED,
                    title = "Savings This Month",
                    body = "You've saved ${formatCurrency(savings)} so far",
                    severity = InsightSeverity.POSITIVE,
                    value = savings
                )
            )
        }

        // 6. Goal progress
        activeGoals.forEach { goal ->
            if (goal.targetAmount > 0) {
                val progress = goal.currentAmount / goal.targetAmount
                when {
                    progress >= 0.9 -> insights.add(
                        FinancialInsight(
                            type = InsightType.GOAL_PROGRESS,
                            title = "Almost There: ${goal.name}",
                            body = "${(progress * 100).toInt()}% complete — ${formatCurrency(goal.targetAmount - goal.currentAmount)} to go",
                            severity = InsightSeverity.POSITIVE,
                            value = progress
                        )
                    )
                    progress >= 0.5 -> insights.add(
                        FinancialInsight(
                            type = InsightType.GOAL_PROGRESS,
                            title = "Halfway: ${goal.name}",
                            body = "${(progress * 100).toInt()}% of ${formatCurrency(goal.targetAmount)} reached",
                            severity = InsightSeverity.INFO,
                            value = progress
                        )
                    )
                }
            }
        }

        return insights
    }

    private fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }
}
