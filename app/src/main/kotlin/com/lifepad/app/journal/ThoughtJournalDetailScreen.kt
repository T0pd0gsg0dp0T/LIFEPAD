package com.lifepad.app.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.MoodIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThoughtJournalDetailScreen(
    onNavigateBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ThoughtJournalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thought Record") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.entry != null) {
                        IconButton(onClick = {
                            viewModel.deleteEntry()
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.entry != null) {
                FloatingActionButton(onClick = { onEdit(uiState.entry!!.id) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.entry == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Entry not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date
                val dateFormatter = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
                Text(
                    text = dateFormatter.format(Date(uiState.entry!!.entryDate)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Situation
                DetailCard(title = "Situation") {
                    Text(
                        text = uiState.data.situation.ifBlank { "Not recorded" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Emotions Before
                if (uiState.emotionsBefore.isNotEmpty()) {
                    DetailCard(title = "Emotions Before") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Mood: ", style = MaterialTheme.typography.bodySmall)
                            MoodIndicator(mood = uiState.moodBefore)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.emotionsBefore.forEach { emotion ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(emotion.name, style = MaterialTheme.typography.bodySmall)
                                Text("${emotion.intensity}/100", style = MaterialTheme.typography.bodySmall)
                            }
                            LinearProgressIndicator(
                                progress = { emotion.intensity / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                // Automatic Thoughts
                DetailCard(title = "Automatic Thoughts") {
                    Text(
                        text = uiState.data.automaticThoughts.ifBlank { "Not recorded" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Belief: ${uiState.data.beliefBefore}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Evidence
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailCard(
                        title = "Evidence For",
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = uiState.data.evidenceFor.ifBlank { "None" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    DetailCard(
                        title = "Evidence Against",
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = uiState.data.evidenceAgainst.ifBlank { "None" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Alternative Thought
                DetailCard(title = "Alternative Thought") {
                    Text(
                        text = uiState.data.alternativeThought.ifBlank { "Not recorded" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Belief: ${uiState.data.beliefAfter}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Thinking Traps
                if (uiState.thinkingTraps.isNotEmpty()) {
                    DetailCard(title = "Thinking Traps Identified") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            uiState.thinkingTraps.forEach { trap ->
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(trap.displayName, style = MaterialTheme.typography.labelSmall)
                                    }
                                )
                            }
                        }
                    }
                }

                // Emotions After
                if (uiState.emotionsAfter.isNotEmpty()) {
                    DetailCard(title = "Emotions After") {
                        uiState.emotionsAfter.forEach { emotion ->
                            val before = uiState.emotionsBefore.find { it.name == emotion.name }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(emotion.name, style = MaterialTheme.typography.bodySmall)
                                if (before != null) {
                                    val diff = emotion.intensity - before.intensity
                                    Text(
                                        text = "${before.intensity} -> ${emotion.intensity} (${if (diff > 0) "+$diff" else "$diff"})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text("${emotion.intensity}/100", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            LinearProgressIndicator(
                                progress = { emotion.intensity / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                // Belief comparison
                DetailCard(title = "Summary") {
                    Text(
                        text = "Belief in original thought: ${uiState.data.beliefBefore}% -> ${uiState.data.beliefAfter}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(60.dp)) // FAB clearance
            }
        }
    }
}

@Composable
private fun DetailCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}
