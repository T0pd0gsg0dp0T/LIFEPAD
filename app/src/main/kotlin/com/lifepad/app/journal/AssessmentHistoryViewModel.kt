package com.lifepad.app.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.AssessmentEntity
import com.lifepad.app.data.repository.AssessmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssessmentHistoryUiState(
    val assessments: List<AssessmentEntity> = emptyList(),
    val gad7Trend: List<AssessmentEntity> = emptyList(),
    val phq9Trend: List<AssessmentEntity> = emptyList(),
    val selectedFilter: String = "ALL",
    val isLoading: Boolean = true
)

@HiltViewModel
class AssessmentHistoryViewModel @Inject constructor(
    private val assessmentRepository: AssessmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssessmentHistoryUiState())
    val uiState: StateFlow<AssessmentHistoryUiState> = _uiState.asStateFlow()

    private var collectJob: Job? = null

    init {
        loadData()
    }

    fun onFilterChange(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val gad7Trend = assessmentRepository.getTrend("GAD7")
            val phq9Trend = assessmentRepository.getTrend("PHQ9")

            _uiState.update {
                it.copy(
                    gad7Trend = gad7Trend,
                    phq9Trend = phq9Trend,
                    isLoading = false
                )
            }
        }

        // Cancel previous collector before starting a new one
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            val filter = _uiState.value.selectedFilter
            val flow = if (filter == "ALL") {
                assessmentRepository.getAll()
            } else {
                assessmentRepository.getByType(filter)
            }
            flow.collect { assessments ->
                _uiState.update { it.copy(assessments = assessments) }
            }
        }
    }
}
