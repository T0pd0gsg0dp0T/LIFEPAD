package com.lifepad.app.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Notepad : Screen("notepad")
    data object NoteEditor : Screen("notepad/edit?noteId={noteId}&checklist={checklist}&folderId={folderId}") {
        fun createRoute(noteId: Long? = null, checklist: Boolean = false, folderId: Long? = null) =
            "notepad/edit?noteId=${noteId ?: 0}&checklist=$checklist&folderId=${folderId ?: 0}"
    }
    data object NoteDetail : Screen("notepad/detail?noteId={noteId}") {
        fun createRoute(noteId: Long) = "notepad/detail?noteId=$noteId"
    }
    data object Journal : Screen("journal")
    data object JournalEditor : Screen("journal/edit?entryId={entryId}&template={template}") {
        fun createRoute(entryId: Long? = null, template: String? = null): String {
            val safeTemplate = Uri.encode(template ?: "free")
            return "journal/edit?entryId=${entryId ?: 0}&template=$safeTemplate"
        }
    }
    data object JournalDetail : Screen("journal/detail?entryId={entryId}") {
        fun createRoute(entryId: Long) = "journal/detail?entryId=$entryId"
    }
    data object Finance : Screen("finance")
    data object TransactionEditor : Screen("finance/edit?transactionId={transactionId}") {
        fun createRoute(transactionId: Long? = null) = "finance/edit?transactionId=${transactionId ?: 0}"
    }
    data object TransactionDetail : Screen("finance/detail?transactionId={transactionId}") {
        fun createRoute(transactionId: Long) = "finance/detail?transactionId=$transactionId"
    }
    data object CategoryEditor : Screen("finance/categories/edit?categoryId={categoryId}") {
        fun createRoute(categoryId: Long? = null) = "finance/categories/edit?categoryId=${categoryId ?: 0}"
    }
    data object Graph : Screen("graph")
    data object FinanceStats : Screen("finance/stats")
    data object MoodStats : Screen("journal/stats")
    data object BudgetEditor : Screen("finance/budget?budgetId={budgetId}") {
        fun createRoute(budgetId: Long? = null) = "finance/budget?budgetId=${budgetId ?: 0}"
    }
    data object Assessment : Screen("journal/assessment")
    data object AssessmentHistory : Screen("journal/assessment/history")
    data object ThoughtJournal : Screen("journal/thought?entryId={entryId}") {
        fun createRoute(entryId: Long? = null) = "journal/thought?entryId=${entryId ?: 0}"
    }
    data object ExposureJournal : Screen("journal/exposure?entryId={entryId}") {
        fun createRoute(entryId: Long? = null) = "journal/exposure?entryId=${entryId ?: 0}"
    }
    data object GratitudeJournal : Screen("journal/gratitude?entryId={entryId}&fromReminder={fromReminder}") {
        fun createRoute(entryId: Long? = null, fromReminder: Boolean = false) =
            "journal/gratitude?entryId=${entryId ?: 0}&fromReminder=$fromReminder"
    }
    data object ReflectionJournal : Screen("journal/reflection?entryId={entryId}&fromReminder={fromReminder}") {
        fun createRoute(entryId: Long? = null, fromReminder: Boolean = false) =
            "journal/reflection?entryId=${entryId ?: 0}&fromReminder=$fromReminder"
    }
    data object SavoringJournal : Screen("journal/savoring?entryId={entryId}") {
        fun createRoute(entryId: Long? = null) = "journal/savoring?entryId=${entryId ?: 0}"
    }
    data object CheckInJournal : Screen("journal/checkin?entryId={entryId}&fromReminder={fromReminder}") {
        fun createRoute(entryId: Long? = null, fromReminder: Boolean = false) =
            "journal/checkin?entryId=${entryId ?: 0}&fromReminder=$fromReminder"
    }
    data object FoodJournal : Screen("journal/food?entryId={entryId}") {
        fun createRoute(entryId: Long? = null) = "journal/food?entryId=${entryId ?: 0}"
    }
    data object ThoughtJournalDetail : Screen("journal/thought/detail/{entryId}") {
        fun createRoute(entryId: Long) = "journal/thought/detail/$entryId"
    }
    data object ExposureJournalDetail : Screen("journal/exposure/detail/{entryId}") {
        fun createRoute(entryId: Long) = "journal/exposure/detail/$entryId"
    }
    data object Export : Screen("journal/export")
    data object RecurringBills : Screen("finance/bills")
    data object RecurringBillEditor : Screen("finance/bills/edit?billId={billId}") {
        fun createRoute(billId: Long? = null) = "finance/bills/edit?billId=${billId ?: 0}"
    }
    data object Goals : Screen("finance/goals")
    data object GoalEditor : Screen("finance/goals/edit?goalId={goalId}") {
        fun createRoute(goalId: Long? = null) = "finance/goals/edit?goalId=${goalId ?: 0}"
    }
    data object NetWorth : Screen("finance/networth")
    data object AssetEditor : Screen("finance/networth/edit?assetId={assetId}") {
        fun createRoute(assetId: Long? = null) = "finance/networth/edit?assetId=${assetId ?: 0}"
    }
    data object SafeToSpend : Screen("finance/safetospend")
    data object FinanceInsights : Screen("finance/insights")
    data object BudgetTemplate : Screen("finance/budget/template")
    data object Settings : Screen("settings")
    data object Search : Screen("search?query={query}") {
        fun createRoute(query: String? = null): String {
            val encoded = Uri.encode(query ?: "")
            return "search?query=$encoded"
        }
    }
}
