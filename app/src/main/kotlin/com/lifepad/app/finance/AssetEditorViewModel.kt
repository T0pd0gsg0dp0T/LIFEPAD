package com.lifepad.app.finance

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.AssetEntity
import com.lifepad.app.data.local.entity.AssetType
import com.lifepad.app.data.repository.NetWorthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssetEditorUiState(
    val assetId: Long? = null,
    val name: String = "",
    val value: String = "",
    val assetType: AssetType = AssetType.OTHER,
    val isLiability: Boolean = false,
    val notes: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AssetEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val netWorthRepository: NetWorthRepository
) : ViewModel() {

    private val assetId: Long? = savedStateHandle.get<Long>("assetId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(AssetEditorUiState(assetId = assetId))
    val uiState: StateFlow<AssetEditorUiState> = _uiState.asStateFlow()

    init {
        loadAsset()
    }

    private fun loadAsset() {
        viewModelScope.launch {
            try {
                if (assetId != null) {
                    val asset = netWorthRepository.getAssetById(assetId)
                    if (asset != null) {
                        _uiState.update {
                            it.copy(
                                name = asset.name,
                                value = asset.value.toString(),
                                assetType = asset.assetType,
                                isLiability = asset.isLiability,
                                notes = asset.notes,
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

    fun onNameChange(name: String) { _uiState.update { it.copy(name = name) } }
    fun onValueChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _uiState.update { it.copy(value = value) }
        }
    }
    fun onTypeChange(type: AssetType) { _uiState.update { it.copy(assetType = type) } }
    fun onLiabilityToggle(isLiability: Boolean) { _uiState.update { it.copy(isLiability = isLiability) } }
    fun onNotesChange(notes: String) { _uiState.update { it.copy(notes = notes) } }

    suspend fun saveAsset(): Long? {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a name") }
            return null
        }
        val value = state.value.toDoubleOrNull()
        if (value == null || value <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid value") }
            return null
        }

        _uiState.update { it.copy(isSaving = true) }

        return try {
            val asset = AssetEntity(
                id = state.assetId ?: 0L,
                name = state.name,
                value = value,
                assetType = state.assetType,
                isLiability = state.isLiability,
                notes = state.notes
            )
            val savedId = netWorthRepository.saveAsset(asset)
            _uiState.update { it.copy(assetId = savedId, isSaving = false) }
            savedId
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isSaving = false, errorMessage = "Failed to save: ${e.message}")
            }
            null
        }
    }

    fun deleteAsset() {
        viewModelScope.launch {
            try {
                assetId?.let { netWorthRepository.deleteAsset(it) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
}
