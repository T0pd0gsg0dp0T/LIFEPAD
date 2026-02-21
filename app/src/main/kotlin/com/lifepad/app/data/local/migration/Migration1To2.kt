package com.lifepad.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_TO_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `budgets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `limitAmount` REAL NOT NULL,
                `period` TEXT NOT NULL DEFAULT 'monthly',
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)")
    }
}
