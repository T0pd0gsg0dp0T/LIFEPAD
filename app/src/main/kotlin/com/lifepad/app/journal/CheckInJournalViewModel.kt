package com.lifepad.app.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.data.repository.ReminderRepository
import com.lifepad.app.data.local.entity.ReminderEntity
import com.lifepad.app.domain.cbt.CheckInJournalData
import com.lifepad.app.domain.cbt.StructuredDataSerializer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckInJournalUiState(
    val entryId: Long? = null,
    val entryDate: Long = System.currentTimeMillis(),
    val mood: Int = 5,
    val energy: Int = 50,
    val stress: Int = 50,
    val notes: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val showReminderDialog: Boolean = false,
    val reminders: List<ReminderEntity> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class CheckInJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {
    private val entryId: Long? = savedStateHandle.get<Long>("entryId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(CheckInJournalUiState(entryId = entryId))
    val uiState: StateFlow<CheckInJournalUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
        viewModelScope.launch {
            reminderRepository.getForItem(JournalTemplateReminders.ITEM_TYPE, JournalTemplateReminders.CHECK_IN_ID).collect { reminders ->
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
                        val data = StructuredDataSerializer.decodeCheckIn(entry.structuredData)
                        _uiState.update {
                            it.copy(
                                mood = data.mood.coerceIn(1, 10),
                                energy = data.energy.coerceIn(0, 100),
                                stress = data.stress.coerceIn(0, 100),
                                notes = data.notes,
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

    fun onMoodChange(mood: Int) { _uiState.update { it.copy(mood = mood) } }
    fun onEnergyChange(value: Int) { _uiState.update { it.copy(energy = value.coerceIn(0, 100)) } }
    fun onStressChange(value: Int) { _uiState.update { it.copy(stress = value.coerceIn(0, 100)) } }
    fun onNotesChange(text: String) { _uiState.update { it.copy(notes = text) } }

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
                linkedItemId = JournalTemplateReminders.CHECK_IN_ID
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
                val structuredData = StructuredDataSerializer.encodeCheckIn(
                    CheckInJournalData(
                        mood = state.mood,
                        energy = state.energy,
                        stress = state.stress,
                        notes = state.notes
                    )
                )

                val content = buildString {
                    appendLine("Mood: ${state.mood}/10")
                    appendLine("Energy: ${state.energy}/100")
                    appendLine("Stress: ${state.stress}/100")
                    if (state.notes.isNotBlank()) appendLine("Notes: ${state.notes}")
                }

                val entry = JournalEntryEntity(
                    id = state.entryId ?: 0L,
                    content = content,
                    mood = state.mood,
                    template = "check_in",
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
