package com.lifepad.app.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("lifepad_settings", Context.MODE_PRIVATE)

    private val _financeWidget = MutableStateFlow(
        FinanceWidget.fromName(prefs.getString(KEY_FINANCE_WIDGET, null))
    )
    val financeWidget: StateFlow<FinanceWidget> = _financeWidget.asStateFlow()

    private val _moodWidget = MutableStateFlow(
        MoodWidget.fromName(prefs.getString(KEY_MOOD_WIDGET, null))
    )
    val moodWidget: StateFlow<MoodWidget> = _moodWidget.asStateFlow()

    private val _notesDoubleTapToEdit = MutableStateFlow(
        prefs.getBoolean(KEY_NOTES_DOUBLE_TAP_EDIT, false)
    )
    val notesDoubleTapToEdit: StateFlow<Boolean> = _notesDoubleTapToEdit.asStateFlow()

    private val _financeInterval = MutableStateFlow(
        FinanceIntervalSetting.fromName(prefs.getString(KEY_FINANCE_INTERVAL, null))
    )
    val financeInterval: StateFlow<FinanceIntervalSetting> = _financeInterval.asStateFlow()

    private val _financeShowNotes = MutableStateFlow(
        prefs.getBoolean(KEY_FINANCE_SHOW_NOTES, true)
    )
    val financeShowNotes: StateFlow<Boolean> = _financeShowNotes.asStateFlow()

    private val _financeShowTags = MutableStateFlow(
        prefs.getBoolean(KEY_FINANCE_SHOW_TAGS, true)
    )
    val financeShowTags: StateFlow<Boolean> = _financeShowTags.asStateFlow()

    fun setFinanceWidget(widget: FinanceWidget) {
        prefs.edit().putString(KEY_FINANCE_WIDGET, widget.name).apply()
        _financeWidget.value = widget
    }

    fun setMoodWidget(widget: MoodWidget) {
        prefs.edit().putString(KEY_MOOD_WIDGET, widget.name).apply()
        _moodWidget.value = widget
    }

    fun setNotesDoubleTapToEdit(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTES_DOUBLE_TAP_EDIT, enabled).apply()
        _notesDoubleTapToEdit.value = enabled
    }

    fun setFinanceInterval(interval: FinanceIntervalSetting) {
        prefs.edit().putString(KEY_FINANCE_INTERVAL, interval.name).apply()
        _financeInterval.value = interval
    }

    fun setFinanceShowNotes(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FINANCE_SHOW_NOTES, enabled).apply()
        _financeShowNotes.value = enabled
    }

    fun setFinanceShowTags(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FINANCE_SHOW_TAGS, enabled).apply()
        _financeShowTags.value = enabled
    }

    fun refresh() {
        _financeWidget.value = FinanceWidget.fromName(prefs.getString(KEY_FINANCE_WIDGET, null))
        _moodWidget.value = MoodWidget.fromName(prefs.getString(KEY_MOOD_WIDGET, null))
        _notesDoubleTapToEdit.value = prefs.getBoolean(KEY_NOTES_DOUBLE_TAP_EDIT, false)
        _financeInterval.value = FinanceIntervalSetting.fromName(prefs.getString(KEY_FINANCE_INTERVAL, null))
        _financeShowNotes.value = prefs.getBoolean(KEY_FINANCE_SHOW_NOTES, true)
        _financeShowTags.value = prefs.getBoolean(KEY_FINANCE_SHOW_TAGS, true)
    }

    companion object {
        private const val KEY_FINANCE_WIDGET = "finance_widget"
        private const val KEY_MOOD_WIDGET = "mood_widget"
        private const val KEY_NOTES_DOUBLE_TAP_EDIT = "notes_double_tap_edit"
        private const val KEY_FINANCE_INTERVAL = "finance_interval"
        private const val KEY_FINANCE_SHOW_NOTES = "finance_show_notes"
        private const val KEY_FINANCE_SHOW_TAGS = "finance_show_tags"
    }
}

enum class FinanceIntervalSetting(val label: String) {
    MONTH("This month"),
    YEAR("This year"),
    ALL("All time"),
    CUSTOM("Custom range");

    companion object {
        fun fromName(name: String?): FinanceIntervalSetting {
            return entries.firstOrNull { it.name == name } ?: MONTH
        }
    }
}

enum class FinanceWidget(val label: String) {
    INCOME_EXPENSE("Income vs Expense"),
    CASHFLOW("Cashflow Forecast"),
    NET_WORTH("Net Worth Trend");

    companion object {
        fun fromName(name: String?): FinanceWidget {
            return entries.firstOrNull { it.name == name } ?: INCOME_EXPENSE
        }
    }
}

enum class MoodWidget(val label: String) {
    MOOD_LINE_2W("Mood Trend (2 weeks)"),
    MOOD_CALENDAR_30D("Mood Calendar (30 days)"),
    MOOD_DISTRIBUTION_30D("Mood Distribution (30 days)");

    companion object {
        fun fromName(name: String?): MoodWidget {
            return entries.firstOrNull { it.name == name } ?: MOOD_LINE_2W
        }
    }
}
