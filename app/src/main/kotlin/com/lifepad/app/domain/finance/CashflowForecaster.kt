package com.lifepad.app.domain.finance

import com.lifepad.app.data.local.entity.BillFrequency
import com.lifepad.app.data.local.entity.RecurringBillEntity
import com.lifepad.app.data.local.entity.TransactionType
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class ForecastPoint(
    val dayIndex: Int,
    val date: Long,
    val balance: Double
)

object CashflowForecaster {

    fun forecast(
        currentBalance: Double,
        confirmedBills: List<RecurringBillEntity>,
        forecastDays: Int
    ): List<ForecastPoint> {
        val points = mutableListOf<ForecastPoint>()
        var runningBalance = currentBalance

        val cal = Calendar.getInstance()
        val startOfDay = cal.timeInMillis

        // Build a map of bill occurrences within the forecast window
        // Each bill may trigger multiple times (e.g., a weekly bill in a 30-day window)
        val billEvents = mutableListOf<Pair<Long, Double>>() // date, signed amount

        for (bill in confirmedBills) {
            val sign = if (bill.transactionType == TransactionType.INCOME) 1.0 else -1.0
            var nextDue = bill.nextDueDate

            // Generate all occurrences within forecast window
            val endDate = startOfDay + TimeUnit.DAYS.toMillis(forecastDays.toLong())
            while (nextDue <= endDate) {
                if (nextDue >= startOfDay) {
                    billEvents.add(nextDue to bill.amount * sign)
                }
                nextDue = advanceDate(nextDue, bill.frequency)
            }
        }

        // Sort events by date
        val eventsByDay = billEvents.groupBy { daysBetween(startOfDay, it.first) }

        for (day in 0..forecastDays) {
            val dayDate = startOfDay + TimeUnit.DAYS.toMillis(day.toLong())

            // Apply any bills due on this day
            eventsByDay[day]?.forEach { (_, amount) ->
                runningBalance += amount
            }

            points.add(
                ForecastPoint(
                    dayIndex = day,
                    date = dayDate,
                    balance = Math.round(runningBalance * 100.0) / 100.0
                )
            )
        }

        return points
    }

    private fun advanceDate(dateMs: Long, frequency: BillFrequency): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMs }
        when (frequency) {
            BillFrequency.WEEKLY -> cal.add(Calendar.DAY_OF_MONTH, 7)
            BillFrequency.BIWEEKLY -> cal.add(Calendar.DAY_OF_MONTH, 14)
            BillFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            BillFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun daysBetween(start: Long, end: Long): Int {
        return TimeUnit.MILLISECONDS.toDays(end - start).toInt()
    }
}
