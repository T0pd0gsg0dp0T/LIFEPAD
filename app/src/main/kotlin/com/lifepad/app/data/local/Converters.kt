package com.lifepad.app.data.local

import androidx.room.TypeConverter
import com.lifepad.app.data.local.entity.ItemType
import com.lifepad.app.data.local.entity.TransactionType

class Converters {
    @TypeConverter
    fun fromItemType(value: ItemType): String = value.name

    @TypeConverter
    fun toItemType(value: String): ItemType = ItemType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
