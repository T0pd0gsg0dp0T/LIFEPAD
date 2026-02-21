package com.lifepad.app.journal

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposureJournalDetailScreen(
    onNavigateBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ExposureJournalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exposure Log") },
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
                val dateFormatter = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
                Text(
                    text = dateFormatter.format(Date(uiState.entry!!.entryDate)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Fear
                DetailSection(title = "Fear / Anxiety") {
                    Text(uiState.data.fearDescription.ifBlank { "Not recorded" })
                }

                // Avoidance
                DetailSection(title = "Avoidance Behavior") {
                    Text(uiState.data.avoidanceBehavior.ifBlank { "Not recorded" })
                }

                // SUDS progression
                DetailSection(title = "Distress Levels (SUDS)") {
                    SudsProgressionBar(
                        before = uiState.data.sudsBefore,
                        during = uiState.data.sudsDuring,
                        after = uiState.data.sudsAfter
                    )
                }

                // Plan
                DetailSection(title = "Exposure Plan") {
                    Text(uiState.data.exposurePlan.ifBlank { "Not recorded" })
                }

                // Reflection
                DetailSection(title = "Reflection") {
                    Text(uiState.data.reflection.ifBlank { "Not recorded" })
                }

                // Emotions comparison
                if (uiState.emotionsBefore.isNotEmpty() || uiState.emotionsAfter.isNotEmpty()) {
                    DetailSection(title = "Emotions") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Before", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                uiState.emotionsBefore.forEach {
                                    Text("${it.name}: ${it.intensity}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("After", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                uiState.emotionsAfter.forEach {
                                    Text("${it.name}: ${it.intensity}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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

@Composable
private fun SudsProgressionBar(before: Int, during: Int, after: Int) {
    val green = Color(0xFF00FF41)
    val red = Color(0xFFFF5252)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Before" to before, "During" to during, "After" to after).forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(56.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = value / 100f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(lerp(green, red, value / 100f))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
