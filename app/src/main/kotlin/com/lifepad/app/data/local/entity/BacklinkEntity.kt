package com.lifepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "backlinks",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceNoteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetNoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sourceNoteId"),
        Index("targetNoteId"),
        Index(value = ["sourceNoteId", "targetNoteId"], unique = true)
    ]
)
data class BacklinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceNoteId: Long,
    val targetNoteId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
