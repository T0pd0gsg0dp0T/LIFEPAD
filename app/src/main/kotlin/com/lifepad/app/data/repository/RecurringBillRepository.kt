package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.RecurringBillDao
import com.lifepad.app.data.local.dao.RecurringBillWithCategory
import com.lifepad.app.data.local.dao.ReminderDao
import com.lifepad.app.data.local.entity.BillFrequency
import com.lifepad.app.data.local.entity.RecurringBillEntity
import com.lifepad.app.data.local.entity.ReminderEntity
import com.lifepad.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringBillRepository @Inject constructor(
    private val recurringBillDao: RecurringBillDao,
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler
) {
    fun getAllBills(): Flow<List<RecurringBillEntity>> = recurringBillDao.getAllBills()

    fun getConfirmedBills(): Flow<List<RecurringBillEntity>> = recurringBillDao.getConfirmedBills()

    fun getBillsUpToDate(endDate: Long): Flow<List<RecurringBillEntity>> =
        recurringBillDao.getBillsUpToDate(endDate)

    fun getBillsWithCategories(): Flow<List<RecurringBillWithCategory>> =
        recurringBillDao.getBillsWithCategories()

    suspend fun getBillById(id: Long): RecurringBillEntity? = recurringBillDao.getBillById(id)

    suspend fun saveBill(bill: RecurringBillEntity): Long {
        val updated = bill.copy(updatedAt = System.currentTimeMillis())
        val billId = recurringBillDao.insert(updated)
        val savedId = if (bill.id == 0L) billId else bill.id

        // Schedule reminder if bill is confirmed and enabled
        if (updated.isConfirmed && updated.isEnabled) {
            scheduleReminderForBill(updated.copy(id = savedId))
        }

        return savedId
    }

    suspend fun confirmBill(bill: RecurringBillEntity): Long {
        val confirmed = bill.copy(
            isConfirmed = true,
            updatedAt = System.currentTimeMillis()
        )
        val savedId = recurringBillDao.insert(confirmed)
        val id = if (bill.id == 0L) savedId else bill.id
        scheduleReminderForBill(confirmed.copy(id = id))
        return id
    }

    suspend fun deleteBill(id: Long) {
        val bill = recurringBillDao.getBillById(id)
        if (bill?.reminderId != null) {
            reminderScheduler.cancel(bill.reminderId)
            reminderDao.deleteById(bill.reminderId)
        }
        recurringBillDao.deleteById(id)
    }

    suspend fun toggleEnabled(id: Long) {
        val bill = recurringBillDao.getBillById(id) ?: return
        val toggled = bill.copy(
            isEnabled = !bill.isEnabled,
            updatedAt = System.currentTimeMillis()
        )
        recurringBillDao.update(toggled)

        if (toggled.isEnabled && toggled.isConfirmed) {
            scheduleReminderForBill(toggled)
        } else if (!toggled.isEnabled && toggled.reminderId != null) {
            reminderScheduler.cancel(toggled.reminderId)
        }
    }

    suspend fun advanceBillDueDate(bill: RecurringBillEntity): RecurringBillEntity {
        val cal = Calendar.getInstance().apply { timeInMillis = bill.nextDueDate }
        when (bill.frequency) {
            BillFrequency.WEEKLY -> cal.add(Calendar.DAY_OF_MONTH, 7)
            BillFrequency.BIWEEKLY -> cal.add(Calendar.DAY_OF_MONTH, 14)
            BillFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            BillFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        val advanced = bill.copy(
            nextDueDate = cal.timeInMillis,
            updatedAt = System.currentTimeMillis()
        )
        recurringBillDao.update(advanced)
        if (advanced.isConfirmed && advanced.isEnabled) {
            scheduleReminderForBill(advanced)
        }
        return advanced
    }

    private suspend fun scheduleReminderForBill(bill: RecurringBillEntity) {
        // Schedule reminder 1 day before due date
        val triggerTime = bill.nextDueDate - TimeUnit.DAYS.toMillis(1)
        if (triggerTime <= System.currentTimeMillis()) return

        // Cancel existing reminder if any
        if (bill.reminderId != null) {
            reminderScheduler.cancel(bill.reminderId)
        }

        val reminder = ReminderEntity(
            id = bill.reminderId ?: 0L,
            title = "Bill Due Tomorrow: ${bill.name}",
            message = "$${String.format(Locale.getDefault(), "%.2f", bill.amount)} due tomorrow",
            triggerTime = triggerTime,
            repeatInterval = 0,
            isEnabled = true,
            linkedItemType = "BILL",
            linkedItemId = bill.id
        )
        val reminderId = reminderDao.insert(reminder)
        val savedReminderId = if (reminder.id == 0L) reminderId else reminder.id

        // Update bill with reminder ID
        recurringBillDao.update(bill.copy(reminderId = savedReminderId))

        reminderScheduler.schedule(reminder.copy(id = savedReminderId))
    }
}
