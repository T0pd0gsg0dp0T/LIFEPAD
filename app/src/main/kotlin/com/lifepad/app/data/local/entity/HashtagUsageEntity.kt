package com.lifepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hashtag_usage",
    foreignKeys = [
        ForeignKey(
            entity = HashtagEntity::class,
            parentColumns = ["id"],
            childColumns = ["hashtagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("hashtagId"),
        Index(value = ["itemType", "itemId"]),
        Index(value = ["hashtagId", "itemType", "itemId"], unique = true)
    ]
)
data class HashtagUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hashtagId: Long,
    val itemType: ItemType,
    val itemId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
