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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.ErrorSnackbarHost
import com.lifepad.app.data.local.dao.RecurringBillWithCategory
import com.lifepad.app.domain.finance.DetectedBill
import com.lifepad.app.ui.theme.ExpenseColor
import com.lifepad.app.ui.theme.IncomeColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillsScreen(
    onNavigateBack: () -> Unit,
    onEditBill: (Long) -> Unit,
    onCreateBill: () -> Unit,
    viewModel: RecurringBillsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance()
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring Bills") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("nav_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateBill) {
                Icon(Icons.Default.Add, "Add bill")
            }
        },
        snackbarHost = {
            ErrorSnackbarHost(
                errorMessage = uiState.errorMessage,
                onDismiss = viewModel::clearError
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("screen_bills"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Detect recurring button
            item {
                OutlinedButton(
                    onClick = viewModel::detectRecurring,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isDetecting
                ) {
                    if (uiState.isDetecting) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Scan for Recurring Transactions")
                }
            }

            // Detected candidates
            if (uiState.detectedCandidates.isNotEmpty()) {
                item {
                    Text(
                        text = "Detected (${uiState.detectedCandidates.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(uiState.detectedCandidates, key = { it.name }) { candidate ->
                    DetectedBillCard(
                        candidate = candidate,
                        onConfirm = { viewModel.confirmBill(candidate) },
                        onDismiss = { viewModel.dismissCandidate(candidate) },
                        currencyFormat = currencyFormat
                    )
                }
            }

            // Confirmed bills
            if (uiState.bills.isNotEmpty()) {
                item {
                    Text(
                        text = "Confirmed Bills (${uiState.bills.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(uiState.bills, key = { it.id }) { bill ->
                    BillCard(
                        bill = bill,
                        onClick = { onEditBill(bill.id) },
                        onDelete = { viewModel.deleteBill(bill.id) },
                        onToggleEnabled = { viewModel.toggleEnabled(bill.id) },
                        currencyFormat = currencyFormat,
                        dateFormat = dateFormat
                    )
                }
            }

            if (uiState.bills.isEmpty() && uiState.detectedCandidates.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No recurring bills", style = MaterialTheme.typography.headlineSmall)
                            Text("Tap + to add or scan your transactions", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectedBillCard(
    candidate: DetectedBill,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                    text = candidate.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = currencyFormat.format(candidate.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ExpenseColor
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text(candidate.frequency.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
                Text(
                    text = "Detected from ${candidate.detectedFromCount} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column {
                IconButton(onClick = onConfirm) {
                    Icon(Icons.Default.Check, "Confirm", tint = IncomeColor)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Dismiss", tint = ExpenseColor)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillCard(
    bill: RecurringBillWithCategory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
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
                        text = bill.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = currencyFormat.format(bill.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (bill.transactionType == "INCOME") IncomeColor else ExpenseColor
                        )
                        bill.categoryName?.let { cat ->
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "Next: ${dateFormat.format(Date(bill.nextDueDate))} | ${bill.frequency.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = bill.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }
        }
    }
}
