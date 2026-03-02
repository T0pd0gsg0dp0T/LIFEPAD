package com.lifepad.app.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.data.repository.ReminderRepository
import com.lifepad.app.data.local.entity.ReminderEntity
import com.lifepad.app.domain.cbt.GratitudeJournalData
import com.lifepad.app.domain.cbt.StructuredDataSerializer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

 data class GratitudeJournalUiState(
    val entryId: Long? = null,
    val entryDate: Long = System.currentTimeMillis(),
    val itemOne: String = "",
    val itemTwo: String = "",
    val itemThree: String = "",
    val whyItMattered: String = "",
    val whoHelped: String = "",
    val mood: Int = 5,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val showReminderDialog: Boolean = false,
    val reminders: List<ReminderEntity> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class GratitudeJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {
    private val entryId: Long? = savedStateHandle.get<Long>("entryId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(GratitudeJournalUiState(entryId = entryId))
    val uiState: StateFlow<GratitudeJournalUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
        viewModelScope.launch {
            reminderRepository.getForItem(JournalTemplateReminders.ITEM_TYPE, JournalTemplateReminders.GRATITUDE_ID).collect { reminders ->
                _uiState.update { it.copy(reminders = reminders) }
            }
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            try {
                if (entryId != null) {
                    val entry = journalRepository.getEntryById(entryId)
                    if (entry != null && entry.structuredData.isNotBlank()) {
                        val data = StructuredDataSerializer.decodeGratitude(entry.structuredData)
                        _uiState.update {
                            it.copy(
                                itemOne = data.itemOne,
                                itemTwo = data.itemTwo,
                                itemThree = data.itemThree,
                                whyItMattered = data.whyItMattered,
                                whoHelped = data.whoHelped,
                                mood = data.mood.coerceIn(1, 10),
                                entryDate = entry.entryDate,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load: ${e.message}") }
            }
        }
    }

    fun onItemOneChange(text: String) { _uiState.update { it.copy(itemOne = text) } }
    fun onItemTwoChange(text: String) { _uiState.update { it.copy(itemTwo = text) } }
    fun onItemThreeChange(text: String) { _uiState.update { it.copy(itemThree = text) } }
    fun onWhyItMatteredChange(text: String) { _uiState.update { it.copy(whyItMattered = text) } }
    fun onWhoHelpedChange(text: String) { _uiState.update { it.copy(whoHelped = text) } }
    fun onMoodChange(mood: Int) { _uiState.update { it.copy(mood = mood) } }

    fun onDateChange(dateMillis: Long) {
        val currentCal = java.util.Calendar.getInstance()
        currentCal.timeInMillis = _uiState.value.entryDate
        val hour = currentCal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = currentCal.get(java.util.Calendar.MINUTE)
        val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        utcCal.timeInMillis = dateMillis
        currentCal.set(
            utcCal.get(java.util.Calendar.YEAR),
            utcCal.get(java.util.Calendar.MONTH),
            utcCal.get(java.util.Calendar.DAY_OF_MONTH),
            hour, minute, 0
        )
        currentCal.set(java.util.Calendar.MILLISECOND, 0)
        _uiState.update { it.copy(entryDate = currentCal.timeInMillis) }
    }

    fun onTimeChange(hour: Int, minute: Int) {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = _uiState.value.entryDate
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
        cal.set(java.util.Calendar.MINUTE, minute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        _uiState.update { it.copy(entryDate = cal.timeInMillis) }
    }

    fun toggleReminderDialog() {
        _uiState.update { it.copy(showReminderDialog = !it.showReminderDialog) }
    }

    fun saveReminder(title: String, message: String, triggerTime: Long, repeatInterval: Long) {
        viewModelScope.launch {
            val existing = _uiState.value.reminders
            existing.forEach { reminderRepository.delete(it.id) }
            val reminder = ReminderEntity(
                title = title,
                message = message,
                triggerTime = triggerTime,
                repeatInterval = repeatInterval,
                linkedItemType = JournalTemplateReminders.ITEM_TYPE,
                linkedItemId = JournalTemplateReminders.GRATITUDE_ID
            )
            reminderRepository.save(reminder)
            _uiState.update { it.copy(showReminderDialog = false) }
        }
    }

    fun saveEntry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val state = _uiState.value
                val structuredData = StructuredDataSerializer.encodeGratitude(
                    GratitudeJournalData(
                        itemOne = state.itemOne,
                        itemTwo = state.itemTwo,
                        itemThree = state.itemThree,
                        whyItMattered = state.whyItMattered,
                        whoHelped = state.whoHelped,
                        mood = state.mood
                    )
                )

                val content = buildString {
                    appendLine("Gratitude 1: ${state.itemOne}")
                    appendLine("Gratitude 2: ${state.itemTwo}")
                    appendLine("Gratitude 3: ${state.itemThree}")
                    if (state.whyItMattered.isNotBlank()) appendLine("Why it mattered: ${state.whyItMattered}")
                    if (state.whoHelped.isNotBlank()) appendLine("Who/what helped: ${state.whoHelped}")
                }

                val entry = JournalEntryEntity(
                    id = state.entryId ?: 0L,
                    content = content,
                    mood = state.mood,
                    template = "gratitude",
                    entryDate = state.entryDate,
                    structuredData = structuredData
                )

                val savedId = journalRepository.saveEntry(entry)
                val actualId = state.entryId ?: savedId
                _uiState.update { it.copy(entryId = actualId, isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

}
