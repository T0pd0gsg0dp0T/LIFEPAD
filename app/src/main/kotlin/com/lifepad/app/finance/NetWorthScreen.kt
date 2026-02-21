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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
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
import com.lifepad.app.components.NetWorthLineChart
import com.lifepad.app.data.local.entity.AssetEntity
import com.lifepad.app.ui.theme.ExpenseColor
import com.lifepad.app.ui.theme.IncomeColor
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthScreen(
    onNavigateBack: () -> Unit,
    onEditAsset: (Long) -> Unit,
    onCreateAsset: () -> Unit,
    viewModel: NetWorthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Net Worth") },
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
            FloatingActionButton(onClick = onCreateAsset) {
                Icon(Icons.Default.Add, "Add asset")
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
                .testTag("screen_networth"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero net worth card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Net Worth",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormat.format(uiState.currentNetWorth),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.currentNetWorth >= 0) IncomeColor else ExpenseColor
                        )

                        // Trend vs previous
                        if (uiState.previousNetWorth != null) {
                            val change = uiState.currentNetWorth - uiState.previousNetWorth!!
                            val icon = if (change >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown
                            val color = if (change >= 0) IncomeColor else ExpenseColor
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = color)
                                Text(
                                    text = " ${currencyFormat.format(kotlin.math.abs(change))} vs last month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }

            // Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Account Balances", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                currencyFormat.format(uiState.accountsTotal),
                                style = MaterialTheme.typography.bodyMedium,
                                color = IncomeColor
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Manual Assets", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                currencyFormat.format(uiState.assetsTotal),
                                style = MaterialTheme.typography.bodyMedium,
                                color = IncomeColor
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Liabilities", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "-${currencyFormat.format(uiState.liabilitiesTotal)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ExpenseColor
                            )
                        }
                    }
                }
            }

            // Net worth trend chart
            if (uiState.snapshots.size >= 2) {
                item {
                    Text(
                        "Net Worth Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        NetWorthLineChart(
                            snapshots = uiState.snapshots,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Assets list
            if (uiState.assets.isNotEmpty()) {
                item {
                    Text(
                        "Assets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.assets, key = { it.id }) { asset ->
                    AssetCard(
                        asset = asset,
                        onClick = { onEditAsset(asset.id) },
                        onDelete = { viewModel.deleteAsset(asset.id) },
                        currencyFormat = currencyFormat,
                        isLiability = false
                    )
                }
            }

            // Liabilities list
            if (uiState.liabilities.isNotEmpty()) {
                item {
                    Text(
                        "Liabilities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.liabilities, key = { it.id }) { liability ->
                    AssetCard(
                        asset = liability,
                        onClick = { onEditAsset(liability.id) },
                        onDelete = { viewModel.deleteAsset(liability.id) },
                        currencyFormat = currencyFormat,
                        isLiability = true
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetCard(
    asset: AssetEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    currencyFormat: NumberFormat,
    isLiability: Boolean
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
                Column {
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text(asset.assetType.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
                Text(
                    text = if (isLiability) "-${currencyFormat.format(asset.value)}"
                    else currencyFormat.format(asset.value),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLiability) ExpenseColor else IncomeColor
                )
            }
        }
    }
}
