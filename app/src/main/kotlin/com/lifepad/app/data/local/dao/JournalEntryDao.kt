package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifepad.app.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

data class MoodDataRow(
    val mood: Int,
    val entryDate: Long
)

data class MoodCountRow(
    val mood: Int,
    val count: Int
)

data class DailyMoodRow(
    val dayTimestamp: Long,
    val avgMood: Double
)

data class TemplateCountRow(
    val template: String,
    val count: Int
)

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries ORDER BY entryDate DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE entryDate BETWEEN :startDate AND :endDate ORDER BY entryDate DESC")
    fun getEntriesByDateRange(startDate: Long, endDate: Long): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE mood = :mood ORDER BY entryDate DESC")
    fun getEntriesByMood(mood: Int): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE isPinned = 1 ORDER BY entryDate DESC")
    fun getPinnedEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    fun observeEntryById(id: Long): Flow<JournalEntryEntity?>

    @Query("SELECT * FROM journal_entries WHERE template = :template ORDER BY entryDate DESC")
    fun getEntriesByTemplate(template: String): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntryEntity): Long

    @Update
    suspend fun update(entry: JournalEntryEntity)

    @Delete
    suspend fun delete(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM journal_entries ORDER BY entryDate DESC LIMIT :limit")
    suspend fun getRecentEntries(limit: Int = 50): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries WHERE id IN (:ids) ORDER BY entryDate DESC")
    suspend fun getEntriesByIds(ids: List<Long>): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries WHERE content LIKE :pattern ORDER BY entryDate DESC")
    suspend fun getEntriesByContentPattern(pattern: String): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries WHERE content LIKE :pattern ORDER BY entryDate DESC")
    fun observeEntriesByContentPattern(pattern: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT COUNT(*) FROM journal_entries")
    suspend fun getEntryCount(): Int

    @Query("SELECT AVG(mood) FROM journal_entries WHERE entryDate BETWEEN :startDate AND :endDate")
    suspend fun getAverageMood(startDate: Long, endDate: Long): Double?

    @Query("SELECT mood, entryDate FROM journal_entries WHERE entryDate BETWEEN :startDate AND :endDate ORDER BY entryDate ASC")
    suspend fun getMoodTrend(startDate: Long, endDate: Long): List<MoodDataRow>

    @Query("SELECT mood, COUNT(*) AS count FROM journal_entries WHERE entryDate BETWEEN :startDate AND :endDate GROUP BY mood ORDER BY mood ASC")
    suspend fun getMoodDistribution(startDate: Long, endDate: Long): List<MoodCountRow>

    // Daily mood map for calendar heatmap (group by day using integer division)
    @Query("""
        SELECT (entryDate / 86400000) * 86400000 AS dayTimestamp, AVG(mood) AS avgMood
        FROM journal_entries
        WHERE entryDate BETWEEN :startDate AND :endDate
        GROUP BY entryDate / 86400000
        ORDER BY dayTimestamp ASC
    """)
    suspend fun getDailyMoodMap(startDate: Long, endDate: Long): List<DailyMoodRow>

    // Template distribution
    @Query("SELECT template, COUNT(*) AS count FROM journal_entries GROUP BY template ORDER BY count DESC")
    suspend fun getTemplateDistribution(): List<TemplateCountRow>

    // All entry dates for streak calculation
    @Query("SELECT entryDate FROM journal_entries ORDER BY entryDate DESC")
    suspend fun getAllEntryDates(): List<Long>

    // Most common mood
    @Query("SELECT mood, COUNT(*) AS count FROM journal_entries GROUP BY mood ORDER BY count DESC LIMIT 1")
    suspend fun getMostCommonMood(): MoodCountRow?

    // Total word count across all entries
    @Query("SELECT content FROM journal_entries")
    suspend fun getAllEntryContents(): List<String>

    // FTS search
    @Query("""
        SELECT journal_entries.* FROM journal_entries
        JOIN journal_entries_fts ON journal_entries.rowid = journal_entries_fts.rowid
        WHERE journal_entries_fts MATCH :query
        ORDER BY journal_entries.entryDate DESC
        LIMIT 200
    """)
    suspend fun searchEntries(query: String): List<JournalEntryEntity>
}
