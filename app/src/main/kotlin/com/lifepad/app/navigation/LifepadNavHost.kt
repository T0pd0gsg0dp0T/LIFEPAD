package com.lifepad.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lifepad.app.dashboard.DashboardScreen
import com.lifepad.app.data.graph.NodeType
import com.lifepad.app.finance.AssetEditorScreen
import com.lifepad.app.finance.BudgetEditorScreen
import com.lifepad.app.finance.BudgetTemplateScreen
import com.lifepad.app.finance.FinanceInsightsScreen
import com.lifepad.app.finance.FinanceScreen
import com.lifepad.app.finance.FinanceStatsScreen
import com.lifepad.app.finance.GoalEditorScreen
import com.lifepad.app.finance.GoalsScreen
import com.lifepad.app.finance.NetWorthScreen
import com.lifepad.app.finance.RecurringBillEditorScreen
import com.lifepad.app.finance.RecurringBillsScreen
import com.lifepad.app.finance.SafeToSpendScreen
import com.lifepad.app.finance.TransactionEditorScreen
import com.lifepad.app.journal.AssessmentHistoryScreen
import com.lifepad.app.journal.AssessmentScreen
import com.lifepad.app.journal.JournalEditorScreen
import com.lifepad.app.journal.JournalListScreen
import com.lifepad.app.journal.ExportScreen
import com.lifepad.app.journal.ExposureJournalDetailScreen
import com.lifepad.app.journal.ExposureJournalScreen
import com.lifepad.app.journal.MoodStatsScreen
import com.lifepad.app.journal.ThoughtJournalDetailScreen
import com.lifepad.app.journal.ThoughtJournalScreen
import com.lifepad.app.notepad.GraphScreen
import com.lifepad.app.notepad.NoteEditorScreen
import com.lifepad.app.notepad.NoteListScreen
import com.lifepad.app.search.SearchScreen

@Composable
fun LifepadNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        // Dashboard
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNoteClick = { navController.navigate(Screen.NoteEditor.createRoute(it)) },
                onEntryClick = { navController.navigate(Screen.JournalEditor.createRoute(it)) },
                onTransactionClick = { navController.navigate(Screen.TransactionEditor.createRoute(it)) },
                onNavigateToNotes = { navController.navigate(Screen.Notepad.route) },
                onNavigateToJournal = { navController.navigate(Screen.Journal.route) },
                onNavigateToFinance = { navController.navigate(Screen.Finance.route) }
            )
        }

        // Notepad
        composable(Screen.Notepad.route) {
            NoteListScreen(
                onNoteClick = { navController.navigate(Screen.NoteEditor.createRoute(it)) },
                onCreateNote = { navController.navigate(Screen.NoteEditor.createRoute()) },
                onNavigateToGraph = { navController.navigate(Screen.Graph.route) }
            )
        }

        composable(Screen.Graph.route) {
            GraphScreen(
                onNavigateBack = { navController.popBackStack() },
                onNodeClick = { id, type ->
                    when (type) {
                        NodeType.NOTE -> {
                            val noteId = id.substringAfter("note_").toLongOrNull()
                            if (noteId != null) {
                                navController.navigate(Screen.NoteEditor.createRoute(noteId))
                            }
                        }
                        NodeType.JOURNAL_ENTRY -> {
                            val entryId = id.substringAfter("entry_").toLongOrNull()
                            if (entryId != null) {
                                navController.navigate(Screen.JournalEditor.createRoute(entryId))
                            }
                        }
                        NodeType.TRANSACTION -> {}
                        NodeType.HASHTAG -> {}
                    }
                }
            )
        }

        composable(
            route = Screen.NoteEditor.route,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            NoteEditorScreen(
                onNavigateBack = { navController.popBackStack() },
                onNoteClick = { navController.navigate(Screen.NoteEditor.createRoute(it)) },
                onJournalEntryClick = { navController.navigate(Screen.JournalEditor.createRoute(it)) },
                onHashtagClick = { hashtag ->
                    navController.navigate(Screen.Search.createRoute("#$hashtag"))
                }
            )
        }

        // Journal
        composable(Screen.Journal.route) {
            JournalListScreen(
                onEntryClick = { navController.navigate(Screen.JournalEditor.createRoute(it)) },
                onCreateEntry = { navController.navigate(Screen.JournalEditor.createRoute()) },
                onNavigateToStats = { navController.navigate(Screen.MoodStats.route) },
                onNavigateToThoughtJournal = { navController.navigate(Screen.ThoughtJournal.createRoute()) },
                onNavigateToExposureJournal = { navController.navigate(Screen.ExposureJournal.createRoute()) },
                onStructuredEntryClick = { id, template ->
                    when (template) {
                        "thought_record" -> navController.navigate(Screen.ThoughtJournalDetail.createRoute(id))
                        "exposure" -> navController.navigate(Screen.ExposureJournalDetail.createRoute(id))
                        else -> navController.navigate(Screen.JournalEditor.createRoute(id))
                    }
                }
            )
        }

        composable(Screen.MoodStats.route) {
            MoodStatsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAssessment = { navController.navigate(Screen.Assessment.route) },
                onNavigateToAssessmentHistory = { navController.navigate(Screen.AssessmentHistory.route) },
                onNavigateToExport = { navController.navigate(Screen.Export.route) }
            )
        }

        composable(Screen.Export.route) {
            ExportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Assessment.route) {
            AssessmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AssessmentHistory.route) {
            AssessmentHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.JournalEditor.route,
            arguments = listOf(
                navArgument("entryId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            JournalEditorScreen(
                onNavigateBack = { navController.popBackStack() },
                onNoteClick = { navController.navigate(Screen.NoteEditor.createRoute(it)) },
                onHashtagClick = { hashtag ->
                    navController.navigate(Screen.Search.createRoute("#$hashtag"))
                }
            )
        }

        // Thought Journal
        composable(
            route = Screen.ThoughtJournal.route,
            arguments = listOf(
                navArgument("entryId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            ThoughtJournalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Thought Journal Detail
        composable(
            route = Screen.ThoughtJournalDetail.route,
            arguments = listOf(
                navArgument("entryId") { type = NavType.LongType }
            )
        ) {
            ThoughtJournalDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onEdit = { entryId ->
                    navController.navigate(Screen.ThoughtJournal.createRoute(entryId))
                }
            )
        }

        // Exposure Journal Detail
        composable(
            route = Screen.ExposureJournalDetail.route,
            arguments = listOf(
                navArgument("entryId") { type = NavType.LongType }
            )
        ) {
            ExposureJournalDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onEdit = { entryId ->
                    navController.navigate(Screen.ExposureJournal.createRoute(entryId))
                }
            )
        }

        // Exposure Journal
        composable(
            route = Screen.ExposureJournal.route,
            arguments = listOf(
                navArgument("entryId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            ExposureJournalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Finance
        composable(Screen.Finance.route) {
            FinanceScreen(
                onTransactionClick = { navController.navigate(Screen.TransactionEditor.createRoute(it)) },
                onCreateTransaction = { navController.navigate(Screen.TransactionEditor.createRoute()) },
                onManageBudgets = { navController.navigate(Screen.BudgetEditor.createRoute()) },
                onBudgetClick = { navController.navigate(Screen.BudgetEditor.createRoute(it)) },
                onNavigateToStats = { navController.navigate(Screen.FinanceStats.route) },
                onNavigateToBills = { navController.navigate(Screen.RecurringBills.route) },
                onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                onNavigateToNetWorth = { navController.navigate(Screen.NetWorth.route) },
                onNavigateToSafeToSpend = { navController.navigate(Screen.SafeToSpend.route) },
                onNavigateToInsights = { navController.navigate(Screen.FinanceInsights.route) },
                onNavigateToBudgetTemplates = { navController.navigate(Screen.BudgetTemplate.route) }
            )
        }

        composable(
            route = Screen.TransactionEditor.route,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            TransactionEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FinanceStats.route) {
            FinanceStatsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.BudgetEditor.route,
            arguments = listOf(
                navArgument("budgetId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            BudgetEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Recurring Bills
        composable(Screen.RecurringBills.route) {
            RecurringBillsScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateBill = { navController.navigate(Screen.RecurringBillEditor.createRoute()) },
                onEditBill = { navController.navigate(Screen.RecurringBillEditor.createRoute(it)) }
            )
        }

        composable(
            route = Screen.RecurringBillEditor.route,
            arguments = listOf(
                navArgument("billId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            RecurringBillEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Goals
        composable(Screen.Goals.route) {
            GoalsScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateGoal = { navController.navigate(Screen.GoalEditor.createRoute()) },
                onEditGoal = { navController.navigate(Screen.GoalEditor.createRoute(it)) }
            )
        }

        composable(
            route = Screen.GoalEditor.route,
            arguments = listOf(
                navArgument("goalId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            GoalEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Net Worth
        composable(Screen.NetWorth.route) {
            NetWorthScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateAsset = { navController.navigate(Screen.AssetEditor.createRoute()) },
                onEditAsset = { navController.navigate(Screen.AssetEditor.createRoute(it)) }
            )
        }

        composable(
            route = Screen.AssetEditor.route,
            arguments = listOf(
                navArgument("assetId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            AssetEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Safe to Spend
        composable(Screen.SafeToSpend.route) {
            SafeToSpendScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Finance Insights
        composable(Screen.FinanceInsights.route) {
            FinanceInsightsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Budget Template
        composable(Screen.BudgetTemplate.route) {
            BudgetTemplateScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Search
        composable(
            route = Screen.Search.route,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) {
            SearchScreen(
                onNoteClick = { navController.navigate(Screen.NoteEditor.createRoute(it)) },
                onEntryClick = { navController.navigate(Screen.JournalEditor.createRoute(it)) },
                onTransactionClick = { navController.navigate(Screen.TransactionEditor.createRoute(it)) }
            )
        }
    }
}
