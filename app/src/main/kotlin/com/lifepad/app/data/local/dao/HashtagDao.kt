package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lifepad.app.data.local.entity.HashtagEntity
import com.lifepad.app.data.local.entity.HashtagUsageEntity
import com.lifepad.app.data.local.entity.HashtagUsageName
import com.lifepad.app.data.local.entity.ItemType
import kotlinx.coroutines.flow.Flow

@Dao
interface HashtagDao {
    @Query("SELECT * FROM hashtags ORDER BY usageCount DESC")
    fun getAllHashtags(): Flow<List<HashtagEntity>>

    @Query("SELECT * FROM hashtags WHERE usageCount > 0 ORDER BY usageCount DESC LIMIT :limit")
    fun getTopHashtags(limit: Int): Flow<List<HashtagEntity>>

    @Query("SELECT * FROM hashtags WHERE name LIKE :prefix || '%' ORDER BY usageCount DESC LIMIT 10")
    suspend fun searchHashtags(prefix: String): List<HashtagEntity>

    @Query("SELECT * FROM hashtags WHERE name = :name LIMIT 1")
    suspend fun getHashtagByName(name: String): HashtagEntity?

    @Query("SELECT * FROM hashtags WHERE id = :id")
    suspend fun getHashtagById(id: Long): HashtagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(hashtag: HashtagEntity): Long

    @Query("UPDATE hashtags SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: Long)

    @Query("UPDATE hashtags SET usageCount = usageCount - 1 WHERE id = :id AND usageCount > 0")
    suspend fun decrementUsageCount(id: Long)

    // Get or create a hashtag
    @Transaction
    suspend fun getOrCreate(name: String): HashtagEntity {
        val normalized = name.lowercase().trim()
        val existing = getHashtagByName(normalized)
        if (existing != null) return existing
        val id = insert(HashtagEntity(name = normalized))
        return getHashtagById(id) ?: HashtagEntity(id = id, name = normalized)
    }

    // HashtagUsage operations
    @Query("SELECT * FROM hashtag_usage WHERE hashtagId = :hashtagId")
    fun getUsagesForHashtag(hashtagId: Long): Flow<List<HashtagUsageEntity>>

    @Query("SELECT * FROM hashtag_usage WHERE itemType = :itemType AND itemId = :itemId")
    suspend fun getUsagesForItem(itemType: ItemType, itemId: Long): List<HashtagUsageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsage(usage: HashtagUsageEntity): Long

    @Query("DELETE FROM hashtag_usage WHERE itemType = :itemType AND itemId = :itemId")
    suspend fun deleteUsagesForItem(itemType: ItemType, itemId: Long)

    @Query("""
        SELECT hashtags.* FROM hashtags
        INNER JOIN hashtag_usage ON hashtags.id = hashtag_usage.hashtagId
        WHERE hashtag_usage.itemType = :itemType AND hashtag_usage.itemId = :itemId
    """)
    suspend fun getHashtagsForItem(itemType: ItemType, itemId: Long): List<HashtagEntity>

    @Query("""
        SELECT hashtags.* FROM hashtags
        INNER JOIN hashtag_usage ON hashtags.id = hashtag_usage.hashtagId
        WHERE hashtag_usage.itemType = :itemType AND hashtag_usage.itemId = :itemId
    """)
    fun observeHashtagsForItem(itemType: ItemType, itemId: Long): Flow<List<HashtagEntity>>

    @Query("""
        SELECT hu.itemId AS itemId, h.name AS name
        FROM hashtag_usage hu
        INNER JOIN hashtags h ON h.id = hu.hashtagId
        WHERE hu.itemType = :itemType
    """)
    fun observeHashtagNamesForItemType(itemType: ItemType): Flow<List<HashtagUsageName>>

    // Cross-module queries
    @Query("""
        SELECT DISTINCT hu.itemId FROM hashtag_usage hu
        WHERE hu.hashtagId = :hashtagId AND hu.itemType = :itemType
    """)
    suspend fun getItemIdsWithHashtag(hashtagId: Long, itemType: ItemType): List<Long>

    @Query("""
        SELECT COUNT(*) FROM hashtag_usage
        WHERE hashtagId = :hashtagId
    """)
    suspend fun getHashtagUsageCount(hashtagId: Long): Int

    // Sync hashtags for an item
    @Transaction
    suspend fun syncHashtagsForItem(itemType: ItemType, itemId: Long, hashtagNames: List<String>) {
        // Get current hashtags
        val currentUsages = getUsagesForItem(itemType, itemId)
        val currentHashtagIds = currentUsages.map { it.hashtagId }.toSet()

        // Get or create new hashtags
        val newHashtags = hashtagNames.map { getOrCreate(it) }
        val newHashtagIds = newHashtags.map { it.id }.toSet()

        // Remove old usages
        val toRemove = currentHashtagIds - newHashtagIds
        toRemove.forEach { hashtagId ->
            decrementUsageCount(hashtagId)
        }
        deleteUsagesForItem(itemType, itemId)

        // Add new usages
        newHashtags.forEach { hashtag ->
            val wasNew = insertUsage(
                HashtagUsageEntity(
                    hashtagId = hashtag.id,
                    itemType = itemType,
                    itemId = itemId
                )
            )
            if (wasNew > 0 && hashtag.id !in currentHashtagIds) {
                incrementUsageCount(hashtag.id)
            }
        }
    }
}
