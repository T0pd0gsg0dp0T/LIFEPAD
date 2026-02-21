package com.lifepad.app.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.data.local.entity.AssessmentEntity
import com.lifepad.app.data.repository.AssessmentRepository
import com.lifepad.app.domain.assessment.AssessmentQuestion
import com.lifepad.app.domain.assessment.AssessmentQuestions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssessmentUiState(
    val selectedType: String? = null,
    val questions: List<AssessmentQuestion> = emptyList(),
    val answers: List<Int> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val isComplete: Boolean = false,
    val score: Int = 0,
    val severity: String = "",
    val maxScore: Int = 0,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false
)

@HiltViewModel
class AssessmentViewModel @Inject constructor(
    private val assessmentRepository: AssessmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssessmentUiState())
    val uiState: StateFlow<AssessmentUiState> = _uiState.asStateFlow()

    fun selectType(type: String) {
        val questions = AssessmentQuestions.getQuestions(type)
        _uiState.update {
            AssessmentUiState(
                selectedType = type,
                questions = questions,
                answers = List(questions.size) { -1 },
                maxScore = AssessmentQuestions.getMaxScore(type)
            )
        }
    }

    fun answerQuestion(questionIndex: Int, answerValue: Int) {
        val currentAnswers = _uiState.value.answers.toMutableList()
        currentAnswers[questionIndex] = answerValue
        _uiState.update { it.copy(answers = currentAnswers) }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentQuestionIndex < state.questions.size - 1) {
            _uiState.update { it.copy(currentQuestionIndex = it.currentQuestionIndex + 1) }
        }
    }

    fun previousQuestion() {
        if (_uiState.value.currentQuestionIndex > 0) {
            _uiState.update { it.copy(currentQuestionIndex = it.currentQuestionIndex - 1) }
        }
    }

    fun submitAssessment() {
        val state = _uiState.value
        val type = state.selectedType ?: return
        if (state.answers.any { it == -1 }) return

        val score = state.answers.sum()
        val severity = AssessmentQuestions.getSeverity(type, score)

        _uiState.update {
            it.copy(
                isComplete = true,
                score = score,
                severity = severity,
                isSaving = true
            )
        }

        viewModelScope.launch {
            val entity = AssessmentEntity(
                type = type,
                score = score,
                answers = state.answers.joinToString(","),
                date = System.currentTimeMillis()
            )
            assessmentRepository.save(entity)
            _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }

    fun reset() {
        _uiState.update { AssessmentUiState() }
    }
}
