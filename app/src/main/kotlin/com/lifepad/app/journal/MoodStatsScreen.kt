package com.lifepad.app.journal

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.EmotionFrequencyChart
import com.lifepad.app.components.MoodCalendar
import com.lifepad.app.components.MoodDistributionChart
import com.lifepad.app.components.MoodLineChart
import com.lifepad.app.components.MoodTimeOfDayChart
import java.util.Locale
import com.lifepad.app.components.TrapFrequencyChart
import com.lifepad.app.components.getMoodEmoji
import com.lifepad.app.journal.PeriodComparison
import com.lifepad.app.journal.PeriodSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodStatsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAssessment: (() -> Unit)? = null,
    onNavigateToAssessmentHistory: (() -> Unit)? = null,
    onNavigateToExport: (() -> Unit)? = null,
    viewModel: MoodStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mood Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            ) {
                // Period selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoodStatsPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = uiState.selectedPeriod == period,
                            onClick = { viewModel.onPeriodChange(period) },
                            label = { Text(period.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Average",
                        value = if (uiState.entryCount > 0)
                            String.format(Locale.getDefault(), "%.1f", uiState.averageMood)
                        else "-"
                    )
                    StatItem(
                        label = "Entries",
                        value = uiState.entryCount.toString()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mood Calendar Heatmap
                if (uiState.dailyMoodMap.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Mood Calendar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MoodCalendar(
                                dailyMoodMap = uiState.dailyMoodMap,
                                periodDays = uiState.selectedPeriod.days
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Mood trend chart
                if (uiState.trendData.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Mood Trend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MoodLineChart(dataPoints = uiState.trendData)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Mood distribution chart
                if (uiState.distribution.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Mood Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MoodDistributionChart(distribution = uiState.distribution)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                uiState.weekComparison?.let { comparison ->
                    ComparisonSection(title = "Week vs Last Week", comparison = comparison)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                uiState.monthComparison?.let { comparison ->
                    ComparisonSection(title = "Month vs Last Month", comparison = comparison)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Journaling Stats section
                if (uiState.totalEntries > 0) {
                    Text(
                        text = "Journaling Stats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        JournalStatCard(
                            label = "Total Entries",
                            value = uiState.totalEntries.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        JournalStatCard(
                            label = "Current Streak",
                            value = "${uiState.currentStreak}d",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        JournalStatCard(
                            label = "Longest Streak",
                            value = "${uiState.longestStreak}d",
                            modifier = Modifier.weight(1f)
                        )
                        JournalStatCard(
                            label = "Avg Words/Entry",
                            value = uiState.avgWordsPerEntry.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        JournalStatCard(
                            label = "Most Common Mood",
                            value = uiState.mostCommonMood?.let { "${getMoodEmoji(it)} $it" } ?: "-",
                            modifier = Modifier.weight(1f)
                        )
                        JournalStatCard(
                            label = "Top Template",
                            value = uiState.topTemplate?.replaceFirstChar { it.uppercase() }
                                ?.replace("_", " ") ?: "-",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Assessments navigation cards
                if (onNavigateToAssessment != null || onNavigateToAssessmentHistory != null) {
                    Text(
                        text = "Assessments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onNavigateToAssessment != null) {
                            OutlinedCard(
                                onClick = onNavigateToAssessment,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Take Assessment",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "GAD-7 or PHQ-9",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        if (onNavigateToAssessmentHistory != null) {
                            OutlinedCard(
                                onClick = onNavigateToAssessmentHistory,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "View History",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Score trends",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Thinking Patterns
                if (uiState.trapFrequency.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Thinking Patterns",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Most common thinking traps identified",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TrapFrequencyChart(trapFrequency = uiState.trapFrequency)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Emotion Trends
                if (uiState.emotionFrequency.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Emotion Trends",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Emotions tracked across entries",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            EmotionFrequencyChart(emotionFrequency = uiState.emotionFrequency)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Export button
                if (onNavigateToExport != null && uiState.totalEntries > 0) {
                    OutlinedCard(
                        onClick = onNavigateToExport,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Export Journal Data",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "JSON, CSV, or Markdown",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (uiState.trendData.isEmpty() && uiState.distribution.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No data yet",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add journal entries with moods to see statistics",
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
private fun ComparisonSection(
    title: String,
    comparison: PeriodComparison
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ComparisonColumn(label = comparison.labelCurrent, summary = comparison.current)
                ComparisonColumn(label = comparison.labelPrevious, summary = comparison.previous)
            }
        }
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
private fun StatItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun JournalStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
