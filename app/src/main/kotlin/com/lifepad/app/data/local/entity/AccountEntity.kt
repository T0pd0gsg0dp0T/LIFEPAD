package com.lifepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val balance: Double = 0.0,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
