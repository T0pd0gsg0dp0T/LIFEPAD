package com.lifepad.app.journal

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.EmotionSelector
import com.lifepad.app.components.MoodSelector
import com.lifepad.app.components.StepperIndicator
import com.lifepad.app.components.ThinkingTrapSelector
import com.lifepad.app.components.TimePickerDialog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThoughtJournalScreen(
    onNavigateBack: () -> Unit,
    viewModel: ThoughtJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    BackHandler {
        if (uiState.currentStep > 0) {
            viewModel.prevStep()
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thought Journal") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentStep > 0) viewModel.prevStep()
                        else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // Date picker dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = uiState.entryDate
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showTimePicker) {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = uiState.entryDate
            TimePickerDialog(
                initialHour = cal.get(java.util.Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(java.util.Calendar.MINUTE),
                onConfirm = { hour, minute ->
                    viewModel.onTimeChange(hour, minute)
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false }
            )
        }

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
            ) {
                // Date/time selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(uiState.entryDate)))
                    }
                    TextButton(onClick = { showTimePicker = true }) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(uiState.entryDate)))
                    }
                }

                // Stepper indicator
                StepperIndicator(
                    steps = uiState.stepLabels,
                    currentStep = uiState.currentStep,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Step content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (uiState.currentStep) {
                        0 -> SituationStep(uiState, viewModel)
                        1 -> EmotionsBeforeStep(uiState, viewModel)
                        2 -> AutomaticThoughtsStep(uiState, viewModel)
                        3 -> EvidenceStep(uiState, viewModel)
                        4 -> ReframeStep(uiState, viewModel)
                        5 -> ReRateStep(uiState, viewModel)
                    }
                }

                // Navigation buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.currentStep > 0) {
                        OutlinedButton(
                            onClick = { viewModel.prevStep() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                    }
                    if (uiState.currentStep < uiState.totalSteps - 1) {
                        Button(
                            onClick = { viewModel.nextStep() },
                            modifier = Modifier.weight(1f),
                            enabled = isStepValid(uiState)
                        ) {
                            Text("Next")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.saveEntry() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isStepValid(state: ThoughtJournalUiState): Boolean {
    return when (state.currentStep) {
        0 -> state.situation.isNotBlank()
        1 -> true // Mood always has a default
        2 -> state.automaticThoughts.isNotBlank()
        3 -> true // Evidence is optional
        4 -> true // Reframe is optional
        5 -> true
        else -> true
    }
}

@Composable
private fun SituationStep(
    uiState: ThoughtJournalUiState,
    viewModel: ThoughtJournalViewModel
) {
    Text(
        text = "What happened?",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Describe the situation that triggered your thoughts and feelings.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = uiState.situation,
        onValueChange = { viewModel.onSituationChange(it) },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        placeholder = { Text("e.g., I was at a meeting and my boss criticized my work in front of everyone...") }
    )
}

@Composable
private fun EmotionsBeforeStep(
    uiState: ThoughtJournalUiState,
    viewModel: ThoughtJournalViewModel
) {
    Text(
        text = "How did you feel?",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Rate your overall mood and select the emotions you experienced.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    MoodSelector(
        selectedMood = uiState.moodBefore,
        onMoodSelected = { viewModel.onMoodBeforeChange(it) }
    )

    Spacer(modifier = Modifier.height(8.dp))

    EmotionSelector(
        emotions = uiState.emotionsBefore,
        onEmotionsChange = { viewModel.onEmotionsBeforeChange(it) },
        label = "Specific Emotions (tap to select, adjust intensity)"
    )
}

@Composable
private fun AutomaticThoughtsStep(
    uiState: ThoughtJournalUiState,
    viewModel: ThoughtJournalViewModel
) {
    Text(
        text = "What went through your mind?",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Write down the automatic thoughts that came to mind in that situation.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    OutlinedTextField(
        value = uiState.automaticThoughts,
        onValueChange = { viewModel.onAutomaticThoughtsChange(it) },
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        placeholder = { Text("e.g., Everyone thinks I'm incompetent. I'll never be good enough...") }
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "How much do you believe this thought?",
        style = MaterialTheme.typography.bodyMedium
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            value = uiState.beliefBefore.toFloat(),
            onValueChange = { viewModel.onBeliefBeforeChange(it.roundToInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${uiState.beliefBefore}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun EvidenceStep(
    uiState: ThoughtJournalUiState,
    viewModel: ThoughtJournalViewModel
) {
    Text(
        text = "Examine the evidence",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Look at the facts objectively, not your feelings about them.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedTextField(
        value = uiState.evidenceFor,
        onValueChange = { viewModel.onEvidenceForChange(it) },
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        label = { Text("Evidence supporting this thought") },
        placeholder = { Text("What facts support this automatic thought?") }
    )

    OutlinedTextField(
        value = uiState.evidenceAgainst,
        onValueChange = { viewModel.onEvidenceAgainstChange(it) },
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        label = { Text("Evidence against this thought") },
        placeholder = { Text("What facts contradict this thought?") }
    )
}

@Composable
private fun ReframeStep(
    uiState: ThoughtJournalUiState,
    viewModel: ThoughtJournalViewModel
) {
    Text(
        text = "Reframe your thinking",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Based on the evidence, write a more balanced alternative thought.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedTextField(
        value = uiState.alternativeThought,
        onValueChange = { viewModel.onAlternativeThoughtChange(it) },
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        placeholder = { Text("e.g., One criticism doesn't mean I'm incompetent. I've done good work before...") }
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "How much do you believe the alternative thought?",
        style = MaterialTheme.typography.bodyMedium
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            value = uiState.beliefAfter.toFloat(),
            onValueChange = { viewModel.onBeliefAfterChange(it.roundToInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${uiState.beliefAfter}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    ThinkingTrapSelector(
        selectedTraps = uiState.selectedTraps,
        onTrapsChange = { viewModel.onTrapsChange(it) }
    )
}

@Composable
private fun ReRateStep(
    uiState: ThoughtJournalUiState,
    viewModel: ThoughtJournalViewModel
) {
    Text(
        text = "How do you feel now?",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "After examining the evidence and reframing, re-rate your emotions.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    MoodSelector(
        selectedMood = uiState.moodAfter,
        onMoodSelected = { viewModel.onMoodAfterChange(it) }
    )

    Spacer(modifier = Modifier.height(8.dp))

    EmotionSelector(
        emotions = uiState.emotionsAfter,
        onEmotionsChange = { viewModel.onEmotionsAfterChange(it) },
        label = "Re-rate your emotions"
    )

    // Before / After comparison
    if (uiState.emotionsBefore.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Before vs After",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Mood: ${uiState.moodBefore} -> ${uiState.moodAfter}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val moodDiff = uiState.moodAfter - uiState.moodBefore
                    Text(
                        text = if (moodDiff > 0) "+$moodDiff" else "$moodDiff",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (moodDiff >= 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Belief in original thought: ${uiState.beliefBefore}% -> ${uiState.beliefAfter}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
