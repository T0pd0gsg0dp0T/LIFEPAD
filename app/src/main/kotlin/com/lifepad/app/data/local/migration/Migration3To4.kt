package com.lifepad.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_TO_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Fix reminders index name from v2→v3 migration (was "index_reminders_linked_item",
        // Room expects "index_reminders_linkedItemType_linkedItemId")
        db.execSQL("DROP INDEX IF EXISTS `index_reminders_linked_item`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_linkedItemType_linkedItemId` ON `reminders` (`linkedItemType`, `linkedItemId`)")

        // Add structuredData column to journal_entries
        db.execSQL("ALTER TABLE journal_entries ADD COLUMN structuredData TEXT NOT NULL DEFAULT ''")

        // Create entry_emotions table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `entry_emotions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `entryId` INTEGER NOT NULL,
                `emotionName` TEXT NOT NULL,
                `intensity` INTEGER NOT NULL,
                `phase` TEXT NOT NULL,
                FOREIGN KEY (`entryId`) REFERENCES `journal_entries`(`id`) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_emotions_entryId` ON `entry_emotions` (`entryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_emotions_emotionName` ON `entry_emotions` (`emotionName`)")

        // Create entry_thinking_traps table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `entry_thinking_traps` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `entryId` INTEGER NOT NULL,
                `trapType` TEXT NOT NULL,
                FOREIGN KEY (`entryId`) REFERENCES `journal_entries`(`id`) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_thinking_traps_entryId` ON `entry_thinking_traps` (`entryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_thinking_traps_trapType` ON `entry_thinking_traps` (`trapType`)")
    }
}
