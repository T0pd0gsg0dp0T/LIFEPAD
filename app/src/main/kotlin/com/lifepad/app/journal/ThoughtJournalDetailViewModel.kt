package com.lifepad.app.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.repository.JournalRepository
import com.lifepad.app.domain.cbt.EmotionRating
import com.lifepad.app.domain.cbt.StructuredDataSerializer
import com.lifepad.app.domain.cbt.ThinkingTrap
import com.lifepad.app.domain.cbt.ThoughtJournalData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThoughtDetailUiState(
    val entry: JournalEntryEntity? = null,
    val data: ThoughtJournalData = ThoughtJournalData(),
    val emotionsBefore: List<EmotionRating> = emptyList(),
    val emotionsAfter: List<EmotionRating> = emptyList(),
    val thinkingTraps: List<ThinkingTrap> = emptyList(),
    val moodBefore: Int = 5,
    val moodAfter: Int = 5,
    val isLoading: Boolean = true
)

@HiltViewModel
class ThoughtJournalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L

    private val _uiState = MutableStateFlow(ThoughtDetailUiState())
    val uiState: StateFlow<ThoughtDetailUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            val entry = journalRepository.getEntryById(entryId)
            if (entry != null) {
                val data = if (entry.structuredData.isNotBlank())
                    StructuredDataSerializer.decodeThought(entry.structuredData)
                else ThoughtJournalData()

                val emotionsBefore = journalRepository.getEmotionsByPhase(entryId, "before")
                    .map { EmotionRating(it.emotionName, it.intensity) }
                val emotionsAfter = journalRepository.getEmotionsByPhase(entryId, "after")
                    .map { EmotionRating(it.emotionName, it.intensity) }

                val trapEntities = journalRepository.getTrapsForEntry(entryId).first()
                val traps = trapEntities.mapNotNull { entity ->
                    ThinkingTrap.entries.find { it.name == entity.trapType }
                }

                _uiState.update {
                    it.copy(
                        entry = entry,
                        data = data,
                        emotionsBefore = emotionsBefore,
                        emotionsAfter = emotionsAfter,
                        thinkingTraps = traps,
                        moodBefore = data.moodBefore.coerceIn(1, 10),
                        moodAfter = data.moodAfter.coerceIn(1, 10),
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
