package com.lifepad.app.domain.finance

enum class InsightType {
    SPENDING_TREND, TOP_CATEGORY, SAVINGS_ACHIEVED,
    BUDGET_ALERT, BILL_DUE_SOON, NET_WORTH_CHANGE, GOAL_PROGRESS
}

enum class InsightSeverity { INFO, WARNING, POSITIVE }

data class FinancialInsight(
    val type: InsightType,
    val title: String,
    val body: String,
    val severity: InsightSeverity,
    val value: Double? = null
)
