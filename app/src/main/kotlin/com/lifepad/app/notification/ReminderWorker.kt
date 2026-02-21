package com.lifepad.app.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifepad.app.data.local.dao.ReminderDao
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

        notificationHelper.showNotification(
            id = reminder.id.toInt(),
            title = reminder.title,
            message = reminder.message
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

    companion object {
        const val WORK_NAME_PREFIX = "reminder_"
        private const val TAG = "ReminderWorker"
    }
}
