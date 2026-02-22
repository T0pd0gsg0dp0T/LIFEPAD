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
fun SavoringJournalScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavoringJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentStep by rememberSaveable { mutableStateOf(0) }
    val scrollState = rememberScrollState()
    val steps = listOf("Moment", "Senses", "Savor", "Mood")

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
                        Text(
                            "Describe the moment you want to savor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = uiState.experience,
                            onValueChange = viewModel::onExperienceChange,
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            placeholder = { Text("What happened?") }
                        )
                    }
                    1 -> {
                        Text(
                            "What did you notice with your senses?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = uiState.sensoryDetails,
                            onValueChange = viewModel::onSensoryDetailsChange,
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            placeholder = { Text("Sounds, sights, smells, textures...") }
                        )
                    }
                    2 -> {
                        Text(
                            "How will you savor this moment?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = uiState.savoring,
                            onValueChange = viewModel::onSavoringChange,
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            placeholder = { Text("What will you do to keep it with you?") }
                        )
                    }
                    else -> {
                        Text(
                            "How do you feel right now?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
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
                        val canProceed = if (currentStep == 0) uiState.experience.isNotBlank() else true
                        Button(onClick = { currentStep += 1 }, enabled = canProceed) {
                            Text("Next")
                        }
                    } else {
                        Button(onClick = { viewModel.saveEntry() }, enabled = uiState.experience.isNotBlank() && !uiState.isSaving) {
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
