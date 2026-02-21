package com.lifepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = JournalEntryEntity::class)
@Entity(tableName = "journal_entries_fts")
data class JournalEntryFts(
    val content: String
)
