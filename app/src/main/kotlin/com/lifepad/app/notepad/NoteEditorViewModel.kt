package com.lifepad.app.notepad

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.lifepad.app.data.local.entity.AttachmentEntity
import com.lifepad.app.data.local.entity.HashtagEntity
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.local.entity.NoteEntity
import com.lifepad.app.data.repository.HashtagRepository
import com.lifepad.app.data.local.entity.ReminderEntity
import com.lifepad.app.data.repository.AttachmentRepository
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.data.repository.NoteRepository
import com.lifepad.app.data.repository.ReminderRepository
import com.lifepad.app.domain.parser.ChecklistParser
import com.lifepad.app.settings.SettingsRepository
import com.lifepad.app.util.FileStorageManager
import com.lifepad.app.util.MarkdownImageInserter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import javax.inject.Inject

data class NoteEditorUiState(
    val noteId: Long? = null,
    val title: String = "",
    val content: String = "",
    val folderId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isChecklist: Boolean = false,
    val checklistItems: List<ChecklistParser.ChecklistItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val doubleTapToEditEnabled: Boolean = false,
    val isEditing: Boolean = true,
    val backlinks: List<NoteEntity> = emptyList(),
    val outgoingLinks: List<NoteEntity> = emptyList(),
    val journalMentions: List<JournalEntryEntity> = emptyList(),
    val backlinksExpanded: Boolean = true,
    val outgoingLinksExpanded: Boolean = true,
    val hashtags: List<HashtagEntity> = emptyList(),
    val hashtagSuggestions: List<HashtagEntity> = emptyList(),
    val wikilinkSuggestions: List<NoteEntity> = emptyList(),
    val showHashtagSuggestions: Boolean = false,
    val showWikilinkSuggestions: Boolean = false,
    val showReminderDialog: Boolean = false,
    val reminders: List<ReminderEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val journalRepository: JournalRepository,
    private val financeRepository: FinanceRepository,
    private val hashtagRepository: HashtagRepository,
    private val reminderRepository: ReminderRepository,
    private val attachmentRepository: AttachmentRepository,
    private val settingsRepository: SettingsRepository,
    private val fileStorageManager: FileStorageManager
) : ViewModel() {

    private val noteId: Long? = savedStateHandle.get<Long>("noteId")?.takeIf { it != 0L }
    private val initialIsChecklist: Boolean = savedStateHandle.get<Boolean>("checklist") ?: false
    private val initialFolderId: Long? = savedStateHandle.get<Long>("folderId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(NoteEditorUiState(noteId = noteId, folderId = initialFolderId))
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var journalMentionsJob: Job? = null
    private var hasUnsavedChanges: Boolean = false

    init {
        loadNote()
        viewModelScope.launch {
            settingsRepository.notesDoubleTapToEdit.collect { enabled ->
                _uiState.update {
                    it.copy(
                        doubleTapToEditEnabled = enabled,
                        isEditing = if (enabled) false else true
                    )
                }
            }
        }
    }

    private fun loadNote() {
        viewModelScope.launch {
            try {
                if (noteId != null) {
                    val note = noteRepository.getNoteById(noteId)
                    if (note != null) {
                        val items = if (note.isChecklist) {
                            ChecklistParser.parseChecklist(note.content)
                        } else emptyList()
                        _uiState.update {
                            it.copy(
                                title = note.title,
                                content = note.content,
                                folderId = note.folderId,
                                createdAt = note.createdAt,
                                updatedAt = note.updatedAt,
                                isPinned = note.isPinned,
                                isChecklist = note.isChecklist,
                                checklistItems = items,
                                isLoading = false
                            )
                        }
                        observeJournalMentions(note.title)
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isChecklist = initialIsChecklist,
                            folderId = initialFolderId,
                            isEditing = !it.doubleTapToEditEnabled
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load note: ${e.message}"
                    )
                }
            }
        }

        // Launch separate coroutines for flow collections to avoid blocking
        if (noteId != null) {
            viewModelScope.launch {
                noteRepository.getBacklinksForNote(noteId).collect { backlinks ->
                    _uiState.update { it.copy(backlinks = backlinks) }
                }
            }
            viewModelScope.launch {
                noteRepository.getHashtagsForNote(noteId).collect { hashtags ->
                    _uiState.update { it.copy(hashtags = hashtags) }
                }
            }
            viewModelScope.launch {
                noteRepository.getOutgoingLinksForNote(noteId).collect { outgoing ->
                    _uiState.update { it.copy(outgoingLinks = outgoing) }
                }
            }
            viewModelScope.launch {
                reminderRepository.getForItem("NOTE", noteId).collect { reminders ->
                    _uiState.update { it.copy(reminders = reminders) }
                }
            }
            viewModelScope.launch {
                attachmentRepository.getAttachments(noteId, ITEM_TYPE_NOTE).collect { attachments ->
                    _uiState.update { it.copy(attachments = attachments) }
                }
            }
        }
    }

    fun toggleBacklinksExpanded() {
        _uiState.update { it.copy(backlinksExpanded = !it.backlinksExpanded) }
    }

    fun toggleOutgoingLinksExpanded() {
        _uiState.update { it.copy(outgoingLinksExpanded = !it.outgoingLinksExpanded) }
    }

    fun onDateChange(dateMillis: Long) {
        _uiState.update { it.copy(createdAt = dateMillis) }
        hasUnsavedChanges = true
        scheduleAutoSave()
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
        observeJournalMentions(title)
        hasUnsavedChanges = true
        scheduleAutoSave()
    }

    private fun observeJournalMentions(noteTitle: String) {
        journalMentionsJob?.cancel()
        journalMentionsJob = viewModelScope.launch {
            journalRepository.observeEntriesReferencingNoteTitle(noteTitle).collect { entries ->
                _uiState.update { it.copy(journalMentions = entries) }
            }
        }
    }

    fun onContentChange(content: String) {
        _uiState.update { it.copy(content = content) }
        checkForSuggestions(content)
        hasUnsavedChanges = true
        scheduleAutoSave()
    }

    private fun checkForSuggestions(content: String) {
        viewModelScope.launch {
            // Check for hashtag suggestions
            val hashtagMatch = Regex("""#(\w*)$""").find(content)
            if (hashtagMatch != null) {
                val prefix = hashtagMatch.groupValues[1]
                if (prefix.isNotEmpty()) {
                    val suggestions = hashtagRepository.searchHashtags(prefix)
                    _uiState.update {
                        it.copy(
                            hashtagSuggestions = suggestions,
                            showHashtagSuggestions = suggestions.isNotEmpty()
                        )
                    }
                } else {
                    _uiState.update { it.copy(showHashtagSuggestions = false) }
                }
            } else {
                _uiState.update { it.copy(showHashtagSuggestions = false) }
            }

            // Check for wikilink suggestions
            val wikilinkMatch = Regex("""\[\[([^\]]*?)$""").find(content)
            if (wikilinkMatch != null) {
                val prefix = wikilinkMatch.groupValues[1]
                val suggestions = noteRepository.searchNotesByTitle(prefix)
                _uiState.update {
                    it.copy(
                        wikilinkSuggestions = suggestions,
                        showWikilinkSuggestions = suggestions.isNotEmpty()
                    )
                }
            } else {
                _uiState.update { it.copy(showWikilinkSuggestions = false) }
            }
        }
    }

    fun insertHashtag(hashtag: HashtagEntity) {
        val currentContent = _uiState.value.content
        val newContent = currentContent.replace(
            Regex("""#\w*$"""),
            "#${hashtag.name} "
        )
        _uiState.update {
            it.copy(
                content = newContent,
                showHashtagSuggestions = false
            )
        }
        hasUnsavedChanges = true
        scheduleAutoSave()
    }

    fun insertWikilink(note: NoteEntity) {
        val currentContent = _uiState.value.content
        val newContent = currentContent.replace(
            Regex("""\[\[[^\]]*$"""),
            "[[${note.title}]]"
        )
        _uiState.update {
            it.copy(
                content = newContent,
                showWikilinkSuggestions = false
            )
        }
        hasUnsavedChanges = true
        scheduleAutoSave()
    }

    fun dismissSuggestions() {
        _uiState.update {
            it.copy(
                showHashtagSuggestions = false,
                showWikilinkSuggestions = false
            )
        }
    }

    fun openWikilink(
        target: String,
        onNoteResolved: (Long) -> Unit,
        onJournalResolved: (Long) -> Unit,
        onTransactionResolved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val prefix = target.substringBefore(":", "")
            val value = target.substringAfter(":", target).trim()
            when (prefix.lowercase()) {
                "journal", "entry" -> {
                    resolveJournalLink(value, onJournalResolved)
                }
                "finance", "transaction" -> {
                    resolveTransactionLink(value, onTransactionResolved)
                }
                else -> {
                    val note = noteRepository.getNoteByTitleIgnoreCase(target)
                    if (note != null) {
                        onNoteResolved(note.id)
                    } else {
                        _uiState.update { it.copy(errorMessage = "Linked note not found: $target") }
                    }
                }
            }
        }
    }

    private suspend fun resolveJournalLink(value: String, onResolved: (Long) -> Unit) {
        val id = value.toLongOrNull()
        if (id != null) {
            val entry = journalRepository.getEntryById(id)
            if (entry != null) {
                onResolved(entry.id)
            } else {
                _uiState.update { it.copy(errorMessage = "Journal entry not found: $value") }
            }
            return
        }
        val results = journalRepository.searchEntries(value)
        if (results.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "No journal entries match: $value") }
        } else {
            val entry = results.maxByOrNull { it.updatedAt } ?: results.first()
            onResolved(entry.id)
            if (results.size > 1) {
                _uiState.update {
                    it.copy(errorMessage = "Multiple journal entries matched \"$value\". Opened most recent.")
                }
            }
        }
    }

    private suspend fun resolveTransactionLink(value: String, onResolved: (Long) -> Unit) {
        val id = value.toLongOrNull()
        if (id != null) {
            val transaction = financeRepository.getTransactionById(id)
            if (transaction != null) {
                onResolved(transaction.id)
            } else {
                _uiState.update { it.copy(errorMessage = "Transaction not found: $value") }
            }
            return
        }
        val results = financeRepository.searchTransactions(value)
        if (results.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "No transactions match: $value") }
        } else {
            val transaction = results.maxByOrNull { it.updatedAt } ?: results.first()
            onResolved(transaction.id)
            if (results.size > 1) {
                _uiState.update {
                    it.copy(errorMessage = "Multiple transactions matched \"$value\". Opened most recent.")
                }
            }
        }
    }

    fun toggleReminderDialog() {
        _uiState.update { it.copy(showReminderDialog = !it.showReminderDialog) }
    }

    fun saveReminder(title: String, message: String, triggerTime: Long, repeatInterval: Long) {
        val currentNoteId = _uiState.value.noteId ?: return
        viewModelScope.launch {
            val reminder = ReminderEntity(
                title = title,
                message = message,
                triggerTime = triggerTime,
                repeatInterval = repeatInterval,
                linkedItemType = "NOTE",
                linkedItemId = currentNoteId
            )
            reminderRepository.save(reminder)
            _uiState.update { it.copy(showReminderDialog = false) }
        }
    }

    fun toggleChecklist() {
        val state = _uiState.value
        if (state.isChecklist) {
            // Convert from checklist to plain text
            val plainContent = ChecklistParser.convertFromChecklist(state.content)
            _uiState.update {
                it.copy(
                    isChecklist = false,
                    content = plainContent,
                    checklistItems = emptyList()
                )
            }
        } else {
            // Convert to checklist
            val checklistContent = ChecklistParser.convertToChecklist(state.content)
            val items = ChecklistParser.parseChecklist(checklistContent)
            _uiState.update {
                it.copy(
                    isChecklist = true,
                    content = checklistContent,
                    checklistItems = items
                )
            }
        }
        hasUnsavedChanges = true
        scheduleAutoSave()
    }

    fun toggleChecklistItem(lineIndex: Int) {
        val newContent = ChecklistParser.toggleItem(_uiState.value.content, lineIndex)
        val items = ChecklistParser.parseChecklist(newContent)
        _uiState.update { it.copy(content = newContent, checklistItems = items) }
        hasUnsavedChanges = true
        scheduleAutoSave()
    }

    fun updateChecklistItemText(index: Int, text: String) {
        val items = _uiState.value.checklistItems.toMutableList()
        if (index < 0 || index >= items.size) return
        items[index] = items[index].copy(text = text)
        val newContent = ChecklistParser.toChecklistContent(items)
        _uiState.update { it.copy(content = newContent, checklistItems = items) }
        hasUnsavedChanges = true
        scheduleAutoSave()
    }

    fun addChecklistItem(text: String) {
        val newContent = ChecklistParser.addItem(_uiState.value.content, text)
        val items = ChecklistParser.parseChecklist(newContent)
        _uiState.update { it.copy(content = newContent, checklistItems = items) }
        hasUnsavedChanges = true
        scheduleAutoSave()
    }

    fun removeChecklistItem(lineIndex: Int) {
        val newContent = ChecklistParser.removeItem(_uiState.value.content, lineIndex)
        val items = ChecklistParser.parseChecklist(newContent)
        _uiState.update { it.copy(content = newContent, checklistItems = items) }
        hasUnsavedChanges = true
        scheduleAutoSave()
    }

    fun setEditing(isEditing: Boolean) {
        val state = _uiState.value
        if (!state.doubleTapToEditEnabled && !isEditing) return
        _uiState.update { it.copy(isEditing = isEditing) }
    }

    fun togglePin() {
        _uiState.update { it.copy(isPinned = !it.isPinned) }
        hasUnsavedChanges = true
        scheduleAutoSave()
    }

    fun toggleDoubleTapToEdit() {
        val newValue = !_uiState.value.doubleTapToEditEnabled
        settingsRepository.setNotesDoubleTapToEdit(newValue)
        _uiState.update {
            it.copy(
                doubleTapToEditEnabled = newValue,
                isEditing = !newValue
            )
        }
    }

    fun addAttachment(uri: Uri, cursorPosition: Int) {
        viewModelScope.launch {
            val currentNoteId = ensureNoteId() ?: return@launch
            val filePath = fileStorageManager.saveFile(uri)
            if (filePath != null) {
                attachmentRepository.addAttachment(currentNoteId, ITEM_TYPE_NOTE, filePath)
                val state = _uiState.value
                if (!state.isChecklist) {
                    val current = state.content
                    if (!current.contains(filePath)) {
                        val updated = MarkdownImageInserter.insertImage(current, cursorPosition, filePath)
                        _uiState.update { it.copy(content = updated) }
                        hasUnsavedChanges = true
                        scheduleAutoSave()
                    }
                } else {
                    val updated = MarkdownImageInserter.insertImage(state.content, cursorPosition, filePath)
                    _uiState.update { it.copy(content = updated) }
                    hasUnsavedChanges = true
                    scheduleAutoSave()
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to save attachment.") }
            }
        }
    }

    fun removeAttachment(attachment: AttachmentEntity) {
        viewModelScope.launch {
            fileStorageManager.deleteFile(attachment.filePath)
            attachmentRepository.deleteAttachment(attachment)
            val state = _uiState.value
            if (!state.isChecklist && state.content.contains(attachment.filePath)) {
                val pattern = Regex("""\n?!\[[^\]]*]\(${Regex.escape(attachment.filePath)}\)\n?""")
                val updated = state.content.replace(pattern, "\n").trimEnd()
                _uiState.update { it.copy(content = updated) }
                hasUnsavedChanges = true
                scheduleAutoSave()
            }
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500) // Debounce 500ms
            saveNote()
        }
    }

    suspend fun saveNote(): Long? {
        _uiState.update { it.copy(isSaving = true) }

        return try {
            val state = _uiState.value
            if (!hasUnsavedChanges) {
                _uiState.update { it.copy(isSaving = false) }
                return state.noteId
            }
            if (state.noteId == null &&
                state.title.isBlank() &&
                state.content.isBlank() &&
                state.checklistItems.isEmpty() &&
                state.attachments.isEmpty()
            ) {
                _uiState.update { it.copy(isSaving = false) }
                return null
            }
            val now = System.currentTimeMillis()
            val note = NoteEntity(
                id = state.noteId ?: 0L,
                title = state.title.ifBlank { "Untitled" },
                content = state.content,
                folderId = state.folderId,
                isPinned = state.isPinned,
                isChecklist = state.isChecklist,
                createdAt = state.createdAt,
                updatedAt = now
            )

            val savedId = noteRepository.saveNote(note)
            _uiState.update {
                it.copy(
                    noteId = savedId,
                    updatedAt = now,
                    isSaving = false
                )
            }
            hasUnsavedChanges = false
            savedId
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    errorMessage = "Failed to save note: ${e.message}"
                )
            }
            null
        }
    }

    suspend fun saveNow(): Long? {
        autoSaveJob?.cancel()
        return saveNote()
    }

    private suspend fun ensureNoteId(): Long? {
        val currentId = _uiState.value.noteId
        if (currentId != null) return currentId
        return saveNote()
    }

    fun deleteNote() {
        viewModelScope.launch {
            try {
                noteId?.let { id ->
                    val attachments = attachmentRepository.getAttachments(id, ITEM_TYPE_NOTE).first()
                    attachments.forEach { attachment ->
                        fileStorageManager.deleteFile(attachment.filePath)
                        attachmentRepository.deleteAttachment(attachment)
                    }
                    noteRepository.deleteNote(id)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to delete note: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        // Flush any pending auto-save when navigating away.
        // NonCancellable lets the save finish even after viewModelScope is cancelled.
        if (autoSaveJob?.isActive == true) {
            autoSaveJob?.cancel()
            viewModelScope.launch(NonCancellable) {
                saveNote()
            }
        } else if (hasUnsavedChanges) {
            viewModelScope.launch(NonCancellable) {
                saveNote()
            }
        }
    }

    companion object {
        private const val ITEM_TYPE_NOTE = "note"
    }
}
