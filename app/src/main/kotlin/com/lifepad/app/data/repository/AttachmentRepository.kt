package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.AttachmentDao
import com.lifepad.app.data.local.entity.AttachmentEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class AttachmentRepository @Inject constructor(
    private val attachmentDao: AttachmentDao
) {
    fun getAttachments(itemId: Long, itemType: String): Flow<List<AttachmentEntity>> =
        attachmentDao.getAttachmentsForItem(itemId, itemType)

    suspend fun addAttachment(itemId: Long, itemType: String, filePath: String): Long {
        return attachmentDao.insert(
            AttachmentEntity(
                itemId = itemId,
                itemType = itemType,
                filePath = filePath
            )
        )
    }

    suspend fun deleteAttachment(attachment: AttachmentEntity) {
        attachmentDao.delete(attachment)
    }
}
