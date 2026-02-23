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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.EmotionSelector
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
    val scrollState = rememberScrollState()
    val steps = listOf("Fear", "Before", "Plan", "After")

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StepperIndicator(
                    steps = steps,
                    currentStep = currentStep
                )

                when (currentStep) {
                    0 -> {
                        Text(
                            text = "What fear are you facing?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = uiState.fearDescription,
                            onValueChange = { viewModel.onFearDescriptionChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = { Text("Describe the fear or anxiety-provoking situation...") }
                        )
                        OutlinedTextField(
                            value = uiState.avoidanceBehavior,
                            onValueChange = { viewModel.onAvoidanceBehaviorChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("How have you been avoiding this?") },
                            placeholder = { Text("e.g., Not going to social events, avoiding phone calls...") }
                        )
                    }
                    1 -> {
                        Text(
                            text = "Before Exposure",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        MoodSelector(
                            selectedMood = uiState.moodBefore,
                            onMoodSelected = { viewModel.onMoodBeforeChange(it) }
                        )
                        EmotionSelector(
                            emotions = uiState.emotionsBefore,
                            onEmotionsChange = { viewModel.onEmotionsBeforeChange(it) },
                            label = "Emotions before"
                        )
                        SudsRatingBar(
                            label = "Distress level before",
                            value = uiState.sudsBefore,
                            onValueChange = { viewModel.onSudsBeforeChange(it) }
                        )
                    }
                    2 -> {
                        Text(
                            text = "Exposure plan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = uiState.exposurePlan,
                            onValueChange = { viewModel.onExposurePlanChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = { Text("What will you do? Be specific about your steps...") }
                        )
                        SudsRatingBar(
                            label = "Distress level during",
                            value = uiState.sudsDuring,
                            onValueChange = { viewModel.onSudsDuringChange(it) }
                        )
                    }
                    else -> {
                        Text(
                            text = "After Exposure",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        SudsRatingBar(
                            label = "Distress level after",
                            value = uiState.sudsAfter,
                            onValueChange = { viewModel.onSudsAfterChange(it) }
                        )
                        OutlinedTextField(
                            value = uiState.reflection,
                            onValueChange = { viewModel.onReflectionChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            label = { Text("Reflection") },
                            placeholder = { Text("What actually happened? What did you learn?") }
                        )
                        MoodSelector(
                            selectedMood = uiState.moodAfter,
                            onMoodSelected = { viewModel.onMoodAfterChange(it) }
                        )
                        EmotionSelector(
                            emotions = uiState.emotionsAfter,
                            onEmotionsChange = { viewModel.onEmotionsAfterChange(it) },
                            label = "Emotions after"
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { if (currentStep > 0) currentStep -= 1 },
                        enabled = currentStep > 0
                    ) {
                        Text("Back")
                    }

                    if (currentStep < steps.lastIndex) {
                        val canProceed = when (currentStep) {
                            0 -> uiState.fearDescription.isNotBlank()
                            1 -> true
                            2 -> uiState.exposurePlan.isNotBlank()
                            else -> true
                        }
                        Button(
                            onClick = { currentStep += 1 },
                            enabled = canProceed
                        ) {
                            Text("Next")
                        }
                    } else {
                        Button(
                            onClick = { viewModel.saveEntry() },
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

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
