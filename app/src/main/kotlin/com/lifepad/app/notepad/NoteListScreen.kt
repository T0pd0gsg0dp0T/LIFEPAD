package com.lifepad.app.notepad

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.ErrorSnackbarHost
import com.lifepad.app.data.local.entity.FolderEntity
import com.lifepad.app.data.local.entity.NoteEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onNavigateBack: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onEditNote: (Long) -> Unit,
    onCreateNote: (Boolean, Long?) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    viewModel: NoteListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFabMenu by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var moveDestinations by remember { mutableStateOf<List<FolderEntity>>(emptyList()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.folderPath.lastOrNull()?.name ?: "Notes",
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (uiState.folderPath.isNotEmpty()) {
                                Text(
                                    text = uiState.folderPath.joinToString(" / ") { it.name },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (uiState.currentFolderId != null) {
                                    viewModel.goUpFolder()
                                } else {
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (uiState.isEditMode) {
                            IconButton(onClick = viewModel::promptMove) {
                                Icon(Icons.Default.Folder, contentDescription = "Move")
                            }
                            IconButton(onClick = viewModel::promptDelete) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                            Text(
                                text = "DONE",
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable { viewModel.toggleEditMode() },
                                style = MaterialTheme.typography.labelLarge
                            )
                        } else {
                            Text(
                                text = "EDIT",
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable { viewModel.toggleEditMode() },
                                style = MaterialTheme.typography.labelLarge
                            )
                            IconButton(
                                onClick = onNavigateToSearch,
                                modifier = Modifier.testTag("nav_search")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showFabMenu = true },
                    modifier = Modifier.testTag("fab_create_note")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create")
                }
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
                    .testTag("screen_notes")
            ) {
                if (uiState.folders.isEmpty() && uiState.notes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No notes yet",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap + to create your first note",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.folders, key = { it.id }) { folder ->
                            FolderItem(
                                folder = folder,
                                isEditMode = uiState.isEditMode,
                                selected = folder.id in uiState.selectedFolderIds,
                                onClick = { viewModel.openFolder(folder.id) },
                                onToggleSelected = { viewModel.toggleFolderSelection(folder.id) },
                                onLongPress = { viewModel.startEditModeWithFolder(folder.id) }
                            )
                        }
                        items(uiState.notes, key = { it.id }) { note ->
                            NoteRow(
                                note = note,
                                isEditMode = uiState.isEditMode,
                                selected = note.id in uiState.selectedNoteIds,
                                onClick = { onNoteClick(note.id) },
                                onEdit = { onEditNote(note.id) },
                                onToggleSelected = { viewModel.toggleNoteSelection(note.id) },
                                onLongPress = { viewModel.startEditModeWithNote(note.id) }
                            )
                        }
                    }
                }
            }
        }

        if (showFabMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                    .clickable { showFabMenu = false }
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                FabMenuButton(
                    label = "New List",
                    icon = Icons.Default.TaskAlt,
                    onClick = {
                        showFabMenu = false
                        onCreateNote(true, uiState.currentFolderId)
                    }
                )
                FabMenuButton(
                    label = "New Note",
                    icon = Icons.Default.Note,
                    onClick = {
                        showFabMenu = false
                        onCreateNote(false, uiState.currentFolderId)
                    }
                )
                FabMenuButton(
                    label = "New Folder",
                    icon = Icons.Default.CreateNewFolder,
                    onClick = {
                        showFabMenu = false
                        viewModel.promptNewFolder()
                    }
                )
            }
        }

        if (uiState.showNewFolderDialog) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.dismissNewFolder()
                    newFolderName = ""
                },
                title = { Text("New folder") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        singleLine = true,
                        placeholder = { Text("Folder name") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createFolder(newFolderName)
                            newFolderName = ""
                        }
                    ) { Text("Create") }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            viewModel.dismissNewFolder()
                            newFolderName = ""
                        }
                    ) { Text("Cancel") }
                }
            )
        }

        if (uiState.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDelete,
                title = { Text("Delete selected") },
                text = { Text("This will delete the selected notes and folders. Notes inside folders will move to root.") },
                confirmButton = {
                    Button(onClick = viewModel::deleteSelected) { Text("Delete") }
                },
                dismissButton = {
                    Button(onClick = viewModel::dismissDelete) { Text("Cancel") }
                }
            )
        }

        if (uiState.showMoveDialog) {
            LaunchedEffect(uiState.showMoveDialog) {
                if (uiState.showMoveDialog) {
                    viewModel.getMoveDestinations { moveDestinations = it }
                }
            }
            AlertDialog(
                onDismissRequest = viewModel::dismissMove,
                title = { Text("Move to folder") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                    ) {
                        Text(
                            text = "Root",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.moveSelected(null) }
                                .padding(vertical = 12.dp)
                        )
                        HorizontalDivider()
                        moveDestinations.forEach { folder ->
                            Text(
                                text = folder.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.moveSelected(folder.id) }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    Button(onClick = viewModel::dismissMove) { Text("Cancel") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(
    note: NoteEntity,
    isEditMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleSelected: () -> Unit,
    onLongPress: () -> Unit
) {
    val firstLine = note.content.lineSequence().firstOrNull()?.trim().orEmpty()
    val subtitle = when {
        note.isChecklist && firstLine.isBlank() -> "(Empty list)"
        firstLine.isBlank() -> "(Empty note)"
        else -> firstLine
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isEditMode) onToggleSelected() else onClick() },
                onLongClick = { if (!isEditMode) onLongPress() }
            )
            .testTag("note_item")
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleSelected() }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onEdit() },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (note.isChecklist) {
                        Icon(
                            Icons.Default.Checklist,
                            contentDescription = "Checklist",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = note.title.ifBlank { "Untitled note" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatDate(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderItem(
    folder: FolderEntity,
    isEditMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleSelected: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isEditMode) onToggleSelected() else onClick() },
                onLongClick = { if (!isEditMode) onLongPress() }
            )
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelected() }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Folder",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FabMenuButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        tonalElevation = 6.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label)
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
