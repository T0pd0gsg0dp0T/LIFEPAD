package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepad.app.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE itemId = :itemId AND itemType = :itemType")
    fun getAttachmentsForItem(itemId: Long, itemType: String): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity): Long

    @Delete
    suspend fun delete(attachment: AttachmentEntity)
}
