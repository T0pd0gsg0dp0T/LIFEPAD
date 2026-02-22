package com.lifepad.app.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.repository.JournalRepository
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
    val errorMessage: String? = null
)

@HiltViewModel
class CheckInJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository
) : ViewModel() {
    private val entryId: Long? = savedStateHandle.get<Long>("entryId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(CheckInJournalUiState(entryId = entryId))
    val uiState: StateFlow<CheckInJournalUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
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
