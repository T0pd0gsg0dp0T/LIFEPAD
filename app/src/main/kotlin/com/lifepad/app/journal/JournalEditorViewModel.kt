package com.lifepad.app.journal

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.AttachmentEntity
import com.lifepad.app.data.local.entity.HashtagEntity
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.local.entity.NoteEntity
import com.lifepad.app.data.local.entity.ReminderEntity
import com.lifepad.app.data.repository.HashtagRepository
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.data.repository.NoteRepository
import com.lifepad.app.data.repository.ReminderRepository
import com.lifepad.app.domain.cbt.EmotionRating
import com.lifepad.app.domain.parser.WikilinkParser
import com.lifepad.app.util.FileStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class JournalEditorUiState(
    val entryId: Long? = null,
    val content: String = "",
    val mood: Int = 5,
    val template: String = "free",
    val entryDate: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isPreviewMode: Boolean = false,
    val hashtags: List<HashtagEntity> = emptyList(),
    val hashtagSuggestions: List<HashtagEntity> = emptyList(),
    val showHashtagSuggestions: Boolean = false,
    val linkedNotes: List<NoteEntity> = emptyList(),
    val wikilinkSuggestions: List<NoteEntity> = emptyList(),
    val showWikilinkSuggestions: Boolean = false,
    val showTemplateSelector: Boolean = false,
    val showReminderDialog: Boolean = false,
    val reminders: List<ReminderEntity> = emptyList(),
    val emotionsBefore: List<EmotionRating> = emptyList(),
    val emotionsAfter: List<EmotionRating> = emptyList(),
    val errorMessage: String? = null,
    val attachments: List<AttachmentEntity> = emptyList()
)

@HiltViewModel
class JournalEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository,
    private val noteRepository: NoteRepository,
    private val hashtagRepository: HashtagRepository,
    private val reminderRepository: ReminderRepository,
    private val fileStorageManager: FileStorageManager
) : ViewModel() {

    private val entryId: Long? = savedStateHandle.get<Long>("entryId")?.takeIf { it != 0L }
    private val initialTemplate: String = savedStateHandle.get<String>("template") ?: "free"

    private val _uiState = MutableStateFlow(JournalEditorUiState(entryId = entryId))
    val uiState: StateFlow<JournalEditorUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        if (entryId == null) {
            _uiState.update {
                it.copy(
                    template = initialTemplate,
                    content = if (initialTemplate != "free") getTemplatePrompt(initialTemplate) else it.content
                )
            }
        }
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            try {
                if (entryId != null) {
                    val entry = journalRepository.getEntryById(entryId)
                    if (entry != null) {
                        _uiState.update {
                            it.copy(
                                content = entry.content,
                                mood = entry.mood,
                                template = entry.template,
                                entryDate = entry.entryDate,
                                isPinned = entry.isPinned,
                                isLoading = false
                            )
                        }
                        refreshLinkedNotes(entry.content)
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
                        errorMessage = "Failed to load entry: ${e.message}"
                    )
                }
            }
        }

        if (entryId != null) {
            viewModelScope.launch {
                journalRepository.getHashtagsForEntry(entryId).collect { hashtags ->
                    _uiState.update { it.copy(hashtags = hashtags) }
                }
            }
            viewModelScope.launch {
                reminderRepository.getForItem("ENTRY", entryId).collect { reminders ->
                    _uiState.update { it.copy(reminders = reminders) }
                }
            }
            viewModelScope.launch {
                val before = journalRepository.getEmotionsByPhase(entryId, "before")
                    .map { EmotionRating(it.emotionName, it.intensity) }
                val after = journalRepository.getEmotionsByPhase(entryId, "after")
                    .map { EmotionRating(it.emotionName, it.intensity) }
                _uiState.update { it.copy(emotionsBefore = before, emotionsAfter = after) }
            }
            viewModelScope.launch {
                journalRepository.getAttachmentsForEntry(entryId).collect { attachments ->
                    _uiState.update { it.copy(attachments = attachments) }
                }
            }
        }
    }

    fun onContentChange(content: String) {
        _uiState.update { it.copy(content = content) }
        checkForSuggestions(content)
        refreshLinkedNotes(content)
        scheduleAutoSave()
    }

    fun onMoodChange(mood: Int) {
        _uiState.update { it.copy(mood = mood) }
        scheduleAutoSave()
    }

    fun onDateChange(dateMillis: Long) {
        _uiState.update { it.copy(entryDate = dateMillis) }
        scheduleAutoSave()
    }

    fun onTemplateSelected(template: String) {
        _uiState.update {
            it.copy(
                template = template,
                showTemplateSelector = false,
                content = if (it.content.isBlank()) getTemplatePrompt(template) else it.content
            )
        }
        scheduleAutoSave()
    }

    fun toggleTemplateSelector() {
        _uiState.update { it.copy(showTemplateSelector = !it.showTemplateSelector) }
    }

    fun togglePreviewMode() {
        _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    private fun getTemplatePrompt(template: String): String {
        return when (template) {
            "thought_record" -> """**Situation:**
What happened?

**Thoughts:**
What went through your mind?

**Feelings:**
How did you feel? (Rate intensity 1-10)

**Behavior:**
What did you do?

**Alternative thoughts:**
Is there another way to look at this?"""

            "gratitude" -> """**Today I'm grateful for:**

1.
2.
3.

**Why these matter to me:**
"""

            "reflection" -> """**Morning reflection:**
How do I want to feel today?

**Evening reflection:**
What went well today?

What could be improved?

What did I learn?"""

            "savoring" -> """**Positive experience:**
What happened?

**Sensory details:**
What did you see, hear, feel, smell, taste?

**Emotions:**
How did it make you feel?

**Savoring:**
How can you hold onto this feeling?"""

            "exposure" -> """**Situation/Fear:**
What are you facing?

**SUDS rating before (0-10):**

**Thoughts before:**
What do you expect to happen?

**SUDS rating during (0-10):**

**What actually happened:**

**SUDS rating after (0-10):**

**What I learned:**"""

            "check_in" -> """**Mood right now (1-10):**

**Current situation:**
Where am I? What am I doing?

**Thoughts:**
What's on my mind?

**Actions:**
What will I do next?"""

            "food" -> """**Meal type:** Breakfast / Lunch / Dinner / Snack

**Foods eaten:**
-

**Portions:** Small / Medium / Large

**Hunger before (1-10):**

**Fullness after (1-10):**

**Mood after eating:**

**Notes:**"""

            else -> ""
        }
    }

    private fun checkForSuggestions(content: String) {
        viewModelScope.launch {
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

            val wikilinkMatch = Regex("""\[\[([^\]]*?)$""").find(content)
            if (wikilinkMatch != null) {
                val prefix = wikilinkMatch.groupValues[1].substringBefore('|').substringBefore('#')
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

    private fun refreshLinkedNotes(content: String) {
        viewModelScope.launch {
            val targets = WikilinkParser.extractWikilinks(content)
            if (targets.isEmpty()) {
                _uiState.update { it.copy(linkedNotes = emptyList()) }
                return@launch
            }
            val resolved = targets.mapNotNull { title ->
                noteRepository.getNoteByTitleIgnoreCase(title)
            }.distinctBy { it.id }
            _uiState.update { it.copy(linkedNotes = resolved) }
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
        refreshLinkedNotes(newContent)
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

    fun onEmotionsBeforeChange(emotions: List<EmotionRating>) {
        _uiState.update { it.copy(emotionsBefore = emotions) }
    }

    fun onEmotionsAfterChange(emotions: List<EmotionRating>) {
        _uiState.update { it.copy(emotionsAfter = emotions) }
    }

    fun toggleReminderDialog() {
        _uiState.update { it.copy(showReminderDialog = !it.showReminderDialog) }
    }

    fun saveReminder(title: String, message: String, triggerTime: Long, repeatInterval: Long) {
        val currentEntryId = _uiState.value.entryId ?: return
        viewModelScope.launch {
            val reminder = ReminderEntity(
                title = title,
                message = message,
                triggerTime = triggerTime,
                repeatInterval = repeatInterval,
                linkedItemType = "ENTRY",
                linkedItemId = currentEntryId
            )
            reminderRepository.save(reminder)
            _uiState.update { it.copy(showReminderDialog = false) }
        }
    }

    fun togglePin() {
        _uiState.update { it.copy(isPinned = !it.isPinned) }
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            saveEntry()
        }
    }

    suspend fun saveEntry(): Long? {
        _uiState.update { it.copy(isSaving = true) }

        return try {
            val state = _uiState.value
            val entry = JournalEntryEntity(
                id = state.entryId ?: 0L,
                content = state.content,
                mood = state.mood,
                template = state.template,
                entryDate = state.entryDate,
                isPinned = state.isPinned
            )

            val savedId = journalRepository.saveEntry(entry)
            val actualId = state.entryId ?: savedId

            if (state.template == "savoring") {
                journalRepository.saveEmotions(actualId, state.emotionsBefore, "before")
                journalRepository.saveEmotions(actualId, state.emotionsAfter, "after")
            }

            _uiState.update {
                it.copy(
                    entryId = actualId,
                    isSaving = false
                )
            }
            actualId
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    errorMessage = "Failed to save entry: ${e.message}"
                )
            }
            null
        }
    }

    fun addAttachment(uri: Uri) {
        viewModelScope.launch {
            val entryId = _uiState.value.entryId ?: saveEntry() ?: return@launch
            val filePath = fileStorageManager.saveFile(uri)
            if (filePath != null) {
                journalRepository.addAttachment(entryId, filePath)
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to save attachment.") }
            }
        }
    }

    fun removeAttachment(attachment: AttachmentEntity) {
        viewModelScope.launch {
            fileStorageManager.deleteFile(attachment.filePath)
            journalRepository.deleteAttachment(attachment)
        }
    }

    fun deleteEntry() {
        viewModelScope.launch {
            try {
                entryId?.let { journalRepository.deleteEntry(it) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to delete entry: ${e.message}")
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
                saveEntry()
            }
        }
    }
}
