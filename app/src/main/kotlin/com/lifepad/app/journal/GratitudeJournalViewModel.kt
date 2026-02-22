package com.lifepad.app.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.repository.JournalRepository
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
    val errorMessage: String? = null
)

@HiltViewModel
class GratitudeJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository
) : ViewModel() {
    private val entryId: Long? = savedStateHandle.get<Long>("entryId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(GratitudeJournalUiState(entryId = entryId))
    val uiState: StateFlow<GratitudeJournalUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
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
