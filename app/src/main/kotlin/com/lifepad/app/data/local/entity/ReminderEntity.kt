package com.lifepad.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    indices = [
        Index("triggerTime"),
        Index("linkedItemType", "linkedItemId")
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(defaultValue = "")
    val message: String = "",
    val triggerTime: Long,
    @ColumnInfo(defaultValue = "0")
    val repeatInterval: Long = 0,
    @ColumnInfo(defaultValue = "1")
    val isEnabled: Boolean = true,
    val linkedItemType: String, // "NOTE", "ENTRY", "TRANSACTION"
    val linkedItemId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
