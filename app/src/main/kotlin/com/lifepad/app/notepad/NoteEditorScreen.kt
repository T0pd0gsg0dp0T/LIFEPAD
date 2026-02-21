package com.lifepad.app.notepad

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.ChecklistItemRow
import com.lifepad.app.components.HashtagChip
import com.lifepad.app.components.InteractiveMarkdownText
import com.lifepad.app.components.ReminderDialog
import com.lifepad.app.data.local.entity.NoteEntity
import com.lifepad.app.data.local.entity.JournalEntryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    onNavigateBack: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onJournalEntryClick: (Long) -> Unit,
    onHashtagClick: (String) -> Unit = {},
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages via Snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Dismiss"
            )
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSaving) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp).width(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
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
                    IconButton(onClick = viewModel::togglePin) {
                        Icon(
                            imageVector = if (uiState.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (uiState.isPinned) "Unpin" else "Pin"
                        )
                    }
                    IconButton(onClick = viewModel::toggleChecklist) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = if (uiState.isChecklist) "Disable checklist" else "Enable checklist",
                            tint = if (uiState.isChecklist)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.noteId != null) {
                        IconButton(
                            onClick = viewModel::toggleReminderDialog,
                            modifier = Modifier.testTag("note_reminder")
                        ) {
                            Icon(
                                imageVector = if (uiState.reminders.isNotEmpty())
                                    Icons.Default.NotificationsActive
                                else
                                    Icons.Outlined.Notifications,
                                contentDescription = "Set reminder"
                            )
                        }
                    }
                    IconButton(onClick = viewModel::togglePreviewMode) {
                        Icon(
                            imageVector = if (uiState.isPreviewMode) Icons.Outlined.Edit else Icons.Default.Preview,
                            contentDescription = if (uiState.isPreviewMode) "Edit" else "Preview"
                        )
                    }
                    if (uiState.noteId != null) {
                        IconButton(onClick = {
                            viewModel.deleteNote()
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
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
                        .imePadding()
                        .testTag("screen_note_editor")
                ) {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::onTitleChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("note_title_input"),
                        placeholder = { Text("Title") },
                        textStyle = MaterialTheme.typography.headlineSmall,
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    // Date selector
                    val noteDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { showDatePicker = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Change date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = noteDateFormat.format(Date(uiState.createdAt)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.isPreviewMode) {
                        // Preview mode - render markdown
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (uiState.content.isBlank()) {
                                Text(
                                    text = "No content",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                InteractiveMarkdownText(
                                    content = uiState.content,
                                    onWikilinkClick = { target ->
                                        viewModel.openWikilink(target, onNoteClick)
                                    },
                                    onHashtagClick = onHashtagClick
                                )
                            }
                        }
                    } else if (uiState.isChecklist) {
                        // Checklist mode
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            items(uiState.checklistItems.size) { index ->
                                val item = uiState.checklistItems[index]
                                ChecklistItemRow(
                                    text = item.text,
                                    isChecked = item.isChecked,
                                    onToggle = { viewModel.toggleChecklistItem(index) },
                                    onTextChange = { viewModel.updateChecklistItemText(index, it) },
                                    onDelete = { viewModel.removeChecklistItem(index) }
                                )
                            }
                            item {
                                // Add new item row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = {
                                        val text = newItemText.ifBlank { "" }
                                        if (text.isNotBlank()) {
                                            viewModel.addChecklistItem(text)
                                            newItemText = ""
                                        }
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add item")
                                    }
                                    OutlinedTextField(
                                        value = newItemText,
                                        onValueChange = { newItemText = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Add item...") },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // Edit mode
                        OutlinedTextField(
                            value = uiState.content,
                            onValueChange = viewModel::onContentChange,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("note_content_input"),
                            placeholder = { Text("Start writing... Use #hashtags and [[wikilinks]]") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }

                    // Hashtags
                    if (uiState.hashtags.isNotEmpty()) {
                        HorizontalDivider()
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.hashtags.forEach { hashtag ->
                                HashtagChip(name = hashtag.name)
                            }
                        }
                    }

                    // Backlinks
                    if (uiState.backlinks.isNotEmpty()) {
                        HorizontalDivider()
                        BacklinkSection(
                            title = "Linked from",
                            notes = uiState.backlinks,
                            expanded = uiState.backlinksExpanded,
                            onToggleExpanded = viewModel::toggleBacklinksExpanded,
                            onNoteClick = onNoteClick
                        )
                    }

                    // Outgoing links
                    if (uiState.outgoingLinks.isNotEmpty()) {
                        HorizontalDivider()
                        BacklinkSection(
                            title = "Links to",
                            notes = uiState.outgoingLinks,
                            expanded = uiState.outgoingLinksExpanded,
                            onToggleExpanded = viewModel::toggleOutgoingLinksExpanded,
                            onNoteClick = onNoteClick
                        )
                    }

                    if (uiState.journalMentions.isNotEmpty()) {
                        HorizontalDivider()
                        JournalMentionSection(
                            entries = uiState.journalMentions,
                            onEntryClick = onJournalEntryClick
                        )
                    }
                }

                // Suggestion dropdowns
                if (uiState.showHashtagSuggestions) {
                    SuggestionDropdown(
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
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

                // Date picker dialog
                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = uiState.createdAt
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

                if (uiState.showWikilinkSuggestions) {
                    SuggestionDropdown(
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        uiState.wikilinkSuggestions.forEach { note ->
                            Text(
                                text = note.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.insertWikilink(note) }
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Reminder dialog
        if (uiState.showReminderDialog) {
            ReminderDialog(
                onDismiss = viewModel::toggleReminderDialog,
                onConfirm = { title, message, triggerTime, repeatInterval ->
                    viewModel.saveReminder(title, message, triggerTime, repeatInterval)
                }
            )
        }
    }
}

@Composable
private fun BacklinkSection(
    title: String,
    notes: List<NoteEntity>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onNoteClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${notes.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                notes.forEach { note ->
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNoteClick(note.id) }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalMentionSection(
    entries: List<JournalEntryEntity>,
    onEntryClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Mentioned in Journal",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        entries.take(5).forEach { entry ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEntryClick(entry.id) }
            ) {
                Text(
                    text = entry.content.take(120).ifBlank { "Journal entry #${entry.id}" },
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SuggestionDropdown(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .heightIn(max = 200.dp),
        shadowElevation = 8.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            content()
        }
    }
}
