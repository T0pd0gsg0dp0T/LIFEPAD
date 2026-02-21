package com.lifepad.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lifepad.app.data.local.dao.AccountDao
import com.lifepad.app.data.local.dao.AssessmentDao
import com.lifepad.app.data.local.dao.BacklinkDao
import com.lifepad.app.data.local.dao.BudgetDao
import com.lifepad.app.data.local.dao.CategoryDao
import com.lifepad.app.data.local.dao.EntryEmotionDao
import com.lifepad.app.data.local.dao.EntryThinkingTrapDao
import com.lifepad.app.data.local.dao.FolderDao
import com.lifepad.app.data.local.dao.HashtagDao
import com.lifepad.app.data.local.dao.JournalEntryDao
import com.lifepad.app.data.local.dao.NoteDao
import com.lifepad.app.data.local.dao.SearchDao
import com.lifepad.app.data.local.dao.ReminderDao
import com.lifepad.app.data.local.dao.AssetDao
import com.lifepad.app.data.local.dao.GoalDao
import com.lifepad.app.data.local.dao.NetWorthSnapshotDao
import com.lifepad.app.data.local.dao.RecurringBillDao
import com.lifepad.app.data.local.dao.TransactionDao
import com.lifepad.app.data.local.entity.AccountEntity
import com.lifepad.app.data.local.entity.AssetEntity
import com.lifepad.app.data.local.entity.AssessmentEntity
import com.lifepad.app.data.local.entity.BacklinkEntity
import com.lifepad.app.data.local.entity.BudgetEntity
import com.lifepad.app.data.local.entity.CategoryEntity
import com.lifepad.app.data.local.entity.EntryEmotionEntity
import com.lifepad.app.data.local.entity.EntryThinkingTrapEntity
import com.lifepad.app.data.local.entity.FolderEntity
import com.lifepad.app.data.local.entity.GoalEntity
import com.lifepad.app.data.local.entity.HashtagEntity
import com.lifepad.app.data.local.entity.HashtagUsageEntity
import com.lifepad.app.data.local.entity.JournalEntryEntity
import com.lifepad.app.data.local.entity.JournalEntryFts
import com.lifepad.app.data.local.entity.NoteEntity
import com.lifepad.app.data.local.entity.NetWorthSnapshotEntity
import com.lifepad.app.data.local.entity.NoteFts
import com.lifepad.app.data.local.entity.RecurringBillEntity
import com.lifepad.app.data.local.entity.ReminderEntity
import com.lifepad.app.data.local.entity.TransactionEntity

import com.lifepad.app.data.local.dao.AttachmentDao
import com.lifepad.app.data.local.entity.AttachmentEntity

//...

@Database(
    entities = [
        NoteEntity::class,
        NoteFts::class,
        BacklinkEntity::class,
        FolderEntity::class,
        JournalEntryEntity::class,
        JournalEntryFts::class,
        TransactionEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        HashtagEntity::class,
        HashtagUsageEntity::class,
        BudgetEntity::class,
        AssessmentEntity::class,
        ReminderEntity::class,
        EntryEmotionEntity::class,
        EntryThinkingTrapEntity::class,
        RecurringBillEntity::class,
        GoalEntity::class,
        AssetEntity::class,
        NetWorthSnapshotEntity::class,
        AttachmentEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LifepadDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun backlinkDao(): BacklinkDao
    abstract fun folderDao(): FolderDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun hashtagDao(): HashtagDao
    abstract fun searchDao(): SearchDao
    abstract fun budgetDao(): BudgetDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun reminderDao(): ReminderDao
    abstract fun entryEmotionDao(): EntryEmotionDao
    abstract fun entryThinkingTrapDao(): EntryThinkingTrapDao
    abstract fun recurringBillDao(): RecurringBillDao
    abstract fun goalDao(): GoalDao
    abstract fun assetDao(): AssetDao
    abstract fun netWorthSnapshotDao(): NetWorthSnapshotDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        const val DATABASE_NAME = "lifepad.db"
    }
}
