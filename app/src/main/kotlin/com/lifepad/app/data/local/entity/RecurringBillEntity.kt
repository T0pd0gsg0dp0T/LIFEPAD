package com.lifepad.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BillFrequency {
    WEEKLY, BIWEEKLY, MONTHLY, YEARLY
}

@Entity(
    tableName = "recurring_bills",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index("accountId"),
        Index("nextDueDate")
    ]
)
data class RecurringBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val frequency: BillFrequency = BillFrequency.MONTHLY,
    val nextDueDate: Long,
    val dayOfMonth: Int? = null,
    @ColumnInfo(defaultValue = "0")
    val isConfirmed: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    val isEnabled: Boolean = true,
    val reminderId: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val detectedFromCount: Int = 0,
    @ColumnInfo(defaultValue = "")
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
