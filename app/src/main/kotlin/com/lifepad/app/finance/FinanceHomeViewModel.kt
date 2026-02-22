package com.lifepad.app.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.CategoryEntity
import com.lifepad.app.data.local.entity.CategoryType
import com.lifepad.app.data.local.entity.HashtagEntity
import com.lifepad.app.data.local.entity.TransactionEntity
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.data.repository.FinanceRepository
import com.lifepad.app.data.repository.HashtagRepository
import com.lifepad.app.domain.parser.HashtagParser
import com.lifepad.app.settings.FinanceIntervalSetting
import com.lifepad.app.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class FinanceHomeUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val archivedCategories: List<CategoryEntity> = emptyList(),
    val hashtags: List<HashtagEntity> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val interval: FinanceIntervalSetting = FinanceIntervalSetting.MONTH,
    val customStart: Long? = null,
    val customEnd: Long? = null,
    val searchQuery: String = "",
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedHashtags: Set<String> = emptySet(),
    val categoryTagOrLogic: Boolean = true,
    val showNotes: Boolean = true,
    val showTags: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class FinanceHomeViewModel @Inject constructor(
    private val financeRepository: FinanceRepository,
    private val hashtagRepository: HashtagRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _interval = MutableStateFlow(settingsRepository.financeInterval.value)
    private val _customStart = MutableStateFlow<Long?>(null)
    private val _customEnd = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _selectedHashtags = MutableStateFlow<Set<String>>(emptySet())
    private val _categoryTagOrLogic = MutableStateFlow(true)
    private val _errorMessage = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FinanceHomeUiState> = combine(
        financeRepository.getAllTransactions(),
        financeRepository.getAllCategories(),
        financeRepository.getArchivedCategories(),
        hashtagRepository.getAllHashtags(),
        settingsRepository.financeShowNotes,
        settingsRepository.financeShowTags,
        _interval,
        _customStart,
        _customEnd,
        _searchQuery,
        _selectedCategoryIds,
        _selectedHashtags,
        _categoryTagOrLogic,
        _errorMessage
    ) { values ->
        val transactions = values[0] as List<TransactionEntity>
        val categories = values[1] as List<CategoryEntity>
        val archivedCategories = values[2] as List<CategoryEntity>
        val hashtags = values[3] as List<HashtagEntity>
        val showNotes = values[4] as Boolean
        val showTags = values[5] as Boolean
        val interval = values[6] as FinanceIntervalSetting
        val customStart = values[7] as Long?
        val customEnd = values[8] as Long?
        val searchQuery = values[9] as String
        val selectedCategoryIds = values[10] as Set<Long>
        val selectedHashtags = values[11] as Set<String>
        val categoryTagOrLogic = values[12] as Boolean
        val errorMessage = values[13] as String?

        val (start, end) = getIntervalRange(interval, customStart, customEnd)
        val filtered = transactions.filter { tx ->
            val inRange = start == null || end == null || (tx.transactionDate in start..end)
            if (!inRange) return@filter false
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                tx.description.contains(searchQuery, ignoreCase = true)
            }
            if (!matchesSearch) return@filter false

            val matchesCategory = if (selectedCategoryIds.isEmpty()) {
                true
            } else {
                tx.categoryId != null && tx.categoryId in selectedCategoryIds
            }

            val txTags = HashtagParser.extractHashtags(tx.description).toSet()
            val matchesTags = if (selectedHashtags.isEmpty()) {
                true
            } else {
                txTags.intersect(selectedHashtags).isNotEmpty()
            }

            if (selectedCategoryIds.isNotEmpty() && selectedHashtags.isNotEmpty()) {
                if (categoryTagOrLogic) {
                    matchesCategory || matchesTags
                } else {
                    matchesCategory && matchesTags
                }
            } else {
                matchesCategory && matchesTags
            }
        }

        val totalIncome = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        FinanceHomeUiState(
            transactions = filtered.sortedByDescending { it.transactionDate },
            categories = categories,
            archivedCategories = archivedCategories,
            hashtags = hashtags,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = totalIncome - totalExpense,
            interval = interval,
            customStart = customStart,
            customEnd = customEnd,
            searchQuery = searchQuery,
            selectedCategoryIds = selectedCategoryIds,
            selectedHashtags = selectedHashtags,
            categoryTagOrLogic = categoryTagOrLogic,
            showNotes = showNotes,
            showTags = showTags,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinanceHomeUiState()
    )

    fun setInterval(interval: FinanceIntervalSetting) {
        _interval.value = interval
        settingsRepository.setFinanceInterval(interval)
    }

    fun setCustomRange(start: Long?, end: Long?) {
        _customStart.value = start
        _customEnd.value = end
        if (start != null && end != null) {
            _interval.value = FinanceIntervalSetting.CUSTOM
            settingsRepository.setFinanceInterval(FinanceIntervalSetting.CUSTOM)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCategorySelection(categoryId: Long) {
        _selectedCategoryIds.value = _selectedCategoryIds.value.toMutableSet().also { set ->
            if (!set.add(categoryId)) {
                set.remove(categoryId)
            }
        }
    }

    fun toggleHashtagSelection(name: String) {
        val normalized = name.lowercase()
        _selectedHashtags.value = _selectedHashtags.value.toMutableSet().also { set ->
            if (!set.add(normalized)) {
                set.remove(normalized)
            }
        }
    }

    fun clearFilters() {
        _selectedCategoryIds.value = emptySet()
        _selectedHashtags.value = emptySet()
        _categoryTagOrLogic.value = true
        _searchQuery.value = ""
    }

    fun toggleCategoryTagLogic() {
        _categoryTagOrLogic.value = !_categoryTagOrLogic.value
    }

    fun setShowNotes(enabled: Boolean) {
        settingsRepository.setFinanceShowNotes(enabled)
    }

    fun setShowTags(enabled: Boolean) {
        settingsRepository.setFinanceShowTags(enabled)
    }

    fun updateCategoryOrder(reordered: List<CategoryEntity>) {
        viewModelScope.launch {
            try {
                val updated = reordered.mapIndexed { index, category ->
                    category.copy(sortOrder = index)
                }
                financeRepository.updateCategories(updated)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update category order: ${e.message}"
            }
        }
    }

    fun archiveCategory(categoryId: Long) {
        viewModelScope.launch {
            try {
                financeRepository.archiveCategory(categoryId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to archive category: ${e.message}"
            }
        }
    }

    fun unarchiveCategory(categoryId: Long) {
        viewModelScope.launch {
            try {
                financeRepository.unarchiveCategory(categoryId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to restore category: ${e.message}"
            }
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            try {
                financeRepository.deleteCategory(categoryId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete category: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun categoriesForType(type: CategoryType): List<CategoryEntity> =
        uiState.value.categories.filter { it.type == type && !it.isArchived }
}

private fun getIntervalRange(
    interval: FinanceIntervalSetting,
    customStart: Long?,
    customEnd: Long?
): Pair<Long?, Long?> {
    return when (interval) {
        FinanceIntervalSetting.MONTH -> {
            val start = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = Calendar.getInstance().apply {
                timeInMillis = start
                add(Calendar.MONTH, 1)
                add(Calendar.MILLISECOND, -1)
            }.timeInMillis
            start to end
        }
        FinanceIntervalSetting.YEAR -> {
            val start = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = Calendar.getInstance().apply {
                timeInMillis = start
                add(Calendar.YEAR, 1)
                add(Calendar.MILLISECOND, -1)
            }.timeInMillis
            start to end
        }
        FinanceIntervalSetting.ALL -> null to null
        FinanceIntervalSetting.CUSTOM -> customStart to customEnd
    }
}
