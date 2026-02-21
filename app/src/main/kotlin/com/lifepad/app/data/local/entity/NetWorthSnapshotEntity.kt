package com.lifepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "net_worth_snapshots",
    indices = [
        Index(value = ["snapshotDate"], unique = true)
    ]
)
data class NetWorthSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val snapshotDate: Long,
    val accountsTotal: Double,
    val assetsTotal: Double,
    val liabilitiesTotal: Double,
    val netWorth: Double,
    val createdAt: Long = System.currentTimeMillis()
)
