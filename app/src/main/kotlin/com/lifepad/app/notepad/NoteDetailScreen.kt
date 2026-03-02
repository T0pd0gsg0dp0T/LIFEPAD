package com.lifepad.app.notepad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.lifepad.app.components.InteractiveMarkdownText
import com.lifepad.app.domain.parser.ChecklistParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    onNavigateBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenLinkedNote: (Long) -> Unit,
    onOpenLinkedJournal: (Long) -> Unit = {},
    onOpenLinkedTransaction: (Long) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (note != null) {
                        IconButton(onClick = { onEdit(note!!.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    ) { padding ->
        if (note == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Note not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "It may have been deleted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onNavigateBack) {
                        Text("Back to Notes")
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = note!!.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatDate(note!!.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (note!!.content.isBlank()) {
                Text(
                    text = "No content",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (note!!.isChecklist) {
                    val items = ChecklistParser.parseChecklist(note!!.content)
                    items.forEach { item ->
                        Text(
                            text = if (item.isChecked) "✓ ${item.text}" else "• ${item.text}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                } else {
                    InteractiveMarkdownText(
                        content = note!!.content,
                        onWikilinkClick = { target ->
                            viewModel.openWikilink(
                                target = target,
                                onNoteResolved = onOpenLinkedNote,
                                onJournalResolved = onOpenLinkedJournal,
                                onTransactionResolved = onOpenLinkedTransaction
                            )
                        },
                        onHashtagClick = onHashtagClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val inlineImagePaths = extractImagePaths(note!!.content)
            val extraAttachments = attachments.filter { it.filePath !in inlineImagePaths }
            if (extraAttachments.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                extraAttachments.forEach { attachment ->
                    Image(
                        painter = rememberAsyncImagePainter(File(attachment.filePath)),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun extractImagePaths(content: String): Set<String> {
    val regex = Regex("""!\[[^\]]*]\((.*?)\)""")
    return regex.findAll(content).map { it.groupValues[1] }.toSet()
}
