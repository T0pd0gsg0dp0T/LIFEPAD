package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.ReminderDao
import com.lifepad.app.data.local.entity.ReminderEntity
import com.lifepad.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler
) {
    suspend fun save(reminder: ReminderEntity): Long {
        val id = reminderDao.insert(reminder)
        val savedReminder = if (reminder.id == 0L) reminder.copy(id = id) else reminder
        if (savedReminder.isEnabled) {
            reminderScheduler.schedule(savedReminder)
        }
        return id
    }

    suspend fun delete(reminderId: Long) {
        reminderScheduler.cancel(reminderId)
        reminderDao.deleteById(reminderId)
    }

    suspend fun toggleEnabled(reminderId: Long) {
        val reminder = reminderDao.getById(reminderId) ?: return
        val updated = reminder.copy(
            isEnabled = !reminder.isEnabled,
            updatedAt = System.currentTimeMillis()
        )
        reminderDao.update(updated)
        if (updated.isEnabled) {
            reminderScheduler.schedule(updated)
        } else {
            reminderScheduler.cancel(reminderId)
        }
    }

    fun getAll(): Flow<List<ReminderEntity>> = reminderDao.getAll()

    fun getForItem(itemType: String, itemId: Long): Flow<List<ReminderEntity>> =
        reminderDao.getForItem(itemType, itemId)

    suspend fun getById(id: Long): ReminderEntity? = reminderDao.getById(id)

    suspend fun rescheduleAll() {
        val upcoming = reminderDao.getUpcoming(System.currentTimeMillis())
        upcoming.forEach { reminderScheduler.schedule(it) }
    }
}
