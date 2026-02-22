package com.lifepad.app.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class JournalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    journalRepository: JournalRepository
) : ViewModel() {
    private val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L

    val entry: StateFlow<com.lifepad.app.data.local.entity.JournalEntryEntity?> =
        journalRepository.observeEntry(entryId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
