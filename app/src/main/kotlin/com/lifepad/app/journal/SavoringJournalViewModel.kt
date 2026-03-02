package com.lifepad.app.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.domain.cbt.SavoringJournalData
import com.lifepad.app.domain.cbt.StructuredDataSerializer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavoringJournalUiState(
    val entryId: Long? = null,
    val entryDate: Long = System.currentTimeMillis(),
    val experience: String = "",
    val sensoryDetails: String = "",
    val savoring: String = "",
    val mood: Int = 5,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SavoringJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository
) : ViewModel() {
    private val entryId: Long? = savedStateHandle.get<Long>("entryId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(SavoringJournalUiState(entryId = entryId))
    val uiState: StateFlow<SavoringJournalUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            try {
                if (entryId != null) {
                    val entry = journalRepository.getEntryById(entryId)
                    if (entry != null && entry.structuredData.isNotBlank()) {
                        val data = StructuredDataSerializer.decodeSavoring(entry.structuredData)
                        _uiState.update {
                            it.copy(
                                experience = data.experience,
                                sensoryDetails = data.sensoryDetails,
                                savoring = data.savoring,
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

    fun onExperienceChange(text: String) { _uiState.update { it.copy(experience = text) } }
    fun onSensoryDetailsChange(text: String) { _uiState.update { it.copy(sensoryDetails = text) } }
    fun onSavoringChange(text: String) { _uiState.update { it.copy(savoring = text) } }
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

    fun saveEntry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val state = _uiState.value
                val structuredData = StructuredDataSerializer.encodeSavoring(
                    SavoringJournalData(
                        experience = state.experience,
                        sensoryDetails = state.sensoryDetails,
                        savoring = state.savoring,
                        mood = state.mood
                    )
                )

                val content = buildString {
                    if (state.experience.isNotBlank()) appendLine("Experience: ${state.experience}")
                    if (state.sensoryDetails.isNotBlank()) appendLine("Sensory details: ${state.sensoryDetails}")
                    if (state.savoring.isNotBlank()) appendLine("Savoring: ${state.savoring}")
                }

                val entry = JournalEntryEntity(
                    id = state.entryId ?: 0L,
                    content = content,
                    mood = state.mood,
                    template = "savoring",
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
