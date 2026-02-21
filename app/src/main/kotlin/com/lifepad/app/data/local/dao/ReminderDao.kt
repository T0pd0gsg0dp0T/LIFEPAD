package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifepad.app.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 ORDER BY triggerTime ASC")
    fun getEnabled(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    fun getAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE linkedItemType = :itemType AND linkedItemId = :itemId")
    fun getForItem(itemType: String, itemId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND triggerTime > :now ORDER BY triggerTime ASC")
    suspend fun getUpcoming(now: Long): List<ReminderEntity>
}
