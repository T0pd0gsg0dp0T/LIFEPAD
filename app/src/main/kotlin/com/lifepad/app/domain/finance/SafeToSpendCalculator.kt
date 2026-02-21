package com.lifepad.app.domain.finance

import com.lifepad.app.data.local.entity.RecurringBillEntity
import java.util.Calendar

data class SafeToSpendResult(
    val availableBuffer: Double,
    val dailyAllowance: Double,
    val daysRemaining: Int,
    val billsReserved: Double,
    val goalsReserved: Double
)

object SafeToSpendCalculator {

    fun compute(
        currentNetBalance: Double,
        upcomingBills: List<RecurringBillEntity>,
        totalMonthlyGoalContributions: Double
    ): SafeToSpendResult {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // Days remaining in month (including today)
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysRemaining = lastDay - today + 1

        // Sum of bills still due this month (only future ones)
        val billsReserved = upcomingBills
            .filter { it.nextDueDate > now }
            .sumOf { it.amount }

        val reserved = billsReserved + totalMonthlyGoalContributions
        val availableBuffer = currentNetBalance - reserved

        val dailyAllowance = if (availableBuffer <= 0 || daysRemaining <= 0) {
            0.0
        } else {
            Math.round(availableBuffer / daysRemaining * 100.0) / 100.0
        }

        return SafeToSpendResult(
            availableBuffer = Math.round(availableBuffer * 100.0) / 100.0,
            dailyAllowance = dailyAllowance,
            daysRemaining = daysRemaining,
            billsReserved = Math.round(billsReserved * 100.0) / 100.0,
            goalsReserved = Math.round(totalMonthlyGoalContributions * 100.0) / 100.0
        )
    }
}
