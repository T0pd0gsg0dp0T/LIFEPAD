package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifepad.app.data.local.entity.RecurringBillEntity
import kotlinx.coroutines.flow.Flow

data class RecurringBillWithCategory(
    val id: Long,
    val name: String,
    val amount: Double,
    val transactionType: String,
    val categoryId: Long?,
    val accountId: Long?,
    val frequency: String,
    val nextDueDate: Long,
    val dayOfMonth: Int?,
    val isConfirmed: Boolean,
    val isEnabled: Boolean,
    val reminderId: Long?,
    val detectedFromCount: Int,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val categoryName: String?
)

@Dao
interface RecurringBillDao {
    @Query("SELECT * FROM recurring_bills ORDER BY nextDueDate ASC")
    fun getAllBills(): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills WHERE isConfirmed = 1 AND isEnabled = 1 ORDER BY nextDueDate ASC")
    fun getConfirmedBills(): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills WHERE isConfirmed = 1 AND isEnabled = 1 AND nextDueDate <= :endDate ORDER BY nextDueDate ASC")
    fun getBillsUpToDate(endDate: Long): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: Long): RecurringBillEntity?

    @Query("""
        SELECT rb.*, c.name AS categoryName
        FROM recurring_bills rb
        LEFT JOIN categories c ON c.id = rb.categoryId
        ORDER BY rb.nextDueDate ASC
    """)
    fun getBillsWithCategories(): Flow<List<RecurringBillWithCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bill: RecurringBillEntity): Long

    @Update
    suspend fun update(bill: RecurringBillEntity)

    @Query("DELETE FROM recurring_bills WHERE id = :id")
    suspend fun deleteById(id: Long)
}
