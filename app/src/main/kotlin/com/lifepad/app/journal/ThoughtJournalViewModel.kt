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

data class ThoughtJournalUiState(
    val currentStep: Int = 0,
    val entryId: Long? = null,
    val entryDate: Long = System.currentTimeMillis(),
    // Step 0: Situation
    val situation: String = "",
    // Step 1: Emotions Before
    val emotionsBefore: List<EmotionRating> = emptyList(),
    val moodBefore: Int = 5,
    // Step 2: Automatic Thoughts
    val automaticThoughts: String = "",
    val beliefBefore: Int = 50,
    // Step 3: Evidence
    val evidenceFor: String = "",
    val evidenceAgainst: String = "",
    // Step 4: Reframe
    val alternativeThought: String = "",
    val selectedTraps: Set<ThinkingTrap> = emptySet(),
    val beliefAfter: Int = 50,
    // Step 5: Re-rate
    val emotionsAfter: List<EmotionRating> = emptyList(),
    val moodAfter: Int = 5,
    // Meta
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val stepLabels = listOf("Situation", "Emotions", "Thoughts", "Evidence", "Reframe", "Re-rate")
    val totalSteps = stepLabels.size
}

@HiltViewModel
class ThoughtJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val entryId: Long? = savedStateHandle.get<Long>("entryId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(ThoughtJournalUiState(entryId = entryId))
    val uiState: StateFlow<ThoughtJournalUiState> = _uiState.asStateFlow()

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            try {
                if (entryId != null) {
                    val entry = journalRepository.getEntryById(entryId)
                    if (entry != null && entry.structuredData.isNotBlank()) {
                        val data = StructuredDataSerializer.decodeThought(entry.structuredData)
                        val emotionsBefore = journalRepository.getEmotionsByPhase(entryId, "before")
                            .map { EmotionRating(it.emotionName, it.intensity) }
                        val emotionsAfter = journalRepository.getEmotionsByPhase(entryId, "after")
                            .map { EmotionRating(it.emotionName, it.intensity) }
                        val trapEntities = journalRepository.getTrapsForEntry(entryId)
                            .first()
                        val trapSet = trapEntities.mapNotNull { entity ->
                            ThinkingTrap.entries.find { it.name == entity.trapType }
                        }.toSet()

                        _uiState.update {
                            it.copy(
                                situation = data.situation,
                                automaticThoughts = data.automaticThoughts,
                                evidenceFor = data.evidenceFor,
                                evidenceAgainst = data.evidenceAgainst,
                                alternativeThought = data.alternativeThought,
                                beliefBefore = data.beliefBefore,
                                beliefAfter = data.beliefAfter,
                                emotionsBefore = emotionsBefore,
                                emotionsAfter = emotionsAfter,
                                selectedTraps = trapSet,
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

    fun nextStep() {
        _uiState.update { state ->
            if (state.currentStep < state.totalSteps - 1) {
                val next = state.currentStep + 1
                // Pre-fill emotionsAfter from emotionsBefore when entering step 5
                if (next == 5 && state.emotionsAfter.isEmpty() && state.emotionsBefore.isNotEmpty()) {
                    state.copy(
                        currentStep = next,
                        emotionsAfter = state.emotionsBefore.map { it.copy() },
                        moodAfter = state.moodBefore
                    )
                } else {
                    state.copy(currentStep = next)
                }
            } else state
        }
    }

    fun prevStep() {
        _uiState.update { state ->
            if (state.currentStep > 0) state.copy(currentStep = state.currentStep - 1)
            else state
        }
    }

    fun onSituationChange(text: String) {
        _uiState.update { it.copy(situation = text) }
    }

    fun onDateChange(dateMillis: Long) {
        _uiState.update { it.copy(entryDate = dateMillis) }
    }

    fun onEmotionsBeforeChange(emotions: List<EmotionRating>) {
        _uiState.update { it.copy(emotionsBefore = emotions) }
    }

    fun onMoodBeforeChange(mood: Int) {
        _uiState.update { it.copy(moodBefore = mood) }
    }

    fun onAutomaticThoughtsChange(text: String) {
        _uiState.update { it.copy(automaticThoughts = text) }
    }

    fun onBeliefBeforeChange(value: Int) {
        _uiState.update { it.copy(beliefBefore = value) }
    }

    fun onEvidenceForChange(text: String) {
        _uiState.update { it.copy(evidenceFor = text) }
    }

    fun onEvidenceAgainstChange(text: String) {
        _uiState.update { it.copy(evidenceAgainst = text) }
    }

    fun onAlternativeThoughtChange(text: String) {
        _uiState.update { it.copy(alternativeThought = text) }
    }

    fun onTrapsChange(traps: Set<ThinkingTrap>) {
        _uiState.update { it.copy(selectedTraps = traps) }
    }

    fun onBeliefAfterChange(value: Int) {
        _uiState.update { it.copy(beliefAfter = value) }
    }

    fun onEmotionsAfterChange(emotions: List<EmotionRating>) {
        _uiState.update { it.copy(emotionsAfter = emotions) }
    }

    fun onMoodAfterChange(mood: Int) {
        _uiState.update { it.copy(moodAfter = mood) }
    }

    fun saveEntry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val state = _uiState.value

                val structuredData = StructuredDataSerializer.encodeThought(
                    ThoughtJournalData(
                        situation = state.situation,
                        automaticThoughts = state.automaticThoughts,
                        evidenceFor = state.evidenceFor,
                        evidenceAgainst = state.evidenceAgainst,
                        alternativeThought = state.alternativeThought,
                        beliefBefore = state.beliefBefore,
                        beliefAfter = state.beliefAfter,
                        moodBefore = state.moodBefore,
                        moodAfter = state.moodAfter
                    )
                )

                // Build human-readable content for search/FTS
                val content = buildString {
                    appendLine("Situation: ${state.situation}")
                    if (state.emotionsBefore.isNotEmpty()) {
                        appendLine("Emotions: ${state.emotionsBefore.joinToString(", ") { "${it.name} (${it.intensity})" }}")
                    }
                    appendLine("Automatic thoughts: ${state.automaticThoughts}")
                    appendLine("Evidence for: ${state.evidenceFor}")
                    appendLine("Evidence against: ${state.evidenceAgainst}")
                    appendLine("Alternative thought: ${state.alternativeThought}")
                    if (state.selectedTraps.isNotEmpty()) {
                        appendLine("Thinking traps: ${state.selectedTraps.joinToString(", ") { it.displayName }}")
                    }
                }

                val entry = JournalEntryEntity(
                    id = state.entryId ?: 0L,
                    content = content,
                    mood = state.moodAfter,
                    template = "thought_record",
                    entryDate = state.entryDate,
                    structuredData = structuredData
                )

                val savedId = journalRepository.saveEntry(entry)
                val actualId = state.entryId ?: savedId

                // Save emotions (before + after)
                journalRepository.saveEmotions(actualId, state.emotionsBefore, "before")
                journalRepository.saveEmotions(actualId, state.emotionsAfter, "after")

                // Save thinking traps
                journalRepository.saveThinkingTraps(actualId, state.selectedTraps)

                _uiState.update {
                    it.copy(entryId = actualId, isSaving = false, isSaved = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Failed to save: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
