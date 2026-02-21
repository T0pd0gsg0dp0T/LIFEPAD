package com.lifepad.app.journal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.lifepad.app.components.EmotionSelector
import com.lifepad.app.components.HashtagChip
import com.lifepad.app.components.InteractiveMarkdownText
import com.lifepad.app.components.MoodSelector
import com.lifepad.app.components.ReminderDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JournalEditorScreen(
    onNavigateBack: () -> Unit,
    onNoteClick: (Long) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    viewModel: JournalEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomSheetState = rememberModalBottomSheetState()
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.addAttachment(uri)
            }
        }
    )

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
                    Column {
                        Text(
                            text = formatDate(uiState.entryDate) + " \u270E",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showDatePicker = true }
                        )
                        if (uiState.isSaving) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(12.dp).width(12.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Saving...", style = MaterialTheme.typography.labelSmall)
                            }
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
                    IconButton(onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add photo")
                    }
                    IconButton(onClick = viewModel::togglePreviewMode) {
                        Icon(
                            imageVector = if (uiState.isPreviewMode) Icons.Outlined.Edit else Icons.Default.Preview,
                            contentDescription = if (uiState.isPreviewMode) "Edit" else "Preview"
                        )
                    }
                    if (uiState.entryId != null) {
                        IconButton(
                            onClick = viewModel::toggleReminderDialog,
                            modifier = Modifier.testTag("journal_reminder")
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
                    IconButton(onClick = viewModel::togglePin) {
                        Icon(
                            imageVector = if (uiState.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (uiState.isPinned) "Unpin" else "Pin"
                        )
                    }
                    if (uiState.entryId != null) {
                        IconButton(onClick = {
                            viewModel.deleteEntry()
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
                        .testTag("screen_journal_editor")
                ) {
                    // Mood selector
                    MoodSelector(
                        selectedMood = uiState.mood,
                        onMoodSelected = viewModel::onMoodChange,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Template indicator
                    Text(
                        text = "Template: ${uiState.template.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { viewModel.toggleTemplateSelector() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Savoring: emotions before
                    if (uiState.template == "savoring") {
                        EmotionSelector(
                            emotions = uiState.emotionsBefore,
                            onEmotionsChange = viewModel::onEmotionsBeforeChange,
                            label = "How do you feel before?",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.isPreviewMode) {
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
                    } else {
                        // Content editor
                        OutlinedTextField(
                            value = uiState.content,
                            onValueChange = viewModel::onContentChange,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("journal_content_input"),
                            placeholder = { Text("Write your thoughts... Use #hashtags and [[wikilinks]]") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }

                    // Savoring: emotions after
                    if (uiState.template == "savoring") {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        EmotionSelector(
                            emotions = uiState.emotionsAfter,
                            onEmotionsChange = viewModel::onEmotionsAfterChange,
                            label = "How do you feel after?",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Attachments
                    if (uiState.attachments.isNotEmpty()) {
                        HorizontalDivider()
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.attachments) { attachment ->
                                Box {
                                    Image(
                                        painter = rememberAsyncImagePainter(File(attachment.filePath)),
                                        contentDescription = null,
                                        modifier = Modifier.height(100.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeAttachment(attachment) },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove attachment")
                                    }
                                }
                            }
                        }
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

                    if (uiState.linkedNotes.isNotEmpty()) {
                        HorizontalDivider()
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.linkedNotes.forEach { note ->
                                SuggestionAssistChip(
                                    label = "[[${note.title}]]",
                                    onClick = { onNoteClick(note.id) }
                                )
                            }
                        }
                    }
                }

                // Hashtag suggestions
                if (!uiState.isPreviewMode && uiState.showHashtagSuggestions) {
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

                if (!uiState.isPreviewMode && uiState.showWikilinkSuggestions) {
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
        }

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

        // Template selector bottom sheet
        if (uiState.showTemplateSelector) {
            ModalBottomSheet(
                onDismissRequest = viewModel::toggleTemplateSelector,
                sheetState = bottomSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Select Template",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    TemplateOption(
                        title = "Free Writing",
                        description = "Write freely without prompts",
                        isSelected = uiState.template == "free",
                        onClick = { viewModel.onTemplateSelected("free") }
                    )

                    TemplateOption(
                        title = "Thought Record",
                        description = "CBT-style structured reflection",
                        isSelected = uiState.template == "thought_record",
                        onClick = { viewModel.onTemplateSelected("thought_record") }
                    )

                    TemplateOption(
                        title = "Gratitude",
                        description = "Focus on things you're thankful for",
                        isSelected = uiState.template == "gratitude",
                        onClick = { viewModel.onTemplateSelected("gratitude") }
                    )

                    TemplateOption(
                        title = "Daily Reflection",
                        description = "Morning and evening prompts",
                        isSelected = uiState.template == "reflection",
                        onClick = { viewModel.onTemplateSelected("reflection") }
                    )

                    TemplateOption(
                        title = "Savoring",
                        description = "Capture and extend positive experiences",
                        isSelected = uiState.template == "savoring",
                        onClick = { viewModel.onTemplateSelected("savoring") }
                    )

                    TemplateOption(
                        title = "Exposure",
                        description = "Track anxiety hierarchy and SUDS ratings",
                        isSelected = uiState.template == "exposure",
                        onClick = { viewModel.onTemplateSelected("exposure") }
                    )

                    TemplateOption(
                        title = "Check-in",
                        description = "Quick mood and situation snapshot",
                        isSelected = uiState.template == "check_in",
                        onClick = { viewModel.onTemplateSelected("check_in") }
                    )

                    TemplateOption(
                        title = "Food Journal",
                        description = "Track meals, hunger, and mood",
                        isSelected = uiState.template == "food",
                        onClick = { viewModel.onTemplateSelected("food") }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
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
private fun SuggestionAssistChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun TemplateOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
