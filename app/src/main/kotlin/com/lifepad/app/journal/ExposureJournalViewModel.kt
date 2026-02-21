package com.lifepad.app.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.domain.cbt.EmotionRating
import com.lifepad.app.domain.cbt.ExposureJournalData
import com.lifepad.app.domain.cbt.StructuredDataSerializer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExposureJournalUiState(
    val entryId: Long? = null,
    val entryDate: Long = System.currentTimeMillis(),
    val fearDescription: String = "",
    val avoidanceBehavior: String = "",
    val emotionsBefore: List<EmotionRating> = emptyList(),
    val moodBefore: Int = 5,
    val sudsBefore: Int = 50,
    val sudsDuring: Int = 50,
    val sudsAfter: Int = 50,
    val exposurePlan: String = "",
    val reflection: String = "",
    val emotionsAfter: List<EmotionRating> = emptyList(),
    val moodAfter: Int = 5,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ExposureJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val entryId: Long? = savedStateHandle.get<Long>("entryId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(ExposureJournalUiState(entryId = entryId))
    val uiState: StateFlow<ExposureJournalUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            try {
                if (entryId != null) {
                    val entry = journalRepository.getEntryById(entryId)
                    if (entry != null && entry.structuredData.isNotBlank()) {
                        val data = StructuredDataSerializer.decodeExposure(entry.structuredData)
                        val emotionsBefore = journalRepository.getEmotionsByPhase(entryId, "before")
                            .map { EmotionRating(it.emotionName, it.intensity) }
                        val emotionsAfter = journalRepository.getEmotionsByPhase(entryId, "after")
                            .map { EmotionRating(it.emotionName, it.intensity) }

                        _uiState.update {
                            it.copy(
                                fearDescription = data.fearDescription,
                                avoidanceBehavior = data.avoidanceBehavior,
                                sudsBefore = data.sudsBefore,
                                sudsDuring = data.sudsDuring,
                                sudsAfter = data.sudsAfter,
                                exposurePlan = data.exposurePlan,
                                reflection = data.reflection,
                                emotionsBefore = emotionsBefore,
                                emotionsAfter = emotionsAfter,
                                moodBefore = data.moodBefore.coerceIn(1, 10),
                                moodAfter = data.moodAfter.coerceIn(1, 10),
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
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to load: ${e.message}")
                }
            }
        }
    }

    fun onFearDescriptionChange(text: String) { _uiState.update { it.copy(fearDescription = text) } }
    fun onAvoidanceBehaviorChange(text: String) { _uiState.update { it.copy(avoidanceBehavior = text) } }
    fun onEmotionsBeforeChange(emotions: List<EmotionRating>) { _uiState.update { it.copy(emotionsBefore = emotions) } }
    fun onMoodBeforeChange(mood: Int) { _uiState.update { it.copy(moodBefore = mood) } }
    fun onSudsBeforeChange(value: Int) { _uiState.update { it.copy(sudsBefore = value) } }
    fun onSudsDuringChange(value: Int) { _uiState.update { it.copy(sudsDuring = value) } }
    fun onSudsAfterChange(value: Int) { _uiState.update { it.copy(sudsAfter = value) } }
    fun onExposurePlanChange(text: String) { _uiState.update { it.copy(exposurePlan = text) } }
    fun onReflectionChange(text: String) { _uiState.update { it.copy(reflection = text) } }
    fun onEmotionsAfterChange(emotions: List<EmotionRating>) { _uiState.update { it.copy(emotionsAfter = emotions) } }
    fun onMoodAfterChange(mood: Int) { _uiState.update { it.copy(moodAfter = mood) } }

    fun saveEntry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val state = _uiState.value

                val structuredData = StructuredDataSerializer.encodeExposure(
                    ExposureJournalData(
                        fearDescription = state.fearDescription,
                        avoidanceBehavior = state.avoidanceBehavior,
                        sudsBefore = state.sudsBefore,
                        sudsDuring = state.sudsDuring,
                        sudsAfter = state.sudsAfter,
                        exposurePlan = state.exposurePlan,
                        reflection = state.reflection,
                        moodBefore = state.moodBefore,
                        moodAfter = state.moodAfter
                    )
                )

                val content = buildString {
                    appendLine("Fear: ${state.fearDescription}")
                    appendLine("Avoidance: ${state.avoidanceBehavior}")
                    appendLine("SUDS: ${state.sudsBefore} -> ${state.sudsDuring} -> ${state.sudsAfter}")
                    appendLine("Plan: ${state.exposurePlan}")
                    appendLine("Reflection: ${state.reflection}")
                }

                val entry = JournalEntryEntity(
                    id = state.entryId ?: 0L,
                    content = content,
                    mood = state.moodAfter,
                    template = "exposure",
                    entryDate = state.entryDate,
                    structuredData = structuredData
                )

                val savedId = journalRepository.saveEntry(entry)
                val actualId = state.entryId ?: savedId

                journalRepository.saveEmotions(actualId, state.emotionsBefore, "before")
                journalRepository.saveEmotions(actualId, state.emotionsAfter, "after")

                _uiState.update { it.copy(entryId = actualId, isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Failed to save: ${e.message}")
                }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
}
