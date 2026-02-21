package com.lifepad.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_TO_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `attachments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `itemId` INTEGER NOT NULL,
                `itemType` TEXT NOT NULL,
                `filePath` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_itemId_itemType` ON `attachments` (`itemId`, `itemType`)")
    }
}
