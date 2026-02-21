package com.lifepad.app.journal

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.ErrorSnackbarHost
import com.lifepad.app.components.MoodIndicator
import com.lifepad.app.data.local.entity.JournalEntryEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalListScreen(
    onEntryClick: (Long) -> Unit,
    onCreateEntry: () -> Unit,
    onNavigateToStats: () -> Unit = {},
    onNavigateToThoughtJournal: () -> Unit = {},
    onNavigateToExposureJournal: () -> Unit = {},
    onStructuredEntryClick: (Long, String) -> Unit = { _, _ -> },
    viewModel: JournalListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Journal") },
                actions = {
                    IconButton(onClick = onNavigateToStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Mood Statistics")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEntry,
                modifier = Modifier.testTag("fab_create_entry")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create entry")
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
                .testTag("screen_journal")
        ) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onSearch = { },
                        expanded = uiState.isSearching,
                        onExpandedChange = { if (!it) viewModel.clearSearch() },
                        placeholder = { Text("Search journal...") },
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
                    items(uiState.searchResults, key = { it.id }) { entry ->
                        JournalEntryItem(
                            entry = entry,
                            onClick = {
                                if (entry.structuredData.isNotBlank() &&
                                    entry.template in listOf("thought_record", "exposure")
                                ) {
                                    onStructuredEntryClick(entry.id, entry.template)
                                } else {
                                    onEntryClick(entry.id)
                                }
                            },
                            onTogglePin = { viewModel.togglePin(entry.id) },
                            onDelete = { viewModel.deleteEntry(entry.id) }
                        )
                    }
                }
            }

            // Greeting + Quick Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Greeting
                val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                    in 5..11 -> "Good morning"
                    in 12..16 -> "Good afternoon"
                    else -> "Good evening"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (uiState.currentStreak > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${uiState.currentStreak}-day streak",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick action cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "Thought\nJournal",
                        icon = Icons.Default.Psychology,
                        onClick = onNavigateToThoughtJournal,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Exposure\nLog",
                        icon = Icons.Default.Shield,
                        onClick = onNavigateToExposureJournal,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Quick\nEntry",
                        icon = Icons.Default.Edit,
                        onClick = onCreateEntry,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No journal entries",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to start journaling",
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
                    items(uiState.entries, key = { it.id }) { entry ->
                        JournalEntryItem(
                            entry = entry,
                            onClick = {
                                if (entry.structuredData.isNotBlank() &&
                                    entry.template in listOf("thought_record", "exposure")
                                ) {
                                    onStructuredEntryClick(entry.id, entry.template)
                                } else {
                                    onEntryClick(entry.id)
                                }
                            },
                            onTogglePin = { viewModel.togglePin(entry.id) },
                            onDelete = { viewModel.deleteEntry(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalEntryItem(
    entry: JournalEntryEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
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
                .testTag("journal_item"),
            colors = CardDefaults.cardColors(
                containerColor = if (entry.isPinned)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(entry.entryDate),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MoodIndicator(mood = entry.mood)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = entry.template.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (entry.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (entry.isPinned) "Unpin" else "Pin",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.content.take(150).replace("\n", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
