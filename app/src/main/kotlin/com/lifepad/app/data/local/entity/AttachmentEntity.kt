package com.lifepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    indices = [Index("itemId", "itemType")],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val itemType: String, // "journal_entry" or "transaction"
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
)
