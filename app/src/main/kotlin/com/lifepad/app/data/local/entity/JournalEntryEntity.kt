package com.lifepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_entries",
    indices = [
        Index("entryDate"),
        Index("mood"),
        Index("updatedAt")
    ]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val mood: Int, // 1-10 scale
    val template: String = "free", // thought_record, gratitude, reflection, free
    val entryDate: Long, // Date of the entry (user can backdate)
    val isPinned: Boolean = false,
    val structuredData: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
