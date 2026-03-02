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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@OptIn(ExperimentalCoroutinesApi::class)
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

    private data class NoteListCore(
        val notes: List<NoteEntity>,
        val folders: List<FolderEntity>,
        val currentFolderId: Long?,
        val folderPath: List<FolderEntity>,
        val isEditMode: Boolean
    )

    private data class NoteListSelection(
        val selectedNotes: Set<Long>,
        val selectedFolders: Set<Long>,
        val error: String?,
        val showMove: Boolean,
        val showDelete: Boolean,
        val showNewFolder: Boolean
    )

    private val coreFlow = combine(
        notesFlow,
        foldersFlow,
        _currentFolderId,
        _folderPath,
        _isEditMode
    ) { notes, folders, currentFolderId, folderPath, isEditMode ->
        NoteListCore(
            notes = notes,
            folders = folders,
            currentFolderId = currentFolderId,
            folderPath = folderPath,
            isEditMode = isEditMode
        )
    }

    private data class SelectionState(
        val selectedNotes: Set<Long>,
        val selectedFolders: Set<Long>,
        val error: String?
    )

    private data class DialogState(
        val showMove: Boolean,
        val showDelete: Boolean,
        val showNewFolder: Boolean
    )

    private val selectionStateFlow = combine(
        _selectedNoteIds,
        _selectedFolderIds,
        _errorMessage
    ) { selectedNotes, selectedFolders, error ->
        SelectionState(
            selectedNotes = selectedNotes,
            selectedFolders = selectedFolders,
            error = error
        )
    }

    private val dialogStateFlow = combine(
        _showMoveDialog,
        _showDeleteDialog,
        _showNewFolderDialog
    ) { showMove, showDelete, showNewFolder ->
        DialogState(
            showMove = showMove,
            showDelete = showDelete,
            showNewFolder = showNewFolder
        )
    }

    private val selectionFlow = combine(
        selectionStateFlow,
        dialogStateFlow
    ) { selection, dialogs ->
        NoteListSelection(
            selectedNotes = selection.selectedNotes,
            selectedFolders = selection.selectedFolders,
            error = selection.error,
            showMove = dialogs.showMove,
            showDelete = dialogs.showDelete,
            showNewFolder = dialogs.showNewFolder
        )
    }

    val uiState: StateFlow<NoteListUiState> = combine(
        coreFlow,
        selectionFlow
    ) { core, selection ->
        NoteListUiState(
            notes = core.notes,
            folders = core.folders,
            currentFolderId = core.currentFolderId,
            folderPath = core.folderPath,
            isEditMode = core.isEditMode,
            selectedNoteIds = selection.selectedNotes,
            selectedFolderIds = selection.selectedFolders,
            errorMessage = selection.error,
            showMoveDialog = selection.showMove,
            showDeleteDialog = selection.showDelete,
            showNewFolderDialog = selection.showNewFolder
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
        clearSelection()
        viewModelScope.launch {
            val folder = folderRepository.getFolderById(current)
            _currentFolderId.value = folder?.parentId
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
