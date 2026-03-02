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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.MoodSelector
import com.lifepad.app.components.ReminderDialog
import com.lifepad.app.components.StepperIndicator
import com.lifepad.app.components.TimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GratitudeJournalScreen(
    onNavigateBack: () -> Unit,
    fromReminder: Boolean = false,
    viewModel: GratitudeJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var showReminderPrompt by rememberSaveable { mutableStateOf(fromReminder) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val steps = listOf("List", "Meaning", "Mood")

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
                title = { Text("Gratitude") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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

                StepperIndicator(steps = steps, currentStep = currentStep)

                when (currentStep) {
                    0 -> {
                        Text("What are three things you're grateful for?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = uiState.itemOne,
                            onValueChange = viewModel::onItemOneChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Gratitude #1") }
                        )
                        OutlinedTextField(
                            value = uiState.itemTwo,
                            onValueChange = viewModel::onItemTwoChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Gratitude #2") }
                        )
                        OutlinedTextField(
                            value = uiState.itemThree,
                            onValueChange = viewModel::onItemThreeChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Gratitude #3") }
                        )
                    }
                    1 -> {
                        Text("Why did one of these matter?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = uiState.whyItMattered,
                            onValueChange = viewModel::onWhyItMatteredChange,
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            placeholder = { Text("Describe why it mattered...") }
                        )
                        OutlinedTextField(
                            value = uiState.whoHelped,
                            onValueChange = viewModel::onWhoHelpedChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Who or what helped?") }
                        )
                    }
                    else -> {
                        Text("How do you feel right now?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        MoodSelector(
                            selectedMood = uiState.mood,
                            onMoodSelected = viewModel::onMoodChange
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { if (currentStep > 0) currentStep -= 1 }, enabled = currentStep > 0) {
                        Text("Back")
                    }

                    if (currentStep < steps.lastIndex) {
                        val canProceed = if (currentStep == 0) {
                            uiState.itemOne.isNotBlank() || uiState.itemTwo.isNotBlank() || uiState.itemThree.isNotBlank()
                        } else true
                        Button(onClick = { currentStep += 1 }, enabled = canProceed) {
                            Text("Next")
                        }
                    } else {
                        val canSave = uiState.itemOne.isNotBlank() || uiState.itemTwo.isNotBlank() || uiState.itemThree.isNotBlank()
                        Button(onClick = { viewModel.saveEntry() }, enabled = canSave && !uiState.isSaving) {
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
            title = { Text("Time for gratitude") },
            text = { Text("Please complete your daily gratitude entry now.") },
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
            initialTitle = "Daily Gratitude",
            initialRepeatOption = com.lifepad.app.components.RepeatOption.DAILY
        )
    }
}
