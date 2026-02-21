package com.lifepad.app.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.lifepad.app.components.MoodIndicator
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.ui.theme.ExpenseColor
import com.lifepad.app.ui.theme.FinancePrimary
import com.lifepad.app.ui.theme.IncomeColor
import com.lifepad.app.ui.theme.JournalPrimary
import com.lifepad.app.ui.theme.NotepadPrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNoteClick: (Long) -> Unit,
    onEntryClick: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToFinance: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LIFEPAD",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                                .format(Date()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .testTag("screen_dashboard"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Today's mood
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = JournalPrimary.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Today's Mood",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (uiState.todayMood != null) {
                                MoodIndicator(mood = uiState.todayMood!!, showNumber = false)
                            } else {
                                Text(
                                    text = "—",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }
                    }

                    // Balance
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = FinancePrimary.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Balance",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currencyFormat.format(uiState.netBalance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.netBalance >= 0) IncomeColor else ExpenseColor
                            )
                        }
                    }

                    // Counts
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = NotepadPrimary.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.noteCount.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Safe-to-spend mini card
                if (uiState.safeToSpendDaily != 0.0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Safe to Spend Today",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormat.format(uiState.safeToSpendDaily),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.safeToSpendDaily > 0) IncomeColor else ExpenseColor
                            )
                        }
                    }
                }

                // Recent Notes
                if (uiState.recentNotes.isNotEmpty()) {
                    DashboardSection(
                        title = "Recent Notes",
                        onSeeAll = onNavigateToNotes
                    ) {
                        uiState.recentNotes.forEach { note ->
                            val preview = if (note.isChecklist) {
                                note.content.lines()
                                    .filter { it.trim().startsWith("- [") }
                                    .joinToString(", ") { line ->
                                        line.trim()
                                            .removePrefix("- [x] ").removePrefix("- [X] ").removePrefix("- [ ] ")
                                    }
                                    .take(50)
                            } else {
                                note.content.take(50).replace("\n", " ")
                            }
                            DashboardItem(
                                title = note.title.ifBlank { "Untitled" },
                                subtitle = preview,
                                onClick = { onNoteClick(note.id) }
                            )
                        }
                    }
                }

                // Recent Journal Entries
                if (uiState.recentEntries.isNotEmpty()) {
                    DashboardSection(
                        title = "Recent Journal Entries",
                        onSeeAll = onNavigateToJournal
                    ) {
                        uiState.recentEntries.forEach { entry ->
                            DashboardItem(
                                title = formatDate(entry.entryDate),
                                subtitle = entry.content.take(50).replace("\n", " "),
                                trailing = { MoodIndicator(mood = entry.mood) },
                                onClick = { onEntryClick(entry.id) }
                            )
                        }
                    }
                }

                // Recent Transactions
                if (uiState.recentTransactions.isNotEmpty()) {
                    DashboardSection(
                        title = "Recent Transactions",
                        onSeeAll = onNavigateToFinance
                    ) {
                        uiState.recentTransactions.forEach { transaction ->
                            val isExpense = transaction.type == TransactionType.EXPENSE
                            DashboardItem(
                                title = transaction.description.ifBlank { "No description" },
                                subtitle = formatDate(transaction.transactionDate),
                                trailing = {
                                    Text(
                                        text = "${if (isExpense) "-" else "+"}${currencyFormat.format(transaction.amount)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpense) ExpenseColor else IncomeColor
                                    )
                                },
                                onClick = { onTransactionClick(transaction.id) }
                            )
                        }
                    }
                }

                // Empty state
                if (uiState.recentNotes.isEmpty() && uiState.recentEntries.isEmpty() && uiState.recentTransactions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Welcome to LIFEPAD",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start by creating a note, journal entry, or tracking a transaction.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSection(
    title: String,
    onSeeAll: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.clickable(onClick = onSeeAll),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DashboardItem(
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
