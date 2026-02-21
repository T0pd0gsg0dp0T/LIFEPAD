package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.HashtagDao
import com.lifepad.app.data.local.entity.HashtagEntity
import com.lifepad.app.data.local.entity.ItemType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HashtagRepository @Inject constructor(
    private val hashtagDao: HashtagDao
) {
    fun getAllHashtags(): Flow<List<HashtagEntity>> = hashtagDao.getAllHashtags()

    fun getTopHashtags(limit: Int = 20): Flow<List<HashtagEntity>> = hashtagDao.getTopHashtags(limit)

    suspend fun searchHashtags(prefix: String): List<HashtagEntity> =
        hashtagDao.searchHashtags(prefix.lowercase())

    suspend fun getHashtagByName(name: String): HashtagEntity? =
        hashtagDao.getHashtagByName(name.lowercase())

    suspend fun getOrCreateHashtag(name: String): HashtagEntity =
        hashtagDao.getOrCreate(name.lowercase())

    suspend fun getHashtagsForNote(noteId: Long): List<HashtagEntity> =
        hashtagDao.getHashtagsForItem(ItemType.NOTE, noteId)

    suspend fun getHashtagsForEntry(entryId: Long): List<HashtagEntity> =
        hashtagDao.getHashtagsForItem(ItemType.ENTRY, entryId)

    suspend fun getHashtagsForTransaction(transactionId: Long): List<HashtagEntity> =
        hashtagDao.getHashtagsForItem(ItemType.TRANSACTION, transactionId)

    suspend fun getNoteIdsWithHashtag(hashtagId: Long): List<Long> =
        hashtagDao.getItemIdsWithHashtag(hashtagId, ItemType.NOTE)

    suspend fun getEntryIdsWithHashtag(hashtagId: Long): List<Long> =
        hashtagDao.getItemIdsWithHashtag(hashtagId, ItemType.ENTRY)

    suspend fun getTransactionIdsWithHashtag(hashtagId: Long): List<Long> =
        hashtagDao.getItemIdsWithHashtag(hashtagId, ItemType.TRANSACTION)

    suspend fun getHashtagUsageCount(hashtagId: Long): Int =
        hashtagDao.getHashtagUsageCount(hashtagId)
}
