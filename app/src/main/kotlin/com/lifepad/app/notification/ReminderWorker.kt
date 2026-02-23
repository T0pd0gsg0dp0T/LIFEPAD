package com.lifepad.app.notification

import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifepad.app.MainActivity
import com.lifepad.app.data.local.dao.ReminderDao
import com.lifepad.app.journal.JournalTemplateReminders
import com.lifepad.app.navigation.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderDao: ReminderDao,
    private val notificationHelper: NotificationHelper,
    private val reminderScheduler: ReminderScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong("reminder_id", -1)
        if (reminderId == -1L) return Result.failure()

        val reminder = reminderDao.getById(reminderId)
        if (reminder == null) {
            Log.d(TAG, "Reminder $reminderId no longer exists, skipping")
            return Result.success()
        }

        if (!reminder.isEnabled) {
            Log.d(TAG, "Reminder $reminderId is disabled, skipping")
            return Result.success()
        }

        val route = buildRouteForReminder(reminder)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NAV_ROUTE, route)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationHelper.showReminderNotification(
            id = reminder.id.toInt(),
            title = reminder.title,
            message = reminder.message,
            pendingIntent = pendingIntent
        )
        Log.d(TAG, "Fired notification for reminder $reminderId")

        // Reschedule if this is a repeating reminder
        if (reminder.repeatInterval > 0) {
            val nextTriggerTime = reminder.triggerTime + reminder.repeatInterval
            val updated = reminder.copy(
                triggerTime = nextTriggerTime,
                updatedAt = System.currentTimeMillis()
            )
            reminderDao.update(updated)
            reminderScheduler.schedule(updated)
            Log.d(TAG, "Rescheduled repeating reminder $reminderId for $nextTriggerTime")
        }

        return Result.success()
    }

    private fun buildRouteForReminder(reminder: com.lifepad.app.data.local.entity.ReminderEntity): String {
        return when (reminder.linkedItemType) {
            JournalTemplateReminders.ITEM_TYPE -> {
                when (reminder.linkedItemId) {
                    JournalTemplateReminders.CHECK_IN_ID ->
                        Screen.CheckInJournal.createRoute(fromReminder = true)
                    JournalTemplateReminders.GRATITUDE_ID ->
                        Screen.GratitudeJournal.createRoute(fromReminder = true)
                    JournalTemplateReminders.REFLECTION_ID ->
                        Screen.ReflectionJournal.createRoute(fromReminder = true)
                    else -> Screen.Journal.route
                }
            }
            "ENTRY" -> Screen.JournalEditor.createRoute(entryId = reminder.linkedItemId)
            "NOTE" -> Screen.NoteEditor.createRoute(noteId = reminder.linkedItemId)
            "TRANSACTION" -> Screen.TransactionEditor.createRoute(transactionId = reminder.linkedItemId)
            else -> Screen.Dashboard.route
        }
    }

    companion object {
        const val WORK_NAME_PREFIX = "reminder_"
        private const val TAG = "ReminderWorker"
    }
}
