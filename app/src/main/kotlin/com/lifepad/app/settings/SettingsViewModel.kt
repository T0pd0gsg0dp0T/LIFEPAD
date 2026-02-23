package com.lifepad.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.security.SecurityManager
import com.lifepad.app.data.local.entity.ReminderEntity
import com.lifepad.app.data.repository.ReminderRepository
import com.lifepad.app.journal.JournalTemplateReminders
import kotlinx.coroutines.launch
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
    private val settingsRepository: SettingsRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val pinState = MutableStateFlow(securityManager.isPinSet())

    val uiState: StateFlow<SettingsUiState> = combine(
        pinState,
        settingsRepository.financeWidget,
        settingsRepository.moodWidget,
        settingsRepository.moodWidgetPeriod,
        reminderRepository.getForItem(JournalTemplateReminders.ITEM_TYPE, JournalTemplateReminders.CHECK_IN_ID),
        reminderRepository.getForItem(JournalTemplateReminders.ITEM_TYPE, JournalTemplateReminders.GRATITUDE_ID),
        reminderRepository.getForItem(JournalTemplateReminders.ITEM_TYPE, JournalTemplateReminders.REFLECTION_ID)
    ) { isPinSet, financeWidget, moodWidget, moodWidgetPeriod, checkInReminders, gratitudeReminders, reflectionReminders ->
        SettingsUiState(
            isPinSet = isPinSet,
            financeWidget = financeWidget,
            moodWidget = moodWidget,
            moodWidgetPeriod = moodWidgetPeriod,
            checkInReminders = checkInReminders,
            gratitudeReminders = gratitudeReminders,
            reflectionReminders = reflectionReminders
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

    fun setMoodWidgetPeriod(period: MoodWidgetPeriod) {
        settingsRepository.setMoodWidgetPeriod(period)
    }

    fun saveCheckInReminder(title: String, message: String, triggerTime: Long, repeatInterval: Long) {
        viewModelScope.launch {
            uiState.value.checkInReminders.forEach { reminderRepository.delete(it.id) }
            val reminder = ReminderEntity(
                title = title,
                message = message,
                triggerTime = triggerTime,
                repeatInterval = repeatInterval,
                linkedItemType = JournalTemplateReminders.ITEM_TYPE,
                linkedItemId = JournalTemplateReminders.CHECK_IN_ID
            )
            reminderRepository.save(reminder)
        }
    }

    fun saveGratitudeReminder(title: String, message: String, triggerTime: Long, repeatInterval: Long) {
        viewModelScope.launch {
            uiState.value.gratitudeReminders.forEach { reminderRepository.delete(it.id) }
            val reminder = ReminderEntity(
                title = title,
                message = message,
                triggerTime = triggerTime,
                repeatInterval = repeatInterval,
                linkedItemType = JournalTemplateReminders.ITEM_TYPE,
                linkedItemId = JournalTemplateReminders.GRATITUDE_ID
            )
            reminderRepository.save(reminder)
        }
    }

    fun saveReflectionReminder(title: String, message: String, triggerTime: Long, repeatInterval: Long) {
        viewModelScope.launch {
            uiState.value.reflectionReminders.forEach { reminderRepository.delete(it.id) }
            val reminder = ReminderEntity(
                title = title,
                message = message,
                triggerTime = triggerTime,
                repeatInterval = repeatInterval,
                linkedItemType = JournalTemplateReminders.ITEM_TYPE,
                linkedItemId = JournalTemplateReminders.REFLECTION_ID
            )
            reminderRepository.save(reminder)
        }
    }

    fun clearCheckInReminder() {
        viewModelScope.launch {
            uiState.value.checkInReminders.forEach { reminderRepository.delete(it.id) }
        }
    }

    fun clearGratitudeReminder() {
        viewModelScope.launch {
            uiState.value.gratitudeReminders.forEach { reminderRepository.delete(it.id) }
        }
    }

    fun clearReflectionReminder() {
        viewModelScope.launch {
            uiState.value.reflectionReminders.forEach { reminderRepository.delete(it.id) }
        }
    }
}

data class SettingsUiState(
    val isPinSet: Boolean,
    val financeWidget: FinanceWidget = FinanceWidget.INCOME_EXPENSE,
    val moodWidget: MoodWidget = MoodWidget.MOOD_LINE,
    val moodWidgetPeriod: MoodWidgetPeriod = MoodWidgetPeriod.MONTH,
    val checkInReminders: List<ReminderEntity> = emptyList(),
    val gratitudeReminders: List<ReminderEntity> = emptyList(),
    val reflectionReminders: List<ReminderEntity> = emptyList()
)
