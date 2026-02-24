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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.MoodLineChart
import com.lifepad.app.components.MoodCalendar
import com.lifepad.app.components.MoodDistributionChart
import com.lifepad.app.components.MoodTimeOfDayChart
import com.lifepad.app.components.EmotionFrequencyChart
import com.lifepad.app.components.TrapFrequencyChart
import com.lifepad.app.dashboard.PeriodComparison
import com.lifepad.app.dashboard.PeriodSummary
import com.lifepad.app.components.IncomeExpenseBarChart
import com.lifepad.app.components.CashflowLineChart
import com.lifepad.app.components.NetWorthLineChart
import com.lifepad.app.settings.FinanceWidget
import com.lifepad.app.settings.MoodWidget
import com.lifepad.app.ui.theme.NotepadPrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNoteClick: (Long) -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onCreateNote: () -> Unit,
    onCreateEntry: (String) -> Unit,
    onCreateTransaction: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance()
    var showAddDialog by remember { mutableStateOf(false) }
    var showJournalTemplateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.testTag("screen_dashboard"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LIFEPAD",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                                .format(Date()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("dashboard_search")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("dashboard_settings")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardSection(
                    title = "Finance",
                    onSeeAll = onNavigateToFinance,
                    seeAllTag = "dashboard_finance_see_all"
                ) {
                    when (uiState.financeWidget) {
                        FinanceWidget.INCOME_EXPENSE -> {
                            if (uiState.incomeExpenseData.isEmpty()) {
                                EmptyCardText("No transactions yet")
                            } else {
                                IncomeExpenseBarChart(data = uiState.incomeExpenseData)
                            }
                        }
                        FinanceWidget.CASHFLOW -> {
                            if (uiState.cashflowPoints.isEmpty()) {
                                EmptyCardText("Not enough data for forecast")
                            } else {
                                CashflowLineChart(forecastPoints = uiState.cashflowPoints)
                            }
                        }
                        FinanceWidget.NET_WORTH -> {
                            if (uiState.netWorthSnapshots.isEmpty()) {
                                EmptyCardText("No net worth snapshots yet")
                            } else {
                                NetWorthLineChart(snapshots = uiState.netWorthSnapshots)
                            }
                        }
                    }
                }

                DashboardSection(
                    title = "${uiState.moodWidget.label} (${uiState.moodWidgetPeriod.label.lowercase()})",
                    onSeeAll = onNavigateToJournal,
                    seeAllTag = "dashboard_journal_see_all"
                ) {
                    when (uiState.moodWidget) {
                        MoodWidget.MOOD_LINE -> {
                            if (uiState.moodLinePoints.isEmpty()) {
                                EmptyCardText("No mood entries yet")
                            } else {
                                MoodLineChart(dataPoints = uiState.moodLinePoints)
                            }
                        }
                        MoodWidget.MOOD_COMPARE_WEEK -> {
                            val comparison = uiState.weekComparison
                            if (comparison == null) {
                                EmptyCardText("No mood entries yet")
                            } else {
                                ComparisonContent(comparison = comparison)
                            }
                        }
                        MoodWidget.MOOD_COMPARE_MONTH -> {
                            val comparison = uiState.monthComparison
                            if (comparison == null) {
                                EmptyCardText("No mood entries yet")
                            } else {
                                ComparisonContent(comparison = comparison)
                            }
                        }
                        MoodWidget.MOOD_CALENDAR -> {
                            if (uiState.moodCalendarMap.isEmpty()) {
                                EmptyCardText("No mood entries yet")
                            } else {
                                MoodCalendar(
                                    dailyMoodMap = uiState.moodCalendarMap,
                                    periodDays = uiState.moodWidgetPeriod.days
                                )
                            }
                        }
                        MoodWidget.MOOD_DISTRIBUTION -> {
                            if (uiState.moodDistribution.isEmpty()) {
                                EmptyCardText("No mood entries yet")
                            } else {
                                MoodDistributionChart(distribution = uiState.moodDistribution)
                            }
                        }
                        MoodWidget.MOOD_TIME_OF_DAY -> {
                            val entriesWithData = uiState.moodByTimeOfDay.filter { it.count > 0 }
                            if (entriesWithData.isEmpty()) {
                                EmptyCardText("No mood entries yet")
                            } else {
                                MoodTimeOfDayChart(entries = uiState.moodByTimeOfDay)
                            }
                        }
                        MoodWidget.EMOTION_FREQUENCY -> {
                            if (uiState.emotionFrequency.isEmpty()) {
                                EmptyCardText("No emotion data yet")
                            } else {
                                EmotionFrequencyChart(emotionFrequency = uiState.emotionFrequency)
                            }
                        }
                        MoodWidget.TRAP_FREQUENCY -> {
                            if (uiState.trapFrequency.isEmpty()) {
                                EmptyCardText("No thinking traps yet")
                            } else {
                                TrapFrequencyChart(trapFrequency = uiState.trapFrequency)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .testTag("dashboard_notes_nav")
                        .clickable(onClick = onNavigateToNotes),
                    colors = CardDefaults.cardColors(
                        containerColor = lerp(NotepadPrimary, Color.Black, 0.85f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .testTag("dashboard_notes_title")
                                    .clickable(onClick = onNavigateToNotes)
                            )
                            Text(
                                text = "View all",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .testTag("dashboard_notes_view_all")
                                    .clickable(onClick = onNavigateToNotes)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val recentNoteId = uiState.recentNoteId
                        val recentNoteTitle = uiState.recentNoteTitle
                        if (recentNoteId != null && recentNoteTitle != null) {
                            Text(
                                text = recentNoteTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNoteClick(recentNoteId) }
                            )
                        } else {
                            Text(
                                text = "No notes yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { showAddDialog = false; onCreateNote() },
                        modifier = Modifier.testTag("dashboard_add_note")
                    ) {
                        Text(text = "Note")
                    }
                    TextButton(
                        onClick = { showAddDialog = false; showJournalTemplateDialog = true },
                        modifier = Modifier.testTag("dashboard_add_journal")
                    ) {
                        Text(text = "Journal Entry")
                    }
                    TextButton(
                        onClick = { showAddDialog = false; onCreateTransaction() },
                        modifier = Modifier.testTag("dashboard_add_transaction")
                    ) {
                        Text(text = "Transaction")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showJournalTemplateDialog) {
        val templates = listOf(
            "free" to "Free Writing",
            "thought_record" to "Thought Record",
            "gratitude" to "Gratitude",
            "reflection" to "Daily Reflection",
            "savoring" to "Savoring",
            "exposure" to "Exposure",
            "check_in" to "Check-in",
            "food" to "Food Journal"
        )
        AlertDialog(
            onDismissRequest = { showJournalTemplateDialog = false },
            title = { Text("Journal entry type") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { (key, label) ->
                        TextButton(onClick = {
                            showJournalTemplateDialog = false
                            onCreateEntry(key)
                        }) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showJournalTemplateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ComparisonContent(
    comparison: PeriodComparison
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ComparisonColumn(label = comparison.labelCurrent, summary = comparison.current)
        ComparisonColumn(label = comparison.labelPrevious, summary = comparison.previous)
    }
}

@Composable
private fun ComparisonColumn(
    label: String,
    summary: PeriodSummary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            text = if (summary.entryCount > 0)
                "Avg: ${String.format(Locale.getDefault(), "%.1f", summary.averageMood)}"
            else "No entries",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (summary.trendData.isNotEmpty()) {
            MoodLineChart(dataPoints = summary.trendData)
        }
        if (summary.distribution.isNotEmpty()) {
            MoodDistributionChart(distribution = summary.distribution)
        }
        if (summary.timeOfDay.isNotEmpty()) {
            MoodTimeOfDayChart(entries = summary.timeOfDay)
        }
    }
}

@Composable
private fun DashboardSection(
    title: String,
    onSeeAll: (() -> Unit)?,
    seeAllTag: String? = null,
    content: @Composable () -> Unit
) {
    val cardColor = lerp(MaterialTheme.colorScheme.surfaceVariant, Color.Black, 0.55f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                if (onSeeAll != null) {
                    Row(
                        modifier = Modifier
                            .then(if (seeAllTag != null) Modifier.testTag(seeAllTag) else Modifier)
                            .clickable(onClick = onSeeAll),
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
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
@Composable
private fun EmptyCardText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
