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

data class ExposureDetailUiState(
    val entry: JournalEntryEntity? = null,
    val data: ExposureJournalData = ExposureJournalData(),
    val emotionsBefore: List<EmotionRating> = emptyList(),
    val emotionsAfter: List<EmotionRating> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ExposureJournalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L

    private val _uiState = MutableStateFlow(ExposureDetailUiState())
    val uiState: StateFlow<ExposureDetailUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            val entry = journalRepository.getEntryById(entryId)
            if (entry != null) {
                val data = if (entry.structuredData.isNotBlank())
                    StructuredDataSerializer.decodeExposure(entry.structuredData)
                else ExposureJournalData()

                val emotionsBefore = journalRepository.getEmotionsByPhase(entryId, "before")
                    .map { EmotionRating(it.emotionName, it.intensity) }
                val emotionsAfter = journalRepository.getEmotionsByPhase(entryId, "after")
                    .map { EmotionRating(it.emotionName, it.intensity) }

                _uiState.update {
                    it.copy(
                        entry = entry,
                        data = data,
                        emotionsBefore = emotionsBefore,
                        emotionsAfter = emotionsAfter,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun deleteEntry() {
        viewModelScope.launch {
            journalRepository.deleteEntry(entryId)
        }
    }
}
