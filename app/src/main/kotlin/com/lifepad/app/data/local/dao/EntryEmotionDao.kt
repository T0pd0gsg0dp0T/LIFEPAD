package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepad.app.data.local.entity.EntryEmotionEntity
import kotlinx.coroutines.flow.Flow

data class EmotionFrequencyRow(val emotionName: String, val count: Int)
data class EmotionAvgRow(val emotionName: String, val avgIntensity: Double)

@Dao
interface EntryEmotionDao {

    @Query("SELECT * FROM entry_emotions WHERE entryId = :entryId")
    fun getForEntry(entryId: Long): Flow<List<EntryEmotionEntity>>

    @Query("SELECT * FROM entry_emotions WHERE entryId = :entryId AND phase = :phase")
    suspend fun getForEntryByPhase(entryId: Long, phase: String): List<EntryEmotionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(emotions: List<EntryEmotionEntity>)

    @Query("DELETE FROM entry_emotions WHERE entryId = :entryId")
    suspend fun deleteForEntry(entryId: Long)

    @Query("DELETE FROM entry_emotions WHERE entryId = :entryId AND phase = :phase")
    suspend fun deleteForEntryAndPhase(entryId: Long, phase: String)

    @Query("""
        SELECT ee.emotionName, COUNT(*) as count
        FROM entry_emotions ee
        INNER JOIN journal_entries je ON ee.entryId = je.id
        WHERE je.entryDate BETWEEN :startDate AND :endDate
        GROUP BY ee.emotionName
        ORDER BY count DESC
    """)
    suspend fun getEmotionFrequency(startDate: Long, endDate: Long): List<EmotionFrequencyRow>

    @Query("""
        SELECT ee.emotionName, AVG(ee.intensity) as avgIntensity
        FROM entry_emotions ee
        INNER JOIN journal_entries je ON ee.entryId = je.id
        WHERE je.entryDate BETWEEN :startDate AND :endDate
        GROUP BY ee.emotionName
        ORDER BY avgIntensity DESC
    """)
    suspend fun getAverageIntensityByEmotion(startDate: Long, endDate: Long): List<EmotionAvgRow>
}
