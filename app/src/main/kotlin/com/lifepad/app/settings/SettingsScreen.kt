package com.lifepad.app.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.ReminderDialog
import com.lifepad.app.components.RepeatOption
import com.lifepad.app.security.PinLockScreen
import com.lifepad.app.security.PinSetupScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SettingsStage {
    DEFAULT,
    VERIFY,
    SETUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var stage by rememberSaveable { mutableStateOf(SettingsStage.DEFAULT) }
    var showFinancePicker by rememberSaveable { mutableStateOf(false) }
    var showMoodPicker by rememberSaveable { mutableStateOf(false) }
    var showMoodPeriodPicker by rememberSaveable { mutableStateOf(false) }
    var showFinanceIntervalPicker by rememberSaveable { mutableStateOf(false) }
    var showCheckInReminderDialog by rememberSaveable { mutableStateOf(false) }
    var showGratitudeReminderDialog by rememberSaveable { mutableStateOf(false) }
    var showReflectionReminderDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshPinState()
    }

    when (stage) {
        SettingsStage.DEFAULT -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Settings") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RowWithIcon(
                                icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                                title = "Dashboard Cards"
                            )
                            SettingsRow(
                                title = "Finance card",
                                value = uiState.financeWidget.label,
                                onClick = { showFinancePicker = true }
                            )
                            SettingsRow(
                                title = "Mood card",
                                value = uiState.moodWidget.label,
                                onClick = { showMoodPicker = true }
                            )
                            SettingsRow(
                                title = "Mood card range",
                                value = uiState.moodWidgetPeriod.label,
                                onClick = { showMoodPeriodPicker = true }
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RowWithIcon(
                                icon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                                title = "Finance"
                            )
                            SettingsRow(
                                title = "Default interval",
                                value = uiState.financeInterval.label,
                                onClick = { showFinanceIntervalPicker = true }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Show category names")
                                    Text(
                                        "Display category text under records",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.financeShowNotes,
                                    onCheckedChange = viewModel::setFinanceShowNotes
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Show hashtags")
                                    Text(
                                        "Display tags extracted from descriptions",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = uiState.financeShowTags,
                                    onCheckedChange = viewModel::setFinanceShowTags
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RowWithIcon(
                                icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                                title = "Reminders"
                            )
                            ReminderRow(
                                title = "Check-in",
                                value = formatReminderValue(uiState.checkInReminders.firstOrNull()),
                                onClick = { showCheckInReminderDialog = true },
                                onClear = if (uiState.checkInReminders.isNotEmpty()) viewModel::clearCheckInReminder else null
                            )
                            ReminderRow(
                                title = "Gratitude",
                                value = formatReminderValue(uiState.gratitudeReminders.firstOrNull()),
                                onClick = { showGratitudeReminderDialog = true },
                                onClear = if (uiState.gratitudeReminders.isNotEmpty()) viewModel::clearGratitudeReminder else null
                            )
                            ReminderRow(
                                title = "Reflection",
                                value = formatReminderValue(uiState.reflectionReminders.firstOrNull()),
                                onClick = { showReflectionReminderDialog = true },
                                onClear = if (uiState.reflectionReminders.isNotEmpty()) viewModel::clearReflectionReminder else null
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RowWithIcon(
                                icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                title = "Security"
                            )
                            Text(
                                text = if (uiState.isPinSet) {
                                    "PIN is set. You can change it at any time."
                                } else {
                                    "No PIN set. Set one to protect your data."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    stage = if (uiState.isPinSet) SettingsStage.VERIFY else SettingsStage.SETUP
                                }
                            ) {
                                Text(if (uiState.isPinSet) "Change PIN" else "Set PIN")
                            }
                        }
                    }
                }
            }
        }
        SettingsStage.VERIFY -> {
            PinLockScreen(
                onUnlocked = { stage = SettingsStage.SETUP },
                showCancel = true,
                onCancel = { stage = SettingsStage.DEFAULT }
            )
        }
        SettingsStage.SETUP -> {
            PinSetupScreen(
                onPinSet = {
                    viewModel.refreshPinState()
                    stage = SettingsStage.DEFAULT
                },
                showSkip = false,
                showCancel = true,
                onCancel = { stage = SettingsStage.DEFAULT }
            )
        }
    }

    if (showFinancePicker) {
        SettingsPickerDialog(
            title = "Finance card",
            options = FinanceWidget.entries,
            selected = uiState.financeWidget,
            onSelect = {
                viewModel.setFinanceWidget(it)
                showFinancePicker = false
            },
            onDismiss = { showFinancePicker = false }
        )
    }

    if (showMoodPicker) {
        SettingsPickerDialog(
            title = "Mood card",
            options = MoodWidget.entries,
            selected = uiState.moodWidget,
            onSelect = {
                viewModel.setMoodWidget(it)
                showMoodPicker = false
            },
            onDismiss = { showMoodPicker = false }
        )
    }

    if (showMoodPeriodPicker) {
        SettingsPickerDialog(
            title = "Mood card range",
            options = MoodWidgetPeriod.entries,
            selected = uiState.moodWidgetPeriod,
            onSelect = {
                viewModel.setMoodWidgetPeriod(it)
                showMoodPeriodPicker = false
            },
            onDismiss = { showMoodPeriodPicker = false }
        )
    }

    if (showFinanceIntervalPicker) {
        SettingsPickerDialog(
            title = "Finance default interval",
            options = FinanceIntervalSetting.entries,
            selected = uiState.financeInterval,
            onSelect = {
                viewModel.setFinanceInterval(it)
                showFinanceIntervalPicker = false
            },
            onDismiss = { showFinanceIntervalPicker = false }
        )
    }

    if (showCheckInReminderDialog) {
        val reminder = uiState.checkInReminders.firstOrNull()
        ReminderDialog(
            onDismiss = { showCheckInReminderDialog = false },
            onConfirm = { title, message, triggerTime, repeatInterval ->
                viewModel.saveCheckInReminder(title, message, triggerTime, repeatInterval)
                showCheckInReminderDialog = false
            },
            initialTitle = reminder?.title ?: "Daily Check-in",
            initialMessage = reminder?.message ?: "",
            initialRepeatOption = repeatOptionForInterval(reminder?.repeatInterval),
            initialTriggerTime = reminder?.triggerTime
        )
    }

    if (showGratitudeReminderDialog) {
        val reminder = uiState.gratitudeReminders.firstOrNull()
        ReminderDialog(
            onDismiss = { showGratitudeReminderDialog = false },
            onConfirm = { title, message, triggerTime, repeatInterval ->
                viewModel.saveGratitudeReminder(title, message, triggerTime, repeatInterval)
                showGratitudeReminderDialog = false
            },
            initialTitle = reminder?.title ?: "Daily Gratitude",
            initialMessage = reminder?.message ?: "",
            initialRepeatOption = repeatOptionForInterval(reminder?.repeatInterval),
            initialTriggerTime = reminder?.triggerTime
        )
    }

    if (showReflectionReminderDialog) {
        val reminder = uiState.reflectionReminders.firstOrNull()
        ReminderDialog(
            onDismiss = { showReflectionReminderDialog = false },
            onConfirm = { title, message, triggerTime, repeatInterval ->
                viewModel.saveReflectionReminder(title, message, triggerTime, repeatInterval)
                showReflectionReminderDialog = false
            },
            initialTitle = reminder?.title ?: "Daily Reflection",
            initialMessage = reminder?.message ?: "",
            initialRepeatOption = repeatOptionForInterval(reminder?.repeatInterval),
            initialTriggerTime = reminder?.triggerTime
        )
    }
}

@Composable
private fun RowWithIcon(
    icon: @Composable () -> Unit,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon()
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReminderRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClear != null) {
            TextButton(onClick = onClear) {
                Text("Clear")
            }
        }
    }
}

private fun formatReminderValue(reminder: com.lifepad.app.data.local.entity.ReminderEntity?): String {
    if (reminder == null) return "Not set"
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(reminder.triggerTime))
    val repeat = when (repeatOptionForInterval(reminder.repeatInterval)) {
        RepeatOption.DAILY -> "Daily"
        RepeatOption.WEEKLY -> "Weekly"
        RepeatOption.MONTHLY -> "Monthly"
        else -> "One-time"
    }
    return "$repeat at $time"
}

private fun repeatOptionForInterval(interval: Long?): RepeatOption {
    return when (interval) {
        RepeatOption.DAILY.intervalMs -> RepeatOption.DAILY
        RepeatOption.WEEKLY.intervalMs -> RepeatOption.WEEKLY
        RepeatOption.MONTHLY.intervalMs -> RepeatOption.MONTHLY
        else -> RepeatOption.NONE
    }
}

@Composable
private fun <T> SettingsPickerDialog(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) where T : Enum<T> {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (option) {
                                is FinanceWidget -> option.label
                                is MoodWidget -> option.label
                                is MoodWidgetPeriod -> option.label
                                is FinanceIntervalSetting -> option.label
                                else -> option.name
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (option == selected) {
                            Text("Selected", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
