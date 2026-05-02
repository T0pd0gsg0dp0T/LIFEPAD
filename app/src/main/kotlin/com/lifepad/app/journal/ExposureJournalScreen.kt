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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lifepad.app.components.TimePickerDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.EmotionSelector
import com.lifepad.app.components.GuidedStepHeader
import com.lifepad.app.components.MoodSelector
import com.lifepad.app.components.SudsRatingBar
import com.lifepad.app.components.StepperIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposureJournalScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExposureJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val steps = listOf("Get started", "Broaden", "Target", "Reflect")

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

    LaunchedEffect(currentStep) {
        scrollState.scrollTo(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exposure Journal") },
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
                                title = "Get started",
                                coaching = "Before we get into the details, briefly describe what's on your mind. Well done taking initiative to journal today."
                            )
                            OutlinedTextField(
                                value = uiState.fearDescription,
                                onValueChange = { viewModel.onFearDescriptionChange(it) },
                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                placeholder = { Text("Briefly describe what's on your mind") }
                            )
                            OutlinedTextField(
                                value = uiState.avoidanceBehavior,
                                onValueChange = { viewModel.onAvoidanceBehaviorChange(it) },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                placeholder = { Text("How are you avoiding it? e.g., Staying silent, leaving early, making excuses...") }
                            )
                        }
                        1 -> {
                            GuidedStepHeader(
                                title = "Broaden your perspective",
                                coaching = "Next, let's explore any related avoidance patterns. List 2–3 situations you avoid that might be driven by the same fear. The purpose is to broaden your perspective, which will help you define the underlying fear in the next step.\n\nCapture how you feel right now so you can compare afterward."
                            )
                            Text(
                                text = "Mood right now",
                                style = MaterialTheme.typography.titleSmall
                            )
                            MoodSelector(
                                selectedMood = uiState.moodBefore,
                                onMoodSelected = { viewModel.onMoodBeforeChange(it) }
                            )
                            EmotionSelector(
                                emotions = uiState.emotionsBefore,
                                onEmotionsChange = { viewModel.onEmotionsBeforeChange(it) },
                                label = "Which emotions are showing up? (tap to add, drag to set intensity)"
                            )
                            SudsRatingBar(
                                label = "Distress level (SUDS, 0-100)",
                                value = uiState.sudsBefore,
                                onValueChange = { viewModel.onSudsBeforeChange(it) }
                            )
                        }
                        2 -> {
                            GuidedStepHeader(
                                title = "Capture your target feared outcome",
                                coaching = "Define the core fear in one sentence. Look across the avoidances and feared situation you've outlined and identify the single, underlying fear that ties them all together."
                            )
                            OutlinedTextField(
                                value = uiState.exposurePlan,
                                onValueChange = { viewModel.onExposurePlanChange(it) },
                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                placeholder = { Text("Capture your target feared outcome") }
                            )
                            SudsRatingBar(
                                label = "Distress level during (rate after you do it)",
                                value = uiState.sudsDuring,
                                onValueChange = { viewModel.onSudsDuringChange(it) }
                            )
                        }
                        else -> {
                            GuidedStepHeader(
                                title = "Take a moment to reflect",
                                coaching = "The point isn't to feel zero anxiety — it's to gather evidence that you can tolerate it. Compare what you feared with what really happened."
                            )
                            SudsRatingBar(
                                label = "Distress level after",
                                value = uiState.sudsAfter,
                                onValueChange = { viewModel.onSudsAfterChange(it) }
                            )
                            OutlinedTextField(
                                value = uiState.reflection,
                                onValueChange = { viewModel.onReflectionChange(it) },
                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                placeholder = { Text("What actually happened? What did you learn? What surprised you?") }
                            )
                            Text(
                                text = "Mood now",
                                style = MaterialTheme.typography.titleSmall
                            )
                            MoodSelector(
                                selectedMood = uiState.moodAfter,
                                onMoodSelected = { viewModel.onMoodAfterChange(it) }
                            )
                            EmotionSelector(
                                emotions = uiState.emotionsAfter,
                                onEmotionsChange = { viewModel.onEmotionsAfterChange(it) },
                                label = "Emotions now"
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
                        val canProceed = when (currentStep) {
                            0 -> uiState.fearDescription.isNotBlank()
                            2 -> uiState.exposurePlan.isNotBlank()
                            else -> true
                        }
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
                            enabled = uiState.fearDescription.isNotBlank() && !uiState.isSaving
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
