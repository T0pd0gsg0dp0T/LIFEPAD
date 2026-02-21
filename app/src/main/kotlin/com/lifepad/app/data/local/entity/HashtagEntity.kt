package com.lifepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hashtags",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class HashtagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // Lowercase, without # prefix
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
