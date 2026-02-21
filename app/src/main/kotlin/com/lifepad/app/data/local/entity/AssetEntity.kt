package com.lifepad.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AssetType {
    PROPERTY, VEHICLE, INVESTMENT, SAVINGS, OTHER
}

@Entity(
    tableName = "assets",
    indices = [
        Index("assetType"),
        Index("isLiability")
    ]
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val value: Double,
    val assetType: AssetType = AssetType.OTHER,
    @ColumnInfo(defaultValue = "0")
    val isLiability: Boolean = false,
    @ColumnInfo(defaultValue = "")
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
