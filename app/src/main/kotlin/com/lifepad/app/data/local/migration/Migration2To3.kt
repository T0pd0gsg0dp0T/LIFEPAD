package com.lifepad.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_TO_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new finance categories
        val now = System.currentTimeMillis()
        db.execSQL(
            "INSERT OR IGNORE INTO categories (name, icon, isDefault, createdAt) VALUES (?, ?, ?, ?)",
            arrayOf<Any>("Rent", "home", 1, now)
        )
        db.execSQL(
            "INSERT OR IGNORE INTO categories (name, icon, isDefault, createdAt) VALUES (?, ?, ?, ?)",
            arrayOf<Any>("Phone", "phone_android", 1, now)
        )
        db.execSQL(
            "INSERT OR IGNORE INTO categories (name, icon, isDefault, createdAt) VALUES (?, ?, ?, ?)",
            arrayOf<Any>("WiFi", "wifi", 1, now)
        )
        db.execSQL(
            "INSERT OR IGNORE INTO categories (name, icon, isDefault, createdAt) VALUES (?, ?, ?, ?)",
            arrayOf<Any>("AI", "smart_toy", 1, now)
        )

        // Add isChecklist column to notes
        db.execSQL("ALTER TABLE notes ADD COLUMN isChecklist INTEGER NOT NULL DEFAULT 0")

        // Create assessments table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `assessments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `type` TEXT NOT NULL,
                `score` INTEGER NOT NULL,
                `answers` TEXT NOT NULL,
                `date` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_assessments_type` ON `assessments` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_assessments_date` ON `assessments` (`date`)")

        // Create reminders table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `reminders` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `message` TEXT NOT NULL DEFAULT '',
                `triggerTime` INTEGER NOT NULL,
                `repeatInterval` INTEGER NOT NULL DEFAULT 0,
                `isEnabled` INTEGER NOT NULL DEFAULT 1,
                `linkedItemType` TEXT NOT NULL,
                `linkedItemId` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_triggerTime` ON `reminders` (`triggerTime`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_linked_item` ON `reminders` (`linkedItemType`, `linkedItemId`)")
    }
}
