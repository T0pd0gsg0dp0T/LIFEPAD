package com.lifepad.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securityManager: SecurityManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val pinState = MutableStateFlow(securityManager.isPinSet())

    val uiState: StateFlow<SettingsUiState> = combine(
        pinState,
        settingsRepository.financeWidget,
        settingsRepository.moodWidget
    ) { isPinSet, financeWidget, moodWidget ->
        SettingsUiState(
            isPinSet = isPinSet,
            financeWidget = financeWidget,
            moodWidget = moodWidget
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isPinSet = securityManager.isPinSet())
    )

    fun refreshPinState() {
        pinState.value = securityManager.isPinSet()
        settingsRepository.refresh()
    }

    fun setFinanceWidget(widget: FinanceWidget) {
        settingsRepository.setFinanceWidget(widget)
    }

    fun setMoodWidget(widget: MoodWidget) {
        settingsRepository.setMoodWidget(widget)
    }
}

data class SettingsUiState(
    val isPinSet: Boolean,
    val financeWidget: FinanceWidget = FinanceWidget.INCOME_EXPENSE,
    val moodWidget: MoodWidget = MoodWidget.MOOD_LINE_2W
)
