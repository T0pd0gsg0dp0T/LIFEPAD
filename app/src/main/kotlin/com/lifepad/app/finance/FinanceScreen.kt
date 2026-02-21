package com.lifepad.app.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.ErrorSnackbarHost
import com.lifepad.app.components.InsightCard
import com.lifepad.app.data.local.entity.TransactionEntity
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.ui.theme.ExpenseColor
import com.lifepad.app.ui.theme.IncomeColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    onTransactionClick: (Long) -> Unit,
    onCreateTransaction: () -> Unit,
    onManageBudgets: () -> Unit = {},
    onBudgetClick: (Long) -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToBills: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToNetWorth: () -> Unit = {},
    onNavigateToSafeToSpend: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToBudgetTemplates: () -> Unit = {},
    viewModel: FinanceViewModel = hiltViewModel(),
    insightsViewModel: FinanceInsightsViewModel = hiltViewModel(),
    safeToSpendViewModel: SafeToSpendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val insightsState by insightsViewModel.uiState.collectAsStateWithLifecycle()
    val safeToSpendState by safeToSpendViewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(Locale.getDefault())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance") },
                actions = {
                    IconButton(
                        onClick = onNavigateToBills,
                        modifier = Modifier.testTag("nav_bills")
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Recurring Bills")
                    }
                    IconButton(
                        onClick = onNavigateToGoals,
                        modifier = Modifier.testTag("nav_goals")
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = "Goals")
                    }
                    IconButton(
                        onClick = onNavigateToNetWorth,
                        modifier = Modifier.testTag("nav_networth")
                    ) {
                        Icon(Icons.Default.PieChart, contentDescription = "Net Worth")
                    }
                    IconButton(
                        onClick = onNavigateToStats,
                        modifier = Modifier.testTag("nav_finance_stats")
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = "Finance Statistics")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTransaction,
                modifier = Modifier.testTag("fab_create_transaction")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        },
        snackbarHost = {
            ErrorSnackbarHost(
                errorMessage = uiState.errorMessage,
                onDismiss = viewModel::clearError
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("screen_finance")
        ) {
            // Balance card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Net Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currencyFormat.format(uiState.netBalance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.netBalance >= 0) IncomeColor else ExpenseColor
                    )
                }
            }

            // Safe-to-spend widget
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(onClick = onNavigateToSafeToSpend)
                    .testTag("finance_safe_to_spend"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Safe to Spend Today",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currencyFormat.format(safeToSpendState.dailyAllowance),
                            modifier = Modifier.testTag("safe_to_spend_amount"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (safeToSpendState.dailyAllowance > 0) IncomeColor else ExpenseColor
                        )
                    }
                    Text(
                        text = "${safeToSpendState.daysRemaining} days left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Insights row
            if (insightsState.insights.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("finance_insights_row")
                ) {
                    items(insightsState.insights.take(5)) { insight ->
                        InsightCard(
                            insight = insight,
                            compact = true,
                            modifier = Modifier.clickable(onClick = onNavigateToInsights)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Budget progress
            if (uiState.budgetsWithSpending.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Budgets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(onClick = onManageBudgets) {
                                Text("Manage")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.budgetsWithSpending.forEach { budget ->
                            val progress = if (budget.limitAmount > 0) (budget.spent / budget.limitAmount).coerceIn(0.0, 1.0) else 0.0
                            val isOverBudget = budget.spent > budget.limitAmount
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onBudgetClick(budget.id) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = budget.categoryName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${currencyFormat.format(budget.spent)} / ${currencyFormat.format(budget.limitAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isOverBudget) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress.toFloat() },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (isOverBudget) ExpenseColor else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onNavigateToBudgetTemplates) {
                        Text("Use Template")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = onManageBudgets) {
                        Text("Set Budgets")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onSearch = { },
                        expanded = uiState.isSearching,
                        onExpandedChange = { if (!it) viewModel.clearSearch() },
                        placeholder = { Text("Search transactions...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = viewModel::clearSearch) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                },
                expanded = uiState.isSearching,
                onExpandedChange = { if (!it) viewModel.clearSearch() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.searchResults, key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.id) },
                            onDelete = { viewModel.deleteTransaction(transaction.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No transactions",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to add a transaction",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.transactions, key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction.id) },
                            onDelete = { viewModel.deleteTransaction(transaction.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    val currencyFormat = NumberFormat.getCurrencyInstance()
    val isExpense = transaction.type == TransactionType.EXPENSE

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .testTag("transaction_item"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.description.ifBlank { "No description" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDate(transaction.transactionDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "${if (isExpense) "-" else "+"}${currencyFormat.format(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) ExpenseColor else IncomeColor
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
