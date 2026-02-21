package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.AttachmentDao
import com.lifepad.app.data.local.dao.DailyMoodRow
import com.lifepad.app.data.local.dao.EmotionAvgRow
import com.lifepad.app.data.local.dao.EmotionFrequencyRow
import com.lifepad.app.data.local.dao.EntryEmotionDao
import com.lifepad.app.data.local.dao.EntryThinkingTrapDao
import com.lifepad.app.data.local.dao.HashtagDao
import com.lifepad.app.data.local.dao.JournalEntryDao
import com.lifepad.app.data.local.dao.MoodCountRow
import com.lifepad.app.data.local.dao.MoodDataRow
import com.lifepad.app.data.local.dao.TemplateCountRow
import com.lifepad.app.data.local.dao.TrapFrequencyRow
import com.lifepad.app.data.local.entity.AttachmentEntity
import com.lifepad.app.data.local.entity.EntryEmotionEntity
import com.lifepad.app.data.local.entity.EntryThinkingTrapEntity
import com.lifepad.app.data.local.entity.ItemType
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.domain.cbt.EmotionRating
import com.lifepad.app.domain.cbt.ThinkingTrap
import com.lifepad.app.domain.parser.HashtagParser
import com.lifepad.app.domain.parser.WikilinkParser
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class JournalRepository @Inject constructor(
    private val journalEntryDao: JournalEntryDao,
    private val hashtagDao: HashtagDao,
    private val entryEmotionDao: EntryEmotionDao,
    private val entryThinkingTrapDao: EntryThinkingTrapDao,
    private val attachmentDao: AttachmentDao
) {
    // ...

    fun getAttachmentsForEntry(entryId: Long): Flow<List<AttachmentEntity>> =
        attachmentDao.getAttachmentsForItem(entryId, "journal_entry")

    suspend fun addAttachment(entryId: Long, filePath: String) {
        val attachment = AttachmentEntity(
            itemId = entryId,
            itemType = "journal_entry",
            filePath = filePath
        )
        attachmentDao.insert(attachment)
    }

    suspend fun deleteAttachment(attachment: AttachmentEntity) {
        attachmentDao.delete(attachment)
    }

    // ...
    fun getAllEntries(): Flow<List<JournalEntryEntity>> = journalEntryDao.getAllEntries()

    fun getEntriesByDateRange(startDate: Long, endDate: Long): Flow<List<JournalEntryEntity>> =
        journalEntryDao.getEntriesByDateRange(startDate, endDate)

    fun getEntriesByMood(mood: Int): Flow<List<JournalEntryEntity>> =
        journalEntryDao.getEntriesByMood(mood)

    fun getEntriesByTemplate(template: String): Flow<List<JournalEntryEntity>> =
        journalEntryDao.getEntriesByTemplate(template)

    fun getPinnedEntries(): Flow<List<JournalEntryEntity>> = journalEntryDao.getPinnedEntries()

    fun observeEntry(entryId: Long): Flow<JournalEntryEntity?> = journalEntryDao.observeEntryById(entryId)

    suspend fun getEntryById(entryId: Long): JournalEntryEntity? = journalEntryDao.getEntryById(entryId)

    suspend fun getRecentEntries(limit: Int = 50): List<JournalEntryEntity> =
        journalEntryDao.getRecentEntries(limit)

    suspend fun searchEntries(query: String): List<JournalEntryEntity> =
        journalEntryDao.searchEntries("$query*")

    suspend fun searchEntriesByHashtagQuery(hashtagQuery: String): List<JournalEntryEntity> {
        val normalized = hashtagQuery.removePrefix("#").lowercase().trim()
        if (normalized.isBlank()) return emptyList()
        val hashtag = hashtagDao.getHashtagByName(normalized) ?: return emptyList()
        val entryIds = hashtagDao.getItemIdsWithHashtag(hashtag.id, ItemType.ENTRY)
        if (entryIds.isEmpty()) return emptyList()
        return journalEntryDao.getEntriesByIds(entryIds)
    }

    suspend fun searchEntriesByWikilinkQuery(wikilinkQuery: String): List<JournalEntryEntity> {
        val targetTitle = WikilinkParser.extractWikilinks(wikilinkQuery).firstOrNull()
            ?: wikilinkQuery.removePrefix("[[").removeSuffix("]]").trim()
        if (targetTitle.isBlank()) return emptyList()
        return getEntriesReferencingNoteTitle(targetTitle)
    }

    suspend fun getEntriesReferencingNoteTitle(noteTitle: String): List<JournalEntryEntity> {
        if (noteTitle.isBlank()) return emptyList()
        val rawMatches = journalEntryDao.getEntriesByContentPattern("%[[${noteTitle}%]]%")
        return rawMatches.filter { entry ->
            WikilinkParser.extractWikilinks(entry.content).any { it.equals(noteTitle, ignoreCase = true) }
        }
    }

    fun observeEntriesReferencingNoteTitle(noteTitle: String): Flow<List<JournalEntryEntity>> {
        if (noteTitle.isBlank()) return flowOf(emptyList())
        return journalEntryDao.observeEntriesByContentPattern("%[[${noteTitle}%]]%").map { entries ->
            entries.filter { entry ->
                WikilinkParser.extractWikilinks(entry.content).any { it.equals(noteTitle, ignoreCase = true) }
            }
        }
    }

    suspend fun saveEntry(entry: JournalEntryEntity): Long {
        val updatedEntry = entry.copy(updatedAt = System.currentTimeMillis())
        val entryId = journalEntryDao.insert(updatedEntry)
        val savedEntryId = if (entry.id == 0L) entryId else entry.id

        // Sync hashtags
        val hashtags = HashtagParser.extractHashtags(entry.content)
        hashtagDao.syncHashtagsForItem(ItemType.ENTRY, savedEntryId, hashtags)

        return savedEntryId
    }

    suspend fun deleteEntry(entryId: Long) {
        journalEntryDao.deleteById(entryId)
    }

    suspend fun togglePin(entryId: Long) {
        val entry = journalEntryDao.getEntryById(entryId) ?: return
        journalEntryDao.update(entry.copy(isPinned = !entry.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun getEntryCount(): Int = journalEntryDao.getEntryCount()

    suspend fun getAverageMood(startDate: Long, endDate: Long): Double? =
        journalEntryDao.getAverageMood(startDate, endDate)

    fun getHashtagsForEntry(entryId: Long) = hashtagDao.observeHashtagsForItem(ItemType.ENTRY, entryId)

    suspend fun getMoodTrend(startDate: Long, endDate: Long): List<MoodDataRow> =
        journalEntryDao.getMoodTrend(startDate, endDate)

    suspend fun getMoodDistribution(startDate: Long, endDate: Long): List<MoodCountRow> =
        journalEntryDao.getMoodDistribution(startDate, endDate)

    suspend fun getDailyMoodMap(startDate: Long, endDate: Long): List<DailyMoodRow> =
        journalEntryDao.getDailyMoodMap(startDate, endDate)

    suspend fun getTemplateDistribution(): List<TemplateCountRow> =
        journalEntryDao.getTemplateDistribution()

    suspend fun getMostCommonMood(): MoodCountRow? =
        journalEntryDao.getMostCommonMood()

    suspend fun calculateStreaks(): Pair<Int, Int> {
        val dates = journalEntryDao.getAllEntryDates()
        if (dates.isEmpty()) return Pair(0, 0)

        // Normalize to day boundaries
        val daySet = dates.map { normalizeToDay(it) }.toSortedSet().toList().sortedDescending()
        if (daySet.isEmpty()) return Pair(0, 0)

        val today = normalizeToDay(System.currentTimeMillis())
        val oneDayMs = 86400000L

        // Current streak: walk backwards from today
        var currentStreak = 0
        var checkDay = today
        for (day in daySet) {
            if (day == checkDay) {
                currentStreak++
                checkDay -= oneDayMs
            } else if (day < checkDay) {
                break
            }
        }

        // Longest streak
        var longestStreak = 1
        var streak = 1
        val sortedAsc = daySet.sorted()
        for (i in 1 until sortedAsc.size) {
            if (sortedAsc[i] - sortedAsc[i - 1] == oneDayMs) {
                streak++
                if (streak > longestStreak) longestStreak = streak
            } else {
                streak = 1
            }
        }

        return Pair(currentStreak, longestStreak)
    }

    suspend fun getAverageWordCount(): Int {
        val contents = journalEntryDao.getAllEntryContents()
        if (contents.isEmpty()) return 0
        val totalWords = contents.sumOf { it.split(Regex("\\s+")).count { word -> word.isNotBlank() } }
        return totalWords / contents.size
    }

    // --- Emotions ---

    fun getEmotionsForEntry(entryId: Long): Flow<List<EntryEmotionEntity>> =
        entryEmotionDao.getForEntry(entryId)

    suspend fun getEmotionsByPhase(entryId: Long, phase: String): List<EntryEmotionEntity> =
        entryEmotionDao.getForEntryByPhase(entryId, phase)

    suspend fun saveEmotions(entryId: Long, emotions: List<EmotionRating>, phase: String) {
        entryEmotionDao.deleteForEntryAndPhase(entryId, phase)
        if (emotions.isNotEmpty()) {
            entryEmotionDao.insertAll(
                emotions.map { emotion ->
                    EntryEmotionEntity(
                        entryId = entryId,
                        emotionName = emotion.name,
                        intensity = emotion.intensity,
                        phase = phase
                    )
                }
            )
        }
    }

    suspend fun getEmotionFrequency(startDate: Long, endDate: Long): List<EmotionFrequencyRow> =
        entryEmotionDao.getEmotionFrequency(startDate, endDate)

    suspend fun getAverageIntensityByEmotion(startDate: Long, endDate: Long): List<EmotionAvgRow> =
        entryEmotionDao.getAverageIntensityByEmotion(startDate, endDate)

    // --- Thinking Traps ---

    fun getTrapsForEntry(entryId: Long): Flow<List<EntryThinkingTrapEntity>> =
        entryThinkingTrapDao.getForEntry(entryId)

    suspend fun saveThinkingTraps(entryId: Long, traps: Set<ThinkingTrap>) {
        entryThinkingTrapDao.deleteForEntry(entryId)
        if (traps.isNotEmpty()) {
            entryThinkingTrapDao.insertAll(
                traps.map { trap ->
                    EntryThinkingTrapEntity(
                        entryId = entryId,
                        trapType = trap.name
                    )
                }
            )
        }
    }

    suspend fun getTrapFrequency(startDate: Long, endDate: Long): List<TrapFrequencyRow> =
        entryThinkingTrapDao.getTrapFrequency(startDate, endDate)

    private fun normalizeToDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
