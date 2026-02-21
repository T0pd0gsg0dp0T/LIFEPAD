package com.lifepad.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_TO_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create recurring_bills table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recurring_bills` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `transactionType` TEXT NOT NULL DEFAULT 'EXPENSE',
                `categoryId` INTEGER,
                `accountId` INTEGER,
                `frequency` TEXT NOT NULL DEFAULT 'MONTHLY',
                `nextDueDate` INTEGER NOT NULL,
                `dayOfMonth` INTEGER,
                `isConfirmed` INTEGER NOT NULL DEFAULT 0,
                `isEnabled` INTEGER NOT NULL DEFAULT 1,
                `reminderId` INTEGER,
                `detectedFromCount` INTEGER NOT NULL DEFAULT 0,
                `notes` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY (`categoryId`) REFERENCES `categories`(`id`) ON DELETE SET NULL,
                FOREIGN KEY (`accountId`) REFERENCES `accounts`(`id`) ON DELETE SET NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_bills_categoryId` ON `recurring_bills` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_bills_accountId` ON `recurring_bills` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_bills_nextDueDate` ON `recurring_bills` (`nextDueDate`)")

        // Create goals table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `goals` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL DEFAULT 'SAVINGS',
                `targetAmount` REAL NOT NULL,
                `currentAmount` REAL NOT NULL DEFAULT 0.0,
                `monthlyContribution` REAL NOT NULL DEFAULT 0.0,
                `deadline` INTEGER,
                `accountId` INTEGER,
                `notes` TEXT NOT NULL DEFAULT '',
                `isCompleted` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY (`accountId`) REFERENCES `accounts`(`id`) ON DELETE SET NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_accountId` ON `goals` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_type` ON `goals` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_deadline` ON `goals` (`deadline`)")

        // Create assets table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `assets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `value` REAL NOT NULL,
                `assetType` TEXT NOT NULL DEFAULT 'OTHER',
                `isLiability` INTEGER NOT NULL DEFAULT 0,
                `notes` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_assetType` ON `assets` (`assetType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_isLiability` ON `assets` (`isLiability`)")

        // Create net_worth_snapshots table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `net_worth_snapshots` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `snapshotDate` INTEGER NOT NULL,
                `accountsTotal` REAL NOT NULL,
                `assetsTotal` REAL NOT NULL,
                `liabilitiesTotal` REAL NOT NULL,
                `netWorth` REAL NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_net_worth_snapshots_snapshotDate` ON `net_worth_snapshots` (`snapshotDate`)")
    }
}
