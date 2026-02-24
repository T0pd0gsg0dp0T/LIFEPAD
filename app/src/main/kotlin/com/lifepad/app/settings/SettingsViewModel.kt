package com.lifepad.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepad.app.security.SecurityManager
import com.lifepad.app.data.local.entity.ReminderEntity
import com.lifepad.app.data.repository.ReminderRepository
import com.lifepad.app.journal.JournalTemplateReminders
import com.lifepad.app.util.BackupManager
import android.net.Uri
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
    private val reminderRepository: ReminderRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val pinState = MutableStateFlow(securityManager.isPinSet())
    private val backupInProgress = MutableStateFlow(false)
    private val backupResult = MutableStateFlow<BackupResult?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        listOf(
            pinState,
            settingsRepository.financeWidget,
            settingsRepository.moodWidget,
            settingsRepository.moodWidgetPeriod,
            settingsRepository.financeInterval,
            settingsRepository.financeShowNotes,
            settingsRepository.financeShowTags,
            reminderRepository.getForItem(JournalTemplateReminders.ITEM_TYPE, JournalTemplateReminders.CHECK_IN_ID),
            reminderRepository.getForItem(JournalTemplateReminders.ITEM_TYPE, JournalTemplateReminders.GRATITUDE_ID),
            reminderRepository.getForItem(JournalTemplateReminders.ITEM_TYPE, JournalTemplateReminders.REFLECTION_ID)
        )
    ) { values ->
        val isPinSet = values[0] as Boolean
        val financeWidget = values[1] as FinanceWidget
        val moodWidget = values[2] as MoodWidget
        val moodWidgetPeriod = values[3] as MoodWidgetPeriod
        val financeInterval = values[4] as FinanceIntervalSetting
        val financeShowNotes = values[5] as Boolean
        val financeShowTags = values[6] as Boolean
        @Suppress("UNCHECKED_CAST")
        val checkInReminders = values[7] as List<ReminderEntity>
        @Suppress("UNCHECKED_CAST")
        val gratitudeReminders = values[8] as List<ReminderEntity>
        @Suppress("UNCHECKED_CAST")
        val reflectionReminders = values[9] as List<ReminderEntity>
        SettingsUiState(
            isPinSet = isPinSet,
            financeWidget = financeWidget,
            moodWidget = moodWidget,
            moodWidgetPeriod = moodWidgetPeriod,
            financeInterval = financeInterval,
            financeShowNotes = financeShowNotes,
            financeShowTags = financeShowTags,
            checkInReminders = checkInReminders,
            gratitudeReminders = gratitudeReminders,
            reflectionReminders = reflectionReminders
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isPinSet = securityManager.isPinSet())
    )

    val isBackupInProgress: StateFlow<Boolean> = backupInProgress

    val lastBackupResult: StateFlow<BackupResult?> = backupResult

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

    fun setFinanceInterval(interval: FinanceIntervalSetting) {
        settingsRepository.setFinanceInterval(interval)
    }

    fun setFinanceShowNotes(enabled: Boolean) {
        settingsRepository.setFinanceShowNotes(enabled)
    }

    fun setFinanceShowTags(enabled: Boolean) {
        settingsRepository.setFinanceShowTags(enabled)
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

    fun createFullBackup(destination: Uri) {
        if (backupInProgress.value) return
        backupInProgress.value = true
        viewModelScope.launch {
            val result = backupManager.createFullBackup(destination)
            backupResult.value = if (result.isSuccess) {
                BackupResult(success = true, message = "Backup created.")
            } else {
                BackupResult(
                    success = false,
                    message = "Backup failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                )
            }
            backupInProgress.value = false
        }
    }

    fun clearBackupResult() {
        backupResult.value = null
    }
}

data class SettingsUiState(
    val isPinSet: Boolean,
    val financeWidget: FinanceWidget = FinanceWidget.INCOME_EXPENSE,
    val moodWidget: MoodWidget = MoodWidget.MOOD_LINE,
    val moodWidgetPeriod: MoodWidgetPeriod = MoodWidgetPeriod.MONTH,
    val financeInterval: FinanceIntervalSetting = FinanceIntervalSetting.MONTH,
    val financeShowNotes: Boolean = true,
    val financeShowTags: Boolean = true,
    val checkInReminders: List<ReminderEntity> = emptyList(),
    val gratitudeReminders: List<ReminderEntity> = emptyList(),
    val reflectionReminders: List<ReminderEntity> = emptyList()
)

data class BackupResult(
    val success: Boolean,
    val message: String
)
