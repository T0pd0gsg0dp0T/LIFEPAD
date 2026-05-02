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
import com.lifepad.app.components.TimePickerDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.GuidedStepHeader
import com.lifepad.app.components.MoodSelector
import com.lifepad.app.components.StepperIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavoringJournalScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavoringJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val steps = listOf("Pick a prompt", "Feelings", "Senses", "Savor")

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
                title = { Text("Savoring") },
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
                                title = "Pick a prompt",
                                coaching = "Pick a positive moment from today and slow down on it. Try one of these:\n\n• In the past day, what made you smile?\n• What's a recent event that moved you?\n• What's something you've been underappreciating?\n• Describe a moment, however brief, when you felt calm or at ease."
                            )
                            OutlinedTextField(
                                value = uiState.experience,
                                onValueChange = viewModel::onExperienceChange,
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                placeholder = { Text("Describe the moment") }
                            )
                        }
                        1 -> {
                            GuidedStepHeader(
                                title = "Feelings",
                                coaching = "Select the feelings and sensations the moment brought up. Naming them strengthens the memory."
                            )
                            MoodSelector(
                                selectedMood = uiState.mood,
                                onMoodSelected = viewModel::onMoodChange
                            )
                        }
                        2 -> {
                            GuidedStepHeader(
                                title = "Senses",
                                coaching = "Replaying sensory detail deepens the memory. Close your eyes for a second. What did you see, hear, smell, taste, or feel on your skin?"
                            )
                            OutlinedTextField(
                                value = uiState.sensoryDetails,
                                onValueChange = viewModel::onSensoryDetailsChange,
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                placeholder = { Text("Sights, sounds, smells, textures, tastes...") }
                            )
                        }
                        else -> {
                            GuidedStepHeader(
                                title = "Take a moment to savor and linger on your feelings",
                                coaching = "By strengthening your memory of these positive experiences, you're able to build happiness over time.\n\nHow will you carry this forward — by sharing it, taking a photo, telling someone, or revisiting it later?"
                            )
                            OutlinedTextField(
                                value = uiState.savoring,
                                onValueChange = viewModel::onSavoringChange,
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                placeholder = { Text("e.g., I'll text Mom about it tonight.") }
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
                        val canProceed = if (currentStep == 0) uiState.experience.isNotBlank() else true
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
                            enabled = uiState.experience.isNotBlank() && !uiState.isSaving
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
}
