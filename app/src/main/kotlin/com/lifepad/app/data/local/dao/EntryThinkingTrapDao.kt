package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepad.app.data.local.entity.EntryThinkingTrapEntity
import kotlinx.coroutines.flow.Flow

data class TrapFrequencyRow(val trapType: String, val count: Int)

@Dao
interface EntryThinkingTrapDao {

    @Query("SELECT * FROM entry_thinking_traps WHERE entryId = :entryId")
    fun getForEntry(entryId: Long): Flow<List<EntryThinkingTrapEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(traps: List<EntryThinkingTrapEntity>)

    @Query("DELETE FROM entry_thinking_traps WHERE entryId = :entryId")
    suspend fun deleteForEntry(entryId: Long)

    @Query("""
        SELECT ett.trapType, COUNT(*) as count
        FROM entry_thinking_traps ett
        INNER JOIN journal_entries je ON ett.entryId = je.id
        WHERE je.entryDate BETWEEN :startDate AND :endDate
        GROUP BY ett.trapType
        ORDER BY count DESC
    """)
    suspend fun getTrapFrequency(startDate: Long, endDate: Long): List<TrapFrequencyRow>
}
