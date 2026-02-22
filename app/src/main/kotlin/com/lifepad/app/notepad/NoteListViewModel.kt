package com.lifepad.app.notepad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.FolderEntity
import com.lifepad.app.data.local.entity.NoteEntity
import com.lifepad.app.data.repository.AttachmentRepository
import com.lifepad.app.data.repository.FolderRepository
import com.lifepad.app.data.repository.NoteRepository
import com.lifepad.app.util.FileStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteListUiState(
    val notes: List<NoteEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val currentFolderId: Long? = null,
    val folderPath: List<FolderEntity> = emptyList(),
    val isEditMode: Boolean = false,
    val selectedNoteIds: Set<Long> = emptySet(),
    val selectedFolderIds: Set<Long> = emptySet(),
    val errorMessage: String? = null,
    val showMoveDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showNewFolderDialog: Boolean = false
)

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository,
    private val attachmentRepository: AttachmentRepository,
    private val fileStorageManager: FileStorageManager
) : ViewModel() {

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isEditMode = MutableStateFlow(false)
    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _selectedFolderIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _folderPath = MutableStateFlow<List<FolderEntity>>(emptyList())
    private val _showMoveDialog = MutableStateFlow(false)
    private val _showDeleteDialog = MutableStateFlow(false)
    private val _showNewFolderDialog = MutableStateFlow(false)

    private val notesFlow = _currentFolderId.flatMapLatest { folderId ->
        if (folderId == null) noteRepository.getRootNotes()
        else noteRepository.getNotesByFolder(folderId)
    }

    private val foldersFlow = _currentFolderId.flatMapLatest { folderId ->
        if (folderId == null) folderRepository.getRootFolders()
        else folderRepository.getChildFolders(folderId)
    }

    private val allFoldersFlow = folderRepository.getAllFolders()

    val uiState: StateFlow<NoteListUiState> = combine(
        notesFlow,
        foldersFlow,
        _currentFolderId,
        _folderPath,
        _isEditMode,
        _selectedNoteIds,
        _selectedFolderIds,
        _errorMessage,
        _showMoveDialog,
        _showDeleteDialog,
        _showNewFolderDialog
    ) { values ->
        val notes = values[0] as List<NoteEntity>
        val folders = values[1] as List<FolderEntity>
        val currentFolderId = values[2] as Long?
        val folderPath = values[3] as List<FolderEntity>
        val isEditMode = values[4] as Boolean
        val selectedNotes = values[5] as Set<Long>
        val selectedFolders = values[6] as Set<Long>
        val error = values[7] as String?
        val showMove = values[8] as Boolean
        val showDelete = values[9] as Boolean
        val showNewFolder = values[10] as Boolean
        NoteListUiState(
            notes = notes,
            folders = folders,
            currentFolderId = currentFolderId,
            folderPath = folderPath,
            isEditMode = isEditMode,
            selectedNoteIds = selectedNotes,
            selectedFolderIds = selectedFolders,
            errorMessage = error,
            showMoveDialog = showMove,
            showDeleteDialog = showDelete,
            showNewFolderDialog = showNewFolder
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NoteListUiState()
    )

    init {
        refreshFolderPath()
    }

    fun openFolder(folderId: Long) {
        _currentFolderId.value = folderId
        clearSelection()
        refreshFolderPath()
    }

    fun goUpFolder() {
        val current = _currentFolderId.value ?: return
        viewModelScope.launch {
            val folder = folderRepository.getFolderById(current)
            _currentFolderId.value = folder?.parentId
            clearSelection()
            refreshFolderPath()
        }
    }

    fun goToRoot() {
        _currentFolderId.value = null
        clearSelection()
        refreshFolderPath()
    }

    private fun refreshFolderPath() {
        viewModelScope.launch {
            val path = mutableListOf<FolderEntity>()
            var currentId = _currentFolderId.value
            while (currentId != null) {
                val folder = folderRepository.getFolderById(currentId) ?: break
                path.add(folder)
                currentId = folder.parentId
            }
            _folderPath.value = path.reversed()
        }
    }

    fun toggleEditMode() {
        val enabled = !_isEditMode.value
        _isEditMode.value = enabled
        if (!enabled) clearSelection()
    }

    fun toggleNoteSelection(noteId: Long) {
        _selectedNoteIds.update { current ->
            if (noteId in current) current - noteId else current + noteId
        }
    }

    fun toggleFolderSelection(folderId: Long) {
        _selectedFolderIds.update { current ->
            if (folderId in current) current - folderId else current + folderId
        }
    }

    fun startEditModeWithNote(noteId: Long) {
        if (!_isEditMode.value) {
            _isEditMode.value = true
        }
        _selectedNoteIds.update { it + noteId }
    }

    fun startEditModeWithFolder(folderId: Long) {
        if (!_isEditMode.value) {
            _isEditMode.value = true
        }
        _selectedFolderIds.update { it + folderId }
    }

    private fun clearSelection() {
        _selectedNoteIds.value = emptySet()
        _selectedFolderIds.value = emptySet()
    }

    fun promptMove() {
        if (_selectedNoteIds.value.isNotEmpty() || _selectedFolderIds.value.isNotEmpty()) {
            _showMoveDialog.value = true
        }
    }

    fun dismissMove() {
        _showMoveDialog.value = false
    }

    fun promptDelete() {
        if (_selectedNoteIds.value.isNotEmpty() || _selectedFolderIds.value.isNotEmpty()) {
            _showDeleteDialog.value = true
        }
    }

    fun dismissDelete() {
        _showDeleteDialog.value = false
    }

    fun promptNewFolder() {
        _showNewFolderDialog.value = true
    }

    fun dismissNewFolder() {
        _showNewFolderDialog.value = false
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            try {
                deleteNoteWithAttachments(noteId)
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

    fun deleteSelected() {
        viewModelScope.launch {
            try {
                _selectedNoteIds.value.forEach { deleteNoteWithAttachments(it) }
                _selectedFolderIds.value.forEach { folderRepository.deleteFolder(it) }
                _showDeleteDialog.value = false
                _isEditMode.value = false
                clearSelection()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete items: ${e.message}"
            }
        }
    }

    private suspend fun deleteNoteWithAttachments(noteId: Long) {
        val attachments = attachmentRepository.getAttachments(noteId, ITEM_TYPE_NOTE).first()
        attachments.forEach { attachment ->
            fileStorageManager.deleteFile(attachment.filePath)
            attachmentRepository.deleteAttachment(attachment)
        }
        noteRepository.deleteNote(noteId)
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                folderRepository.createFolder(name.trim(), _currentFolderId.value)
                _showNewFolderDialog.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create folder: ${e.message}"
            }
        }
    }

    fun moveSelected(destinationFolderId: Long?) {
        viewModelScope.launch {
            try {
                _selectedNoteIds.value.forEach { noteRepository.moveNote(it, destinationFolderId) }
                _selectedFolderIds.value.forEach { folderRepository.moveFolder(it, destinationFolderId) }
                _showMoveDialog.value = false
                _isEditMode.value = false
                clearSelection()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to move items: ${e.message}"
            }
        }
    }

    fun getMoveDestinations(callback: (List<FolderEntity>) -> Unit) {
        viewModelScope.launch {
            try {
                val allFolders = allFoldersFlow.first()
                val excluded = computeExcludedFolderIds(allFolders)
                callback(allFolders.filter { it.id !in excluded })
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load folders: ${e.message}"
            }
        }
    }

    private fun computeExcludedFolderIds(allFolders: List<FolderEntity>): Set<Long> {
        val selectedFolderIds = _selectedFolderIds.value
        if (selectedFolderIds.isEmpty()) return emptySet()
        val childrenByParent = allFolders.groupBy { it.parentId }
        val excluded = mutableSetOf<Long>()
        fun collectDescendants(id: Long) {
            excluded.add(id)
            childrenByParent[id]?.forEach { child -> collectDescendants(child.id) }
        }
        selectedFolderIds.forEach { collectDescendants(it) }
        return excluded
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        private const val ITEM_TYPE_NOTE = "note"
    }
}
