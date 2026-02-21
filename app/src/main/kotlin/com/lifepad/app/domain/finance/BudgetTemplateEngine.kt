package com.lifepad.app.domain.finance

import com.lifepad.app.data.local.entity.CategoryEntity

enum class BudgetTemplateType(val label: String, val description: String) {
    FIFTY_THIRTY_TWENTY(
        "50/30/20 Rule",
        "50% needs, 30% wants, 20% savings"
    ),
    ZERO_BASED(
        "Zero-Based",
        "Allocate 100% of income across categories"
    ),
    PAY_YOURSELF_FIRST(
        "Pay Yourself First",
        "Save 20% first, then budget the rest"
    )
}

data class TemplateBudgetItem(
    val categoryName: String,
    val categoryId: Long,
    val amount: Double,
    val period: String = "monthly"
)

object BudgetTemplateEngine {

    // Category names considered "needs"
    private val needsCategories = setOf(
        "Food", "Rent", "Utilities", "Transport", "Healthcare", "Phone", "WiFi"
    )

    // Category names considered "wants"
    private val wantsCategories = setOf(
        "Entertainment", "Shopping", "Subscriptions", "AI", "Other"
    )

    // Category names considered "savings"
    private val savingsCategories = setOf("Savings")

    fun apply(
        template: BudgetTemplateType,
        monthlyIncome: Double,
        categories: List<CategoryEntity>
    ): List<TemplateBudgetItem> {
        return when (template) {
            BudgetTemplateType.FIFTY_THIRTY_TWENTY -> applyFiftyThirtyTwenty(monthlyIncome, categories)
            BudgetTemplateType.ZERO_BASED -> applyZeroBased(monthlyIncome, categories)
            BudgetTemplateType.PAY_YOURSELF_FIRST -> applyPayYourselfFirst(monthlyIncome, categories)
        }
    }

    fun applyZeroBasedCustom(
        monthlyIncome: Double,
        allocations: Map<Long, Double>, // categoryId to percentage (0-100)
        categories: List<CategoryEntity>
    ): List<TemplateBudgetItem> {
        return allocations.mapNotNull { (categoryId, pct) ->
            val category = categories.find { it.id == categoryId } ?: return@mapNotNull null
            if (pct <= 0) return@mapNotNull null
            TemplateBudgetItem(
                categoryName = category.name,
                categoryId = category.id,
                amount = roundCents(monthlyIncome * pct / 100.0),
                period = "monthly"
            )
        }
    }

    private fun applyFiftyThirtyTwenty(
        monthlyIncome: Double,
        categories: List<CategoryEntity>
    ): List<TemplateBudgetItem> {
        val items = mutableListOf<TemplateBudgetItem>()

        val needs = categories.filter { it.name in needsCategories }
        val wants = categories.filter { it.name in wantsCategories }
        val savings = categories.filter { it.name in savingsCategories }

        val needsBudget = monthlyIncome * 0.50
        val wantsBudget = monthlyIncome * 0.30
        val savingsBudget = monthlyIncome * 0.20

        // Split each pool equally among matching categories
        if (needs.isNotEmpty()) {
            val perCategory = needsBudget / needs.size
            needs.forEach { cat ->
                items.add(TemplateBudgetItem(cat.name, cat.id, roundCents(perCategory)))
            }
        }

        if (wants.isNotEmpty()) {
            val perCategory = wantsBudget / wants.size
            wants.forEach { cat ->
                items.add(TemplateBudgetItem(cat.name, cat.id, roundCents(perCategory)))
            }
        }

        if (savings.isNotEmpty()) {
            val perCategory = savingsBudget / savings.size
            savings.forEach { cat ->
                items.add(TemplateBudgetItem(cat.name, cat.id, roundCents(perCategory)))
            }
        }

        return items
    }

    private fun applyZeroBased(
        monthlyIncome: Double,
        categories: List<CategoryEntity>
    ): List<TemplateBudgetItem> {
        // Default: equal distribution across all categories
        if (categories.isEmpty()) return emptyList()
        val perCategory = monthlyIncome / categories.size
        return categories.map { cat ->
            TemplateBudgetItem(cat.name, cat.id, roundCents(perCategory))
        }
    }

    private fun applyPayYourselfFirst(
        monthlyIncome: Double,
        categories: List<CategoryEntity>
    ): List<TemplateBudgetItem> {
        val items = mutableListOf<TemplateBudgetItem>()

        val savingsAmount = monthlyIncome * 0.20
        val remainder = monthlyIncome * 0.80

        // Savings category gets 20%
        val savings = categories.filter { it.name in savingsCategories }
        if (savings.isNotEmpty()) {
            val perSavings = savingsAmount / savings.size
            savings.forEach { cat ->
                items.add(TemplateBudgetItem(cat.name, cat.id, roundCents(perSavings)))
            }
        }

        // Everything else splits the remaining 80%
        val expenseCategories = categories.filter { it.name !in savingsCategories }
        if (expenseCategories.isNotEmpty()) {
            val perCategory = remainder / expenseCategories.size
            expenseCategories.forEach { cat ->
                items.add(TemplateBudgetItem(cat.name, cat.id, roundCents(perCategory)))
            }
        }

        return items
    }

    private fun roundCents(value: Double): Double {
        return Math.round(value * 100.0) / 100.0
    }
}
