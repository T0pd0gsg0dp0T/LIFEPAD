package com.lifepad.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class GoalType {
    SAVINGS, DEBT_PAYOFF, EMERGENCY_FUND
}

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("accountId"),
        Index("type"),
        Index("deadline")
    ]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: GoalType = GoalType.SAVINGS,
    val targetAmount: Double,
    @ColumnInfo(defaultValue = "0.0")
    val currentAmount: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    val monthlyContribution: Double = 0.0,
    val deadline: Long? = null,
    val accountId: Long? = null,
    @ColumnInfo(defaultValue = "")
    val notes: String = "",
    @ColumnInfo(defaultValue = "0")
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
