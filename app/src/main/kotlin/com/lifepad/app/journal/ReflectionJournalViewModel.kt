package com.lifepad.app.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.domain.cbt.ReflectionJournalData
import com.lifepad.app.domain.cbt.StructuredDataSerializer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

 data class ReflectionJournalUiState(
    val entryId: Long? = null,
    val entryDate: Long = System.currentTimeMillis(),
    val intention: String = "",
    val highlights: String = "",
    val challenges: String = "",
    val improveTomorrow: String = "",
    val mood: Int = 5,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ReflectionJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository
) : ViewModel() {
    private val entryId: Long? = savedStateHandle.get<Long>("entryId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(ReflectionJournalUiState(entryId = entryId))
    val uiState: StateFlow<ReflectionJournalUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            try {
                if (entryId != null) {
                    val entry = journalRepository.getEntryById(entryId)
                    if (entry != null && entry.structuredData.isNotBlank()) {
                        val data = StructuredDataSerializer.decodeReflection(entry.structuredData)
                        _uiState.update {
                            it.copy(
                                intention = data.intention,
                                highlights = data.highlights,
                                challenges = data.challenges,
                                improveTomorrow = data.improveTomorrow,
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

    fun onIntentionChange(text: String) { _uiState.update { it.copy(intention = text) } }
    fun onHighlightsChange(text: String) { _uiState.update { it.copy(highlights = text) } }
    fun onChallengesChange(text: String) { _uiState.update { it.copy(challenges = text) } }
    fun onImproveTomorrowChange(text: String) { _uiState.update { it.copy(improveTomorrow = text) } }
    fun onMoodChange(mood: Int) { _uiState.update { it.copy(mood = mood) } }

    fun saveEntry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val state = _uiState.value
                val structuredData = StructuredDataSerializer.encodeReflection(
                    ReflectionJournalData(
                        intention = state.intention,
                        highlights = state.highlights,
                        challenges = state.challenges,
                        improveTomorrow = state.improveTomorrow,
                        mood = state.mood
                    )
                )

                val content = buildString {
                    if (state.intention.isNotBlank()) appendLine("Intention: ${state.intention}")
                    if (state.highlights.isNotBlank()) appendLine("Highlights: ${state.highlights}")
                    if (state.challenges.isNotBlank()) appendLine("Challenges: ${state.challenges}")
                    if (state.improveTomorrow.isNotBlank()) appendLine("Improve tomorrow: ${state.improveTomorrow}")
                }

                val entry = JournalEntryEntity(
                    id = state.entryId ?: 0L,
                    content = content,
                    mood = state.mood,
                    template = "reflection",
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
