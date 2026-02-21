package com.lifepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
