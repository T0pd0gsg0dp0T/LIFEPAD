package com.lifepad.app.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.domain.cbt.FoodJournalData
import com.lifepad.app.domain.cbt.StructuredDataSerializer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoodJournalUiState(
    val entryId: Long? = null,
    val entryDate: Long = System.currentTimeMillis(),
    val meal: String = "",
    val hungerBefore: Int = 50,
    val hungerAfter: Int = 50,
    val moodBefore: Int = 5,
    val moodAfter: Int = 5,
    val reflection: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class FoodJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository
) : ViewModel() {
    private val entryId: Long? = savedStateHandle.get<Long>("entryId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(FoodJournalUiState(entryId = entryId))
    val uiState: StateFlow<FoodJournalUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            try {
                if (entryId != null) {
                    val entry = journalRepository.getEntryById(entryId)
                    if (entry != null && entry.structuredData.isNotBlank()) {
                        val data = StructuredDataSerializer.decodeFood(entry.structuredData)
                        _uiState.update {
                            it.copy(
                                meal = data.meal,
                                hungerBefore = data.hungerBefore.coerceIn(0, 100),
                                hungerAfter = data.hungerAfter.coerceIn(0, 100),
                                moodBefore = data.moodBefore.coerceIn(1, 10),
                                moodAfter = data.moodAfter.coerceIn(1, 10),
                                reflection = data.reflection,
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

    fun onMealChange(text: String) { _uiState.update { it.copy(meal = text) } }
    fun onHungerBeforeChange(value: Int) { _uiState.update { it.copy(hungerBefore = value.coerceIn(0, 100)) } }
    fun onHungerAfterChange(value: Int) { _uiState.update { it.copy(hungerAfter = value.coerceIn(0, 100)) } }
    fun onMoodBeforeChange(value: Int) { _uiState.update { it.copy(moodBefore = value.coerceIn(1, 10)) } }
    fun onMoodAfterChange(value: Int) { _uiState.update { it.copy(moodAfter = value.coerceIn(1, 10)) } }
    fun onReflectionChange(text: String) { _uiState.update { it.copy(reflection = text) } }

    fun saveEntry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val state = _uiState.value
                val structuredData = StructuredDataSerializer.encodeFood(
                    FoodJournalData(
                        meal = state.meal,
                        hungerBefore = state.hungerBefore,
                        hungerAfter = state.hungerAfter,
                        moodBefore = state.moodBefore,
                        moodAfter = state.moodAfter,
                        reflection = state.reflection
                    )
                )

                val content = buildString {
                    if (state.meal.isNotBlank()) appendLine("Meal: ${state.meal}")
                    appendLine("Hunger before: ${state.hungerBefore}/100")
                    appendLine("Hunger after: ${state.hungerAfter}/100")
                    appendLine("Mood before: ${state.moodBefore}/10")
                    appendLine("Mood after: ${state.moodAfter}/10")
                    if (state.reflection.isNotBlank()) appendLine("Reflection: ${state.reflection}")
                }

                val entry = JournalEntryEntity(
                    id = state.entryId ?: 0L,
                    content = content,
                    mood = state.moodAfter,
                    template = "food",
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
