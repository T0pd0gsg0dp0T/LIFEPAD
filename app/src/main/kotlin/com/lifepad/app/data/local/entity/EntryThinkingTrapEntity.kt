package com.lifepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_thinking_traps",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("entryId"),
        Index("trapType")
    ]
)
data class EntryThinkingTrapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryId: Long,
    val trapType: String
)
