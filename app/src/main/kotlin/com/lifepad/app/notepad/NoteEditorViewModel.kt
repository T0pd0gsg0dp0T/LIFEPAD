package com.lifepad.app.notepad

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.HashtagEntity
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.local.entity.NoteEntity
import com.lifepad.app.data.repository.HashtagRepository
import com.lifepad.app.data.local.entity.ReminderEntity
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.data.repository.NoteRepository
import com.lifepad.app.data.repository.ReminderRepository
import com.lifepad.app.domain.parser.ChecklistParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteEditorUiState(
    val noteId: Long? = null,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isChecklist: Boolean = false,
    val checklistItems: List<ChecklistParser.ChecklistItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isPreviewMode: Boolean = false,
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
    val errorMessage: String? = null
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val journalRepository: JournalRepository,
    private val hashtagRepository: HashtagRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val noteId: Long? = savedStateHandle.get<Long>("noteId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(NoteEditorUiState(noteId = noteId))
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var journalMentionsJob: Job? = null

    init {
        loadNote()
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
                                createdAt = note.createdAt,
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
                    _uiState.update { it.copy(isLoading = false) }
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
        scheduleAutoSave()
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
        observeJournalMentions(title)
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

    fun openWikilink(targetTitle: String, onResolved: (Long) -> Unit) {
        viewModelScope.launch {
            val note = noteRepository.getNoteByTitleIgnoreCase(targetTitle)
            if (note != null) {
                onResolved(note.id)
            } else {
                _uiState.update { it.copy(errorMessage = "Linked note not found: $targetTitle") }
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
        scheduleAutoSave()
    }

    fun toggleChecklistItem(lineIndex: Int) {
        val newContent = ChecklistParser.toggleItem(_uiState.value.content, lineIndex)
        val items = ChecklistParser.parseChecklist(newContent)
        _uiState.update { it.copy(content = newContent, checklistItems = items) }
        scheduleAutoSave()
    }

    fun updateChecklistItemText(index: Int, text: String) {
        val items = _uiState.value.checklistItems.toMutableList()
        if (index < 0 || index >= items.size) return
        items[index] = items[index].copy(text = text)
        val newContent = ChecklistParser.toChecklistContent(items)
        _uiState.update { it.copy(content = newContent, checklistItems = items) }
        scheduleAutoSave()
    }

    fun addChecklistItem(text: String) {
        val newContent = ChecklistParser.addItem(_uiState.value.content, text)
        val items = ChecklistParser.parseChecklist(newContent)
        _uiState.update { it.copy(content = newContent, checklistItems = items) }
        scheduleAutoSave()
    }

    fun removeChecklistItem(lineIndex: Int) {
        val newContent = ChecklistParser.removeItem(_uiState.value.content, lineIndex)
        val items = ChecklistParser.parseChecklist(newContent)
        _uiState.update { it.copy(content = newContent, checklistItems = items) }
        scheduleAutoSave()
    }

    fun togglePreviewMode() {
        _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    fun togglePin() {
        _uiState.update { it.copy(isPinned = !it.isPinned) }
        scheduleAutoSave()
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
            val note = NoteEntity(
                id = state.noteId ?: 0L,
                title = state.title.ifBlank { "Untitled" },
                content = state.content,
                isPinned = state.isPinned,
                isChecklist = state.isChecklist,
                createdAt = state.createdAt
            )

            val savedId = noteRepository.saveNote(note)
            _uiState.update {
                it.copy(
                    noteId = savedId,
                    isSaving = false
                )
            }
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

    fun deleteNote() {
        viewModelScope.launch {
            try {
                noteId?.let { noteRepository.deleteNote(it) }
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
            viewModelScope.launch(kotlinx.coroutines.NonCancellable) {
                saveNote()
            }
        }
    }
}
