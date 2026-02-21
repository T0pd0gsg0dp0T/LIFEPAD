package com.lifepad.app.finance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.ErrorSnackbarHost
import com.lifepad.app.data.local.entity.GoalType
import com.lifepad.app.ui.theme.ExpenseColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoalEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.goalId != null) "Edit Goal" else "New Goal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.goalId != null) {
                        IconButton(onClick = {
                            viewModel.deleteGoal()
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, "Delete", tint = ExpenseColor)
                        }
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                val saved = viewModel.saveGoal()
                                if (saved != null) onNavigateBack()
                            }
                        },
                        enabled = !uiState.isSaving
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        snackbarHost = {
            ErrorSnackbarHost(
                errorMessage = uiState.errorMessage,
                onDismiss = viewModel::clearError
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Goal Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Goal type
            Text("Goal Type", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalType.entries.forEach { type ->
                    val label = when (type) {
                        GoalType.SAVINGS -> "Savings"
                        GoalType.DEBT_PAYOFF -> "Debt Payoff"
                        GoalType.EMERGENCY_FUND -> "Emergency"
                    }
                    FilterChip(
                        selected = uiState.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.targetAmount,
                onValueChange = viewModel::onTargetAmountChange,
                label = { Text("Target Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.currentAmount,
                onValueChange = viewModel::onCurrentAmountChange,
                label = { Text("Current Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.monthlyContribution,
                onValueChange = viewModel::onMonthlyContributionChange,
                label = { Text("Monthly Contribution") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text("This amount will be reserved from your safe-to-spend") },
                singleLine = true
            )

            // Deadline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = if (uiState.deadline != null) "Deadline: ${dateFormat.format(Date(uiState.deadline!!))}"
                    else "Set Deadline (optional)",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (uiState.deadline != null) {
                TextButton(onClick = { viewModel.onDeadlineChange(null) }) {
                    Text("Clear Deadline")
                }
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4
            )
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = uiState.deadline ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.onDeadlineChange(it) }
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
    }
}
