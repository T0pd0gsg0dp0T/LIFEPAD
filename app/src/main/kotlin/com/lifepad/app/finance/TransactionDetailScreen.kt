package com.lifepad.app.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.ErrorSnackbarHost
import com.lifepad.app.components.HashtagChip
import com.lifepad.app.components.InteractiveMarkdownText
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.ui.theme.ExpenseColor
import com.lifepad.app.ui.theme.IncomeColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onNavigateBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenLinkedNote: (Long) -> Unit,
    onOpenLinkedJournal: (Long) -> Unit,
    onOpenLinkedTransaction: (Long) -> Unit,
    onHashtagClick: (String) -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val transaction = uiState.transaction

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (transaction != null) {
                        IconButton(onClick = { onEdit(transaction.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        snackbarHost = {
            ErrorSnackbarHost(
                errorMessage = uiState.errorMessage,
                onDismiss = viewModel::clearError
            )
        }
    ) { padding ->
        if (transaction == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text("Transaction not found", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        val currencyFormat = NumberFormat.getCurrencyInstance()
        val isExpense = transaction.type == TransactionType.EXPENSE

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = uiState.category?.name ?: "Uncategorized",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            InteractiveMarkdownText(
                content = transaction.description.ifBlank { "No description" },
                onWikilinkClick = { target ->
                    viewModel.openWikilink(
                        target = target,
                        onNoteResolved = onOpenLinkedNote,
                        onJournalResolved = onOpenLinkedJournal,
                        onTransactionResolved = onOpenLinkedTransaction
                    )
                },
                onHashtagClick = onHashtagClick
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatDate(transaction.transactionDate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (if (isExpense) "-" else "+") + currencyFormat.format(transaction.amount),
                style = MaterialTheme.typography.headlineSmall,
                color = if (isExpense) ExpenseColor else IncomeColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (uiState.hashtags.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.hashtags.size) { index ->
                        val hashtag = uiState.hashtags[index]
                        HashtagChip(name = hashtag.name, onClick = { onHashtagClick(hashtag.name) })
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
