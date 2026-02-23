package com.lifepad.app.finance

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.CategoryEntity
import com.lifepad.app.data.local.entity.CategoryType
import com.lifepad.app.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class CategoryEditorUiState(
    val categoryId: Long? = null,
    val name: String = "",
    val type: CategoryType = CategoryType.EXPENSE,
    val icon: String = "more_horiz",
    val color: Int = categoryColorPalette.first().toArgb(),
    val isDefault: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CategoryEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val categoryId: Long? = savedStateHandle.get<Long>("categoryId")?.takeIf { it != 0L }

    private val _uiState = MutableStateFlow(CategoryEditorUiState(categoryId = categoryId))
    val uiState: StateFlow<CategoryEditorUiState> = _uiState.asStateFlow()

    init {
        loadCategory()
    }

    private fun loadCategory() {
        viewModelScope.launch {
            if (categoryId == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            try {
                val category = financeRepository.getCategoryById(categoryId)
                if (category != null) {
                    _uiState.update {
                        it.copy(
                            name = category.name,
                            type = category.type,
                            icon = category.icon ?: "more_horiz",
                            color = category.color,
                            isDefault = category.isDefault,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateType(type: CategoryType) {
        _uiState.update { it.copy(type = type) }
    }

    fun updateIcon(icon: String) {
        _uiState.update { it.copy(icon = icon) }
    }

    fun updateColor(color: Int) {
        _uiState.update { it.copy(color = color) }
    }

    fun saveCategory(onSaved: (Long) -> Unit) {
        if (_uiState.value.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val existing = categoryId?.let { financeRepository.getCategoryById(it) }
                val sortOrder = existing?.sortOrder ?: financeRepository.getAllCategoriesSnapshot().size
                val category = CategoryEntity(
                    id = categoryId ?: 0L,
                    name = _uiState.value.name.trim(),
                    icon = _uiState.value.icon,
                    type = _uiState.value.type,
                    color = _uiState.value.color,
                    sortOrder = sortOrder,
                    isArchived = existing?.isArchived ?: false,
                    isDefault = _uiState.value.isDefault,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                val id = financeRepository.saveCategory(category)
                onSaved(if (categoryId == null) id else category.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteCategory(onDeleted: () -> Unit) {
        val id = categoryId ?: return
        viewModelScope.launch {
            try {
                financeRepository.deleteCategory(id)
                onDeleted()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
