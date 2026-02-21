package com.lifepad.app.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.dao.SearchResult
import com.lifepad.app.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val filterTypes: Set<String> = emptySet(), // Empty = all types
    val errorMessage: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        val initialQuery = savedStateHandle.get<String>("query").orEmpty()
        if (initialQuery.isNotBlank()) {
            onQueryChange(initialQuery)
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        if (query.isNotBlank()) {
            search()
        } else {
            _uiState.update { it.copy(results = emptyList()) }
        }
    }

    fun toggleTypeFilter(type: String) {
        _uiState.update { state ->
            val newFilters = if (type in state.filterTypes) {
                state.filterTypes - type
            } else {
                state.filterTypes + type
            }
            state.copy(filterTypes = newFilters)
        }
        if (_uiState.value.query.isNotBlank()) {
            search()
        }
    }

    private fun search() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val query = _uiState.value.query
                val types = _uiState.value.filterTypes.takeIf { it.isNotEmpty() }

                val results = if (query.startsWith("#")) {
                    // Hashtag search
                    searchRepository.searchByHashtag(query)
                } else {
                    // Full-text search
                    searchRepository.search(query, types)
                }

                _uiState.update {
                    it.copy(
                        results = results,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Search failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                query = "",
                results = emptyList()
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
