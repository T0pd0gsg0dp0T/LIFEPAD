package com.lifepad.app.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.HashtagChip
import com.lifepad.app.components.MoodIndicator
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.data.local.entity.CategoryType
import com.lifepad.app.components.CategoryIcon
import com.lifepad.app.components.categoryIconForName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.lifepad.app.ui.theme.ExpenseColor
import com.lifepad.app.ui.theme.IncomeColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val bottomSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedCategory = categories.find { it.id == uiState.categoryId }
    val expectedType = if (uiState.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
    val filteredCategories = categories.filter { it.type == expectedType && !it.isArchived }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.transactionId == null) "New Transaction" else "Edit Transaction")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("nav_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.transactionId != null) {
                        IconButton(onClick = {
                            viewModel.deleteTransaction()
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val result = viewModel.saveTransaction()
                                if (result != null) {
                                    onNavigateBack()
                                }
                            }
                        },
                        enabled = uiState.amount.isNotBlank(),
                        modifier = Modifier.testTag("transaction_save")
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .imePadding()
                        .testTag("screen_transaction_editor")
                ) {
                    // Transaction type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.type == TransactionType.EXPENSE,
                            onClick = { viewModel.onTypeChange(TransactionType.EXPENSE) },
                            label = { Text("Expense") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ExpenseColor.copy(alpha = 0.2f),
                                selectedLabelColor = ExpenseColor
                            ),
                            modifier = Modifier.weight(1f)
                                .testTag("transaction_type_expense")
                        )
                        FilterChip(
                            selected = uiState.type == TransactionType.INCOME,
                            onClick = { viewModel.onTypeChange(TransactionType.INCOME) },
                            label = { Text("Income") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IncomeColor.copy(alpha = 0.2f),
                                selectedLabelColor = IncomeColor
                            ),
                            modifier = Modifier.weight(1f)
                                .testTag("transaction_type_income")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Amount
                    OutlinedTextField(
                        value = uiState.amount,
                        onValueChange = viewModel::onAmountChange,
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_amount_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleCategorySelector() }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "Select category",
                            onValueChange = { },
                            label = { Text("Category") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Description") },
                        placeholder = { Text("Add description, use #hashtags") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_description_input"),
                        minLines = 2,
                        maxLines = 4
                    )

                    // Date selector
                    Spacer(modifier = Modifier.height(16.dp))
                    val txDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Change date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = txDateFormat.format(Date(uiState.transactionDate)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Journal entry link
                    Spacer(modifier = Modifier.height(16.dp))
                    val linkedEntry = uiState.linkedEntryPreview
                    if (linkedEntry != null) {
                        val entry = linkedEntry
                        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MoodIndicator(mood = entry.mood)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dateFormat.format(Date(entry.entryDate)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = entry.content.take(80).let { if (entry.content.length > 80) "$it..." else it },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                IconButton(onClick = viewModel::unlinkEntry) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Unlink",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    } else {
                        androidx.compose.material3.OutlinedButton(
                            onClick = viewModel::toggleEntryPicker,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Link to journal entry")
                        }
                    }

                    // Hashtags
                    if (uiState.hashtags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.hashtags.forEach { hashtag ->
                                HashtagChip(name = hashtag.name)
                            }
                        }
                    }
                }

                // Hashtag suggestions
                if (uiState.showHashtagSuggestions) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .heightIn(max = 200.dp),
                        shadowElevation = 8.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column {
                            uiState.hashtagSuggestions.forEach { hashtag ->
                                Text(
                                    text = "#${hashtag.name}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.insertHashtag(hashtag) }
                                        .padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category selector bottom sheet
        if (uiState.showCategorySelector) {
            ModalBottomSheet(
                onDismissRequest = viewModel::toggleCategorySelector,
                sheetState = bottomSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Select Category",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    filteredCategories.forEach { category ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.onCategorySelected(category.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (category.id == uiState.categoryId)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.small
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CategoryIcon(
                                        icon = categoryIconForName(category.icon),
                                        tint = androidx.compose.ui.graphics.Color(category.color)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Date picker dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = uiState.transactionDate
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Entry picker bottom sheet
        if (uiState.showEntryPicker) {
            ModalBottomSheet(
                onDismissRequest = viewModel::toggleEntryPicker,
                sheetState = bottomSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Link Journal Entry",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (uiState.availableEntries.isEmpty()) {
                        Text(
                            text = "No journal entries yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        uiState.availableEntries.forEach { entry ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.onEntryLinked(entry.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MoodIndicator(mood = entry.mood)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dateFormat.format(Date(entry.entryDate)),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = entry.content.take(60).let { if (entry.content.length > 60) "$it..." else it },
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
