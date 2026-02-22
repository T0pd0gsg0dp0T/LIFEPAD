package com.lifepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String? = null,
    val type: CategoryType = CategoryType.EXPENSE,
    val color: Int = 0xFF8E8E8E.toInt(),
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class CategoryType {
    INCOME,
    EXPENSE
}
