package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.BudgetDao
import com.lifepad.app.data.local.dao.GoalDao
import com.lifepad.app.data.local.dao.RecurringBillDao
import com.lifepad.app.data.local.dao.TransactionDao
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.domain.finance.CategorySpendingData
import com.lifepad.app.domain.finance.FinancialInsight
import com.lifepad.app.domain.finance.InsightGenerator
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val recurringBillDao: RecurringBillDao,
    private val goalDao: GoalDao
) {
    suspend fun generateInsights(): List<FinancialInsight> {
        val cal = Calendar.getInstance()

        // This month range
        // First set calendar to last day of month at 23:59:59.999 to get thisMonthEnd
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val thisMonthEnd = cal.timeInMillis
        // Then reset to first day of month at 00:00:00.000 for thisMonthStart
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val thisMonthStart = cal.timeInMillis

        // Last month range
        cal.add(Calendar.MONTH, -1)
        val lastMonthStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val lastMonthEnd = cal.timeInMillis

        val thisMonthIncome = transactionDao.getTotalByType(TransactionType.INCOME, thisMonthStart, thisMonthEnd)
        val thisMonthExpenses = transactionDao.getTotalByType(TransactionType.EXPENSE, thisMonthStart, thisMonthEnd)
        val lastMonthIncome = transactionDao.getTotalByType(TransactionType.INCOME, lastMonthStart, lastMonthEnd)
        val lastMonthExpenses = transactionDao.getTotalByType(TransactionType.EXPENSE, lastMonthStart, lastMonthEnd)

        val categorySpending = transactionDao.getSpendingByCategory(thisMonthStart, thisMonthEnd)
            .map { CategorySpendingData(it.categoryName, it.total) }

        val budgetsWithSpending = budgetDao.getBudgetsWithSpending(thisMonthStart, thisMonthEnd)
            .firstOrNull() ?: emptyList()

        val upcomingBills = recurringBillDao.getConfirmedBills()
            .firstOrNull() ?: emptyList()

        val activeGoals = goalDao.getActiveGoals()
            .firstOrNull() ?: emptyList()

        return InsightGenerator.generate(
            thisMonthIncome = thisMonthIncome,
            thisMonthExpenses = thisMonthExpenses,
            lastMonthIncome = lastMonthIncome,
            lastMonthExpenses = lastMonthExpenses,
            categorySpending = categorySpending,
            budgetsWithSpending = budgetsWithSpending,
            upcomingBills = upcomingBills,
            activeGoals = activeGoals
        )
    }
}
