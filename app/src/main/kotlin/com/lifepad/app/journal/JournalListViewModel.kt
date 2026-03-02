package com.lifepad.app.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JournalListUiState(
    val entries: List<JournalEntryEntity> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<JournalEntryEntity> = emptyList(),
    val currentStreak: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class JournalListViewModel @Inject constructor(
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<JournalEntryEntity>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _currentStreak = MutableStateFlow(0)

    private val _statusFlow = combine(_errorMessage, _currentStreak) { error, streak -> error to streak }

    init {
        viewModelScope.launch {
            journalRepository.getAllEntries().collect {
                try {
                    val (current, _) = journalRepository.calculateStreaks()
                    _currentStreak.value = current
                } catch (_: Exception) {}
            }
        }
    }

    val uiState: StateFlow<JournalListUiState> = combine(
        journalRepository.getAllEntries(),
        _searchQuery,
        _isSearching,
        _searchResults,
        _statusFlow
    ) { entries, searchQuery, isSearching, searchResults, (errorMessage, streak) ->
        JournalListUiState(
            entries = entries,
            searchQuery = searchQuery,
            isSearching = isSearching,
            searchResults = searchResults,
            currentStreak = streak,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JournalListUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _isSearching.value = false
            _searchResults.value = emptyList()
        } else {
            _isSearching.value = true
            viewModelScope.launch {
                try {
                    _searchResults.value = when {
                        query.startsWith("#") -> journalRepository.searchEntriesByHashtagQuery(query)
                        query.startsWith("[[") && query.endsWith("]]") -> journalRepository.searchEntriesByWikilinkQuery(query)
                        else -> journalRepository.searchEntries(query)
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Search failed: ${e.message}"
                }
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _isSearching.value = false
        _searchResults.value = emptyList()
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            try {
                journalRepository.deleteEntry(entryId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete entry: ${e.message}"
            }
        }
    }

    fun togglePin(entryId: Long) {
        viewModelScope.launch {
            try {
                journalRepository.togglePin(entryId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update entry: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
