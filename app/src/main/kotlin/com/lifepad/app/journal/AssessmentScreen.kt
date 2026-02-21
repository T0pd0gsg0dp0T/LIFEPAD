package com.lifepad.app.journal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.domain.assessment.AssessmentQuestions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: AssessmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle system back button during assessment
    BackHandler(enabled = uiState.isComplete) {
        viewModel.reset()
        onNavigateBack()
    }
    BackHandler(enabled = uiState.selectedType != null && !uiState.isComplete) {
        viewModel.reset()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.selectedType?.let { AssessmentQuestions.getTitle(it) }
                            ?: "Mental Health Assessment"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.selectedType != null && !uiState.isComplete) {
                            viewModel.reset()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (uiState.selectedType == null) {
                // Type selection
                TypeSelectionContent(onSelectType = viewModel::selectType)
            } else if (!uiState.isComplete) {
                // Questionnaire
                QuestionnaireContent(
                    uiState = uiState,
                    onAnswer = viewModel::answerQuestion,
                    onNext = viewModel::nextQuestion,
                    onPrevious = viewModel::previousQuestion,
                    onSubmit = viewModel::submitAssessment
                )
            }
        }

        // Result dialog
        if (uiState.isComplete) {
            ResultDialog(
                type = uiState.selectedType ?: "",
                score = uiState.score,
                maxScore = uiState.maxScore,
                severity = uiState.severity,
                onDismiss = {
                    viewModel.reset()
                    onNavigateBack()
                }
            )
        }
    }
}

@Composable
private fun TypeSelectionContent(onSelectType: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Choose an assessment",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "These are standardized self-report tools for tracking your mental health over time. " +
                "They are for self-monitoring only and do not constitute a clinical diagnosis.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedCard(onClick = { onSelectType("GAD7") }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "GAD-7",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generalized Anxiety Disorder",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "7 questions about anxiety symptoms over the past 2 weeks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedCard(onClick = { onSelectType("PHQ9") }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PHQ-9",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Patient Health Questionnaire (Depression)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "9 questions about depression symptoms over the past 2 weeks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuestionnaireContent(
    uiState: AssessmentUiState,
    onAnswer: (Int, Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit
) {
    val question = uiState.questions[uiState.currentQuestionIndex]
    val currentAnswer = uiState.answers[uiState.currentQuestionIndex]
    val progress = (uiState.currentQuestionIndex + 1).toFloat() / uiState.questions.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Progress
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Question ${uiState.currentQuestionIndex + 1} of ${uiState.questions.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Over the past 2 weeks, how often have you been bothered by:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = question.text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Answer options
        Column(modifier = Modifier.selectableGroup()) {
            question.options.forEachIndexed { index, option ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .selectable(
                            selected = currentAnswer == index,
                            onClick = { onAnswer(uiState.currentQuestionIndex, index) },
                            role = Role.RadioButton
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentAnswer == index)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentAnswer == index,
                            onClick = null
                        )
                        Text(
                            text = option,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onPrevious,
                enabled = uiState.currentQuestionIndex > 0
            ) {
                Text("Previous")
            }

            if (uiState.currentQuestionIndex < uiState.questions.size - 1) {
                Button(
                    onClick = onNext,
                    enabled = currentAnswer != -1
                ) {
                    Text("Next")
                }
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = uiState.answers.none { it == -1 }
                ) {
                    Text("Submit")
                }
            }
        }
    }
}

@Composable
private fun ResultDialog(
    type: String,
    score: Int,
    maxScore: Int,
    severity: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Assessment Result",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = AssessmentQuestions.getShortTitle(type),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$score / $maxScore",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = severity,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "This result has been saved to your assessment history. " +
                        "This is a self-monitoring tool only and does not constitute a clinical diagnosis. " +
                        "If you have concerns, please consult a healthcare professional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
