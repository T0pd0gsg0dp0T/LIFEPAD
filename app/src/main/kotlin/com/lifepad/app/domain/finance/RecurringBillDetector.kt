package com.lifepad.app.domain.finance

import com.lifepad.app.data.local.entity.BillFrequency
import com.lifepad.app.data.local.entity.TransactionEntity
import com.lifepad.app.data.local.entity.TransactionType
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sqrt

data class DetectedBill(
    val name: String,
    val amount: Double,
    val frequency: BillFrequency,
    val nextDueDate: Long,
    val detectedFromCount: Int,
    val categoryId: Long?
)

object RecurringBillDetector {

    fun detect(transactions: List<TransactionEntity>): List<DetectedBill> {
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }

        // Group by normalized description
        val groups = expenses.groupBy { normalizeDescription(it.description) }

        return groups.mapNotNull { (key, txns) ->
            if (txns.size < 2) return@mapNotNull null
            detectFromGroup(key, txns)
        }.sortedByDescending { it.detectedFromCount }
    }

    private fun detectFromGroup(name: String, transactions: List<TransactionEntity>): DetectedBill? {
        val sorted = transactions.sortedBy { it.transactionDate }

        // Compute pairwise intervals in days
        val intervals = sorted.zipWithNext { a, b ->
            TimeUnit.MILLISECONDS.toDays(b.transactionDate - a.transactionDate).toDouble()
        }
        if (intervals.isEmpty()) return null

        val medianInterval = median(intervals)

        // Determine frequency from median interval
        val frequency = when {
            medianInterval in 6.0..8.0 -> BillFrequency.WEEKLY
            medianInterval in 13.0..16.0 -> BillFrequency.BIWEEKLY
            medianInterval in 25.0..35.0 -> BillFrequency.MONTHLY
            medianInterval in 350.0..380.0 -> BillFrequency.YEARLY
            else -> return null
        }

        // Check amount consistency (coefficient of variation < 10%)
        val amounts = sorted.map { it.amount }
        val mean = amounts.average()
        if (mean <= 0) return null
        val stddev = sqrt(amounts.map { (it - mean) * (it - mean) }.average())
        val cv = stddev / mean
        if (cv > 0.15) return null

        // Compute next due date
        val lastDate = sorted.last().transactionDate
        val intervalMs = TimeUnit.DAYS.toMillis(medianInterval.toLong())
        val nextDueDate = lastDate + intervalMs

        // Use the most common categoryId
        val categoryId = sorted.mapNotNull { it.categoryId }
            .groupBy { it }
            .maxByOrNull { it.value.size }
            ?.key

        return DetectedBill(
            name = name,
            amount = Math.round(mean * 100.0) / 100.0,
            frequency = frequency,
            nextDueDate = nextDueDate,
            detectedFromCount = sorted.size,
            categoryId = categoryId
        )
    }

    private fun normalizeDescription(description: String): String {
        // Strip hashtags, punctuation, and collapse whitespace
        return description
            .replace(Regex("#\\w+"), "")
            .replace(Regex("[^\\w\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }
}
