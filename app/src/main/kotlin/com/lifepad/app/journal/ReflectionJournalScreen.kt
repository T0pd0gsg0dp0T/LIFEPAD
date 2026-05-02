package com.lifepad.app.journal

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.GuidedStepHeader
import com.lifepad.app.components.MoodSelector
import com.lifepad.app.components.StepperIndicator
import com.lifepad.app.components.TimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflectionJournalScreen(
    onNavigateBack: () -> Unit,
    fromReminder: Boolean = false,
    viewModel: ReflectionJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var showReminderPrompt by rememberSaveable { mutableStateOf(fromReminder) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val steps = listOf("Intention", "Review", "Mood")

    BackHandler {
        if (currentStep > 0) currentStep -= 1 else onNavigateBack()
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

    LaunchedEffect(currentStep) { scrollState.scrollTo(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Reflection") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep -= 1 else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
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

                StepperIndicator(
                    steps = steps,
                    currentStep = currentStep,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (currentStep) {
                        0 -> {
                            GuidedStepHeader(
                                title = "What's your intention?",
                                coaching = "An intention is a compass, not a checklist. Pick one quality or focus you want to bring to today (or tomorrow). Keep it short — one sentence is plenty."
                            )
                            OutlinedTextField(
                                value = uiState.intention,
                                onValueChange = viewModel::onIntentionChange,
                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                placeholder = { Text("e.g., Stay present in conversations today.") }
                            )
                        }
                        1 -> {
                            GuidedStepHeader(
                                title = "Look back at your day",
                                coaching = "Reflection turns experience into learning. Skim today and notice — what worked, what was hard, and one thing you'd do differently."
                            )
                            OutlinedTextField(
                                value = uiState.highlights,
                                onValueChange = viewModel::onHighlightsChange,
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                label = { Text("What went well?") },
                                placeholder = { Text("Wins, moments, kindnesses — large or small.") }
                            )
                            OutlinedTextField(
                                value = uiState.challenges,
                                onValueChange = viewModel::onChallengesChange,
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                label = { Text("What was hard?") },
                                placeholder = { Text("Stuck moments, frustrations, things that drained you.") }
                            )
                            OutlinedTextField(
                                value = uiState.improveTomorrow,
                                onValueChange = viewModel::onImproveTomorrowChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("One thing to try tomorrow") },
                                placeholder = { Text("Tiny adjustments compound — keep it concrete.") }
                            )
                        }
                        else -> {
                            GuidedStepHeader(
                                title = "How do you feel after reflecting?",
                                coaching = "Just reviewing the day shifts something. Take a breath. Where are you on the mood scale right now?"
                            )
                            MoodSelector(
                                selectedMood = uiState.mood,
                                onMoodSelected = viewModel::onMoodChange
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                    }
                    if (currentStep < steps.lastIndex) {
                        val canProceed = if (currentStep == 0) uiState.intention.isNotBlank() else true
                        Button(
                            onClick = { currentStep += 1 },
                            modifier = Modifier.weight(1f),
                            enabled = canProceed
                        ) {
                            Text("Next")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.saveEntry() },
                            modifier = Modifier.weight(1f),
                            enabled = uiState.intention.isNotBlank() && !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Save Entry")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReminderPrompt) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Time for reflection") },
            text = { Text("Please complete your daily reflection now.") },
            confirmButton = {
                TextButton(onClick = { showReminderPrompt = false }) {
                    Text("Start")
                }
            }
        )
    }
}
