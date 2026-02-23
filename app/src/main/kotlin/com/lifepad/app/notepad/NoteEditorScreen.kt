package com.lifepad.app.notepad

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.ChecklistItemRow
import com.lifepad.app.components.HashtagChip
import com.lifepad.app.components.InteractiveMarkdownText
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.local.entity.NoteEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.foundation.Image
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    onNavigateBack: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onJournalEntryClick: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var showActionRow by remember { mutableStateOf(true) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showCountDialog by remember { mutableStateOf(false) }
    var showTimestampDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var searchMatches by remember { mutableStateOf<List<IntRange>>(emptyList()) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    var contentField by remember { mutableStateOf(TextFieldValue(uiState.content)) }
    var titleField by remember { mutableStateOf(uiState.title) }
    var newChecklistItem by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.addAttachment(uri, contentField.selection.start)
            }
        }
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            scope.launch {
                snackbarHostState.showSnackbar("Photo permission denied. You can still pick a photo.")
            }
        }
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    LaunchedEffect(uiState.content) {
        if (uiState.content != contentField.text) {
            contentField = contentField.copy(
                text = uiState.content,
                selection = TextRange(uiState.content.length)
            )
        }
    }

    LaunchedEffect(uiState.title) {
        if (uiState.title != titleField) {
            titleField = uiState.title
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message)
            viewModel.clearError()
        }
    }

    BackHandler {
        scope.launch {
            viewModel.saveNow()
            onNavigateBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                viewModel.saveNow()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .testTag("screen_note_editor")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                    scope.launch {
                        viewModel.saveNow()
                        onNavigateBack()
                    }
                },
                    modifier = Modifier.testTag("nav_back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                BasicTextField(
                    value = titleField,
                    onValueChange = {
                        titleField = it
                        viewModel.onTitleChange(it)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("note_title_input"),
                    singleLine = true,
                    readOnly = uiState.doubleTapToEditEnabled && !uiState.isEditing,
                    textStyle = MaterialTheme.typography.titleLarge.merge(
                        TextStyle(color = MaterialTheme.colorScheme.onSurface)
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (titleField.isBlank()) {
                                Text(
                                    text = "Note Title",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (uiState.doubleTapToEditEnabled && uiState.isEditing) {
                    TextButton(onClick = { viewModel.setEditing(false) }) {
                        Text("Done")
                    }
                }

                IconButton(onClick = { showActionRow = !showActionRow }) {
                    Icon(Icons.Default.Build, contentDescription = "Toggle actions")
                }
            }

            if (uiState.isSaving) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...", style = MaterialTheme.typography.labelSmall)
                }
            }

            AnimatedVisibility(visible = showActionRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val text = buildString {
                            if (uiState.title.isNotBlank()) {
                                append(uiState.title)
                                append("\n\n")
                            }
                            append(uiState.content)
                        }
                        clipboard.setText(AnnotatedString(text))
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search document")
                    }
                    IconButton(onClick = { showCountDialog = true }) {
                        Icon(Icons.Default.TextFields, contentDescription = "Word count")
                    }
                    IconButton(onClick = { viewModel.toggleDoubleTapToEdit() }) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = "Double tap to edit",
                            tint = if (uiState.doubleTapToEditEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showTimestampDialog = true }) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Timestamps")
                    }
                    IconButton(onClick = {
                        val shareText = buildString {
                            if (uiState.title.isNotBlank()) {
                                append(uiState.title)
                                append("\n\n")
                            }
                            append(uiState.content)
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share note"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            mediaPermission
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            permissionLauncher.launch(mediaPermission)
                        }
                    }) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add image")
                    }
                }
            }

            HorizontalDivider()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isChecklist) {
                    if (uiState.isEditing) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = {
                                        val text = newChecklistItem.trim()
                                        if (text.isNotBlank()) {
                                            viewModel.addChecklistItem(text)
                                            newChecklistItem = ""
                                        }
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add item")
                                    }
                                    OutlinedTextField(
                                        value = newChecklistItem,
                                        onValueChange = { newChecklistItem = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Add item...") },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            }
                        }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { viewModel.setEditing(true) }
                                )
                            }
                    ) {
                            uiState.checklistItems.forEach { item ->
                                Text(
                                    text = if (item.isChecked) "✓ ${item.text}" else "• ${item.text}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                } else {
                    if (uiState.isEditing) {
                        OutlinedTextField(
                            value = contentField,
                            onValueChange = { newValue ->
                                contentField = newValue
                                if (newValue.text != uiState.content) {
                                    viewModel.onContentChange(newValue.text)
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .testTag("note_content_input"),
                            placeholder = { Text("Start writing... Use #hashtags and [[wikilinks]]") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { viewModel.setEditing(true) }
                                    )
                                }
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
                                    viewModel.openWikilink(
                                        target = target,
                                        onNoteResolved = onNoteClick,
                                        onJournalResolved = onJournalEntryClick,
                                        onTransactionResolved = onTransactionClick
                                    )
                                },
                                onHashtagClick = onHashtagClick
                            )
                            }
                        }
                    }
                }

                if (uiState.isEditing && uiState.showHashtagSuggestions) {
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
                } else if (uiState.isEditing && uiState.showWikilinkSuggestions) {
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

            if (uiState.attachments.isNotEmpty()) {
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.attachments) { attachment ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(context)
                                        .data(File(attachment.filePath))
                                        .size(144)
                                        .build()
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(72.dp)
                            )
                            Text(
                                text = File(attachment.filePath).name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.removeAttachment(attachment) }) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }

            if (uiState.hashtags.isNotEmpty()) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.hashtags.forEach { hashtag ->
                        HashtagChip(
                            name = hashtag.name,
                            onClick = { onHashtagClick(hashtag.name) }
                        )
                    }
                }
            }

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
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Search in note") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            searchMatches = findMatches(uiState.content, it)
                            currentMatchIndex = 0
                        },
                        placeholder = { Text("Search text") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Enter text to search"
                        else "${searchMatches.size} matches"
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            if (searchMatches.isNotEmpty()) {
                                currentMatchIndex =
                                    (currentMatchIndex - 1 + searchMatches.size) % searchMatches.size
                                val range = searchMatches[currentMatchIndex]
                                viewModel.setEditing(true)
                                contentField = contentField.copy(
                                    selection = TextRange(range.first, range.last + 1)
                                )
                            }
                        }
                    ) { Text("Prev") }
                    TextButton(
                        onClick = {
                            if (searchMatches.isNotEmpty()) {
                                currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size
                                val range = searchMatches[currentMatchIndex]
                                viewModel.setEditing(true)
                                contentField = contentField.copy(
                                    selection = TextRange(range.first, range.last + 1)
                                )
                            }
                        }
                    ) { Text("Next") }
                    TextButton(onClick = { showSearchDialog = false }) { Text("Close") }
                }
            }
        )
    }

    if (showCountDialog) {
        val words = countWords(uiState.content)
        val chars = uiState.content.length
        val charsNoSpaces = uiState.content.count { !it.isWhitespace() }
        val lines = uiState.content.lines().size
        AlertDialog(
            onDismissRequest = { showCountDialog = false },
            title = { Text("Counts") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Words: $words")
                    Text("Characters: $chars")
                    Text("Characters (no spaces): $charsNoSpaces")
                    Text("Lines: $lines")
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountDialog = false }) { Text("Close") }
            }
        )
    }

    if (showTimestampDialog) {
        val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        AlertDialog(
            onDismissRequest = { showTimestampDialog = false },
            title = { Text("Timestamps") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Created: ${formatter.format(Date(uiState.createdAt))}")
                    Text("Updated: ${formatter.format(Date(uiState.updatedAt))}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimestampDialog = false }) { Text("Close") }
            }
        )
    }
}

private fun findMatches(content: String, query: String): List<IntRange> {
    if (query.isBlank()) return emptyList()
    val regex = Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
    return regex.findAll(content).map { it.range }.toList()
}

private fun countWords(content: String): Int {
    return Regex("""\b\w+\b""").findAll(content).count()
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
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (expanded) "Hide" else "Show",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
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
