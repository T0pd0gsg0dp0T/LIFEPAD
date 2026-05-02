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
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.GuidedStepHeader
import com.lifepad.app.components.MoodSelector
import com.lifepad.app.components.ReminderDialog
import com.lifepad.app.components.StepperIndicator
import com.lifepad.app.components.TimePickerDialog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInJournalScreen(
    onNavigateBack: () -> Unit,
    fromReminder: Boolean = false,
    viewModel: CheckInJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var showReminderPrompt by rememberSaveable { mutableStateOf(fromReminder) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val steps = listOf("Mood", "Energy", "Stress", "Notes")
    val hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val partOfDay = when (hourOfDay) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        else -> "evening"
    }

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
                title = { Text("Check-in") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep -= 1 else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleReminderDialog) {
                        Icon(
                            imageVector = if (uiState.reminders.isNotEmpty())
                                Icons.Filled.NotificationsActive
                            else
                                Icons.Outlined.Notifications,
                            contentDescription = "Set reminder"
                        )
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
                                title = "How are you this $partOfDay?",
                                coaching = "Pick the option that feels closest to where you are right now. Even a quick check-in builds self-awareness over time."
                            )
                            MoodSelector(
                                selectedMood = uiState.mood,
                                onMoodSelected = viewModel::onMoodChange
                            )
                        }
                        1 -> {
                            GuidedStepHeader(
                                title = "How's your energy?",
                                coaching = "Energy and mood travel together but aren't the same. You can be calm and tired, or anxious and wired. Slide to wherever you are right now."
                            )
                            RatingSlider(
                                label = "Energy",
                                value = uiState.energy,
                                onValueChange = viewModel::onEnergyChange
                            )
                        }
                        2 -> {
                            GuidedStepHeader(
                                title = "How tense or stressed do you feel?",
                                coaching = "Naming stress lowers it. Notice your shoulders, jaw, breath. Where are you on the dial right now?"
                            )
                            RatingSlider(
                                label = "Stress",
                                value = uiState.stress,
                                onValueChange = viewModel::onStressChange
                            )
                        }
                        else -> {
                            GuidedStepHeader(
                                title = "What's been on your mind?",
                                coaching = "What people, places, and activities are having an impact on your day? A quick line about what's going on helps you spot patterns over time."
                            )
                            OutlinedTextField(
                                value = uiState.notes,
                                onValueChange = viewModel::onNotesChange,
                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                placeholder = { Text("e.g., Slept poorly. Big meeting at 2pm.") }
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
                        Button(
                            onClick = { currentStep += 1 },
                            modifier = Modifier.weight(1f)
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
            title = { Text("Time for your check-in") },
            text = { Text("Please complete your daily check-in now.") },
            confirmButton = {
                TextButton(onClick = { showReminderPrompt = false }) {
                    Text("Start")
                }
            }
        )
    }

    if (uiState.showReminderDialog) {
        ReminderDialog(
            onDismiss = viewModel::toggleReminderDialog,
            onConfirm = { title, message, triggerTime, repeatInterval ->
                viewModel.saveReminder(title, message, triggerTime, repeatInterval)
            },
            initialTitle = "Daily Check-in",
            initialRepeatOption = com.lifepad.app.components.RepeatOption.DAILY
        )
    }
}

@Composable
private fun RatingSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$label: $value/100",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..100f,
            steps = 9
        )
    }
}
