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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.lifepad.app.components.StepperIndicator

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
    val scrollState = rememberScrollState()
    val steps = listOf("Intention", "Review", "Mood")

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
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
                StepperIndicator(steps = steps, currentStep = currentStep)

                when (currentStep) {
                    0 -> {
                        Text("Set an intention for today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = uiState.intention,
                            onValueChange = viewModel::onIntentionChange,
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("What do you want to focus on?") }
                        )
                    }
                    1 -> {
                        Text("Review your day", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = uiState.highlights,
                            onValueChange = viewModel::onHighlightsChange,
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("What went well?") }
                        )
                        OutlinedTextField(
                            value = uiState.challenges,
                            onValueChange = viewModel::onChallengesChange,
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("What was challenging?") }
                        )
                        OutlinedTextField(
                            value = uiState.improveTomorrow,
                            onValueChange = viewModel::onImproveTomorrowChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("One thing to improve tomorrow") }
                        )
                    }
                    else -> {
                        Text("How do you feel now?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                        val canProceed = if (currentStep == 0) uiState.intention.isNotBlank() else true
                        Button(onClick = { currentStep += 1 }, enabled = canProceed) {
                            Text("Next")
                        }
                    } else {
                        Button(onClick = { viewModel.saveEntry() }, enabled = uiState.intention.isNotBlank() && !uiState.isSaving) {
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
