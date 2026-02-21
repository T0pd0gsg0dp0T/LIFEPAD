package com.lifepad.app.notepad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.NoteEntity
import com.lifepad.app.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteListUiState(
    val notes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<NoteEntity> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<NoteEntity>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NoteListUiState> = combine(
        noteRepository.getAllNotes(),
        _searchQuery,
        _isSearching,
        _searchResults,
        _errorMessage
    ) { notes, query, searching, results, error ->
        NoteListUiState(
            notes = notes,
            searchQuery = query,
            isSearching = searching,
            searchResults = results,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NoteListUiState()
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
                    _searchResults.value = noteRepository.searchNotes(query)
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

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            try {
                noteRepository.deleteNote(noteId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete note: ${e.message}"
            }
        }
    }

    fun togglePin(noteId: Long) {
        viewModelScope.launch {
            try {
                noteRepository.togglePin(noteId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update note: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
