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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.lifepad.app.data.local.entity.BillFrequency
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.ui.theme.ExpenseColor
import com.lifepad.app.ui.theme.IncomeColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecurringBillEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.billId != null) "Edit Bill" else "New Bill") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.billId != null) {
                        IconButton(onClick = {
                            viewModel.deleteBill()
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, "Delete", tint = ExpenseColor)
                        }
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                val saved = viewModel.saveBill()
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
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            // Type toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.transactionType == TransactionType.EXPENSE,
                    onClick = { viewModel.onTypeChange(TransactionType.EXPENSE) },
                    label = { Text("Expense", color = if (uiState.transactionType == TransactionType.EXPENSE) ExpenseColor else MaterialTheme.colorScheme.onSurface) }
                )
                FilterChip(
                    selected = uiState.transactionType == TransactionType.INCOME,
                    onClick = { viewModel.onTypeChange(TransactionType.INCOME) },
                    label = { Text("Income", color = if (uiState.transactionType == TransactionType.INCOME) IncomeColor else MaterialTheme.colorScheme.onSurface) }
                )
            }

            // Category picker
            val selectedCategory = categories.find { it.id == uiState.categoryId }
            OutlinedTextField(
                value = selectedCategory?.name ?: "Select category",
                onValueChange = {},
                label = { Text("Category") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleCategorySelector() },
                readOnly = true,
                enabled = false
            )

            // Frequency chips
            Text("Frequency", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BillFrequency.entries.forEach { freq ->
                    FilterChip(
                        selected = uiState.frequency == freq,
                        onClick = { viewModel.onFrequencyChange(freq) },
                        label = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Next due date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = "Next Due: ${dateFormat.format(Date(uiState.nextDueDate))}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Reminder toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Reminder", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.enableReminder,
                    onCheckedChange = viewModel::onReminderToggle
                )
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

        // Category bottom sheet
        if (uiState.showCategorySelector) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleCategorySelector() },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Category", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    categories.forEach { cat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.onCategorySelected(cat.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (cat.id == uiState.categoryId)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = cat.name,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        // Date picker dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.nextDueDate)
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
    }
}
