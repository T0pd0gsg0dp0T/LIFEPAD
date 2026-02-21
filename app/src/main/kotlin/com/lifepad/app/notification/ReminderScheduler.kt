package com.lifepad.app.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lifepad.app.data.local.entity.ReminderEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedule(reminder: ReminderEntity) {
        val delay = (reminder.triggerTime - System.currentTimeMillis()).coerceAtLeast(0)

        val inputData = Data.Builder()
            .putLong("reminder_id", reminder.id)
            .build()

        val builder = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(inputData)

        if (delay > 0) {
            builder.setInitialDelay(delay, TimeUnit.MILLISECONDS)
        }

        val workRequest = builder.build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${ReminderWorker.WORK_NAME_PREFIX}${reminder.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancel(reminderId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(
            "${ReminderWorker.WORK_NAME_PREFIX}$reminderId"
        )
    }
}
