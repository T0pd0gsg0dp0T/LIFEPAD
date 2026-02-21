package com.lifepad.app.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifepad.app.data.local.LifepadDatabase
import com.lifepad.app.security.SecurityManager
import net.sqlcipher.database.SupportFactory
import com.lifepad.app.data.local.dao.AccountDao
import com.lifepad.app.data.local.dao.BacklinkDao
import com.lifepad.app.data.local.dao.BudgetDao
import com.lifepad.app.data.local.dao.CategoryDao
import com.lifepad.app.data.local.dao.FolderDao
import com.lifepad.app.data.local.dao.HashtagDao
import com.lifepad.app.data.local.dao.JournalEntryDao
import com.lifepad.app.data.local.dao.AssessmentDao
import com.lifepad.app.data.local.dao.EntryEmotionDao
import com.lifepad.app.data.local.dao.AssetDao
import com.lifepad.app.data.local.dao.EntryThinkingTrapDao
import com.lifepad.app.data.local.dao.GoalDao
import com.lifepad.app.data.local.dao.AttachmentDao
import com.lifepad.app.data.local.dao.NetWorthSnapshotDao
import com.lifepad.app.data.local.dao.NoteDao
import com.lifepad.app.data.local.dao.RecurringBillDao
import com.lifepad.app.data.local.dao.ReminderDao
import com.lifepad.app.data.local.dao.SearchDao
import com.lifepad.app.data.local.dao.TransactionDao
import com.lifepad.app.data.local.migration.MIGRATION_1_TO_2
import com.lifepad.app.data.local.migration.MIGRATION_2_TO_3
import com.lifepad.app.data.local.migration.MIGRATION_3_TO_4
import com.lifepad.app.data.local.migration.MIGRATION_4_TO_5
import com.lifepad.app.data.local.migration.MIGRATION_5_TO_6
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val TAG = "DatabaseModule"

    @Provides
    @Singleton
    fun provideSecurityManager(@ApplicationContext context: Context): SecurityManager {
        return SecurityManager(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): LifepadDatabase {
        // Migrate existing unencrypted DB to SQLCipher if needed
        val isEncrypted = securityManager.migrateDatabaseToEncrypted(LifepadDatabase.DATABASE_NAME)

        fun baseBuilder() = Room.databaseBuilder(
            context,
            LifepadDatabase::class.java,
            LifepadDatabase.DATABASE_NAME
        )

        val builder = baseBuilder()

        if (isEncrypted) {
            val passphrase = securityManager.getDbPassphrase()
            val factory = SupportFactory(passphrase)
            builder.openHelperFactory(factory)
        }

        val database = builder
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Prepopulate default categories using raw SQL
                    val now = System.currentTimeMillis()
                    val categories = listOf(
                        Triple("Food", "restaurant", true),
                        Triple("Transport", "directions_car", true),
                        Triple("Utilities", "bolt", true),
                        Triple("Entertainment", "movie", true),
                        Triple("Healthcare", "medical_services", true),
                        Triple("Shopping", "shopping_bag", true),
                        Triple("Work", "work", true),
                        Triple("Savings", "savings", true),
                        Triple("Subscriptions", "subscriptions", true),
                        Triple("Other", "more_horiz", true),
                        Triple("Rent", "home", true),
                        Triple("Phone", "phone_android", true),
                        Triple("WiFi", "wifi", true),
                        Triple("AI", "smart_toy", true)
                    )
                    categories.forEach { (name, icon, isDefault) ->
                        db.execSQL(
                            "INSERT INTO categories (name, icon, isDefault, createdAt) VALUES (?, ?, ?, ?)",
                            arrayOf<Any>(name, icon, if (isDefault) 1 else 0, now)
                        )
                    }

                    // Insert default account
                    db.execSQL(
                        "INSERT INTO accounts (name, balance, isDefault, createdAt) VALUES (?, ?, ?, ?)",
                        arrayOf<Any>("Cash", 0.0, 1, now)
                    )
                }
            })
            .addMigrations(MIGRATION_1_TO_2, MIGRATION_2_TO_3, MIGRATION_3_TO_4, MIGRATION_4_TO_5, MIGRATION_5_TO_6)
            .build()

        // Verify DB can actually be opened. If SQLCipher key/data is stale, recreate DB.
        return try {
            database.openHelper.writableDatabase
            database
        } catch (e: Exception) {
            Log.e(TAG, "Database open failed, recreating local DB", e)
            database.close()
            context.deleteDatabase(LifepadDatabase.DATABASE_NAME)
            context.getDatabasePath("${LifepadDatabase.DATABASE_NAME}-wal").delete()
            context.getDatabasePath("${LifepadDatabase.DATABASE_NAME}-shm").delete()
            context.getDatabasePath("${LifepadDatabase.DATABASE_NAME}.backup").delete()
            context.getDatabasePath("${LifepadDatabase.DATABASE_NAME}.tmp").delete()

            val recoveredBuilder = baseBuilder()
            if (isEncrypted) {
                val passphrase = securityManager.getDbPassphrase()
                recoveredBuilder.openHelperFactory(SupportFactory(passphrase))
            }
            recoveredBuilder
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        val now = System.currentTimeMillis()
                        val categories = listOf(
                            Triple("Food", "restaurant", true),
                            Triple("Transport", "directions_car", true),
                            Triple("Utilities", "bolt", true),
                            Triple("Entertainment", "movie", true),
                            Triple("Healthcare", "medical_services", true),
                            Triple("Shopping", "shopping_bag", true),
                            Triple("Work", "work", true),
                            Triple("Savings", "savings", true),
                            Triple("Subscriptions", "subscriptions", true),
                            Triple("Other", "more_horiz", true),
                            Triple("Rent", "home", true),
                            Triple("Phone", "phone_android", true),
                            Triple("WiFi", "wifi", true),
                            Triple("AI", "smart_toy", true)
                        )
                        categories.forEach { (name, icon, isDefaultCategory) ->
                            db.execSQL(
                                "INSERT INTO categories (name, icon, isDefault, createdAt) VALUES (?, ?, ?, ?)",
                                arrayOf<Any>(name, icon, if (isDefaultCategory) 1 else 0, now)
                            )
                        }
                        db.execSQL(
                            "INSERT INTO accounts (name, balance, isDefault, createdAt) VALUES (?, ?, ?, ?)",
                            arrayOf<Any>("Cash", 0.0, 1, now)
                        )
                    }
                })
                .addMigrations(MIGRATION_1_TO_2, MIGRATION_2_TO_3, MIGRATION_3_TO_4, MIGRATION_4_TO_5, MIGRATION_5_TO_6)
                .build()
        }
    }

    @Provides
    fun provideNoteDao(database: LifepadDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideBacklinkDao(database: LifepadDatabase): BacklinkDao = database.backlinkDao()

    @Provides
    fun provideFolderDao(database: LifepadDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideJournalEntryDao(database: LifepadDatabase): JournalEntryDao = database.journalEntryDao()

    @Provides
    fun provideTransactionDao(database: LifepadDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: LifepadDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideAccountDao(database: LifepadDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideHashtagDao(database: LifepadDatabase): HashtagDao = database.hashtagDao()

    @Provides
    fun provideSearchDao(database: LifepadDatabase): SearchDao = database.searchDao()

    @Provides
    fun provideBudgetDao(database: LifepadDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideAssessmentDao(database: LifepadDatabase): AssessmentDao = database.assessmentDao()

    @Provides
    fun provideReminderDao(database: LifepadDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideEntryEmotionDao(database: LifepadDatabase): EntryEmotionDao = database.entryEmotionDao()

    @Provides
    fun provideEntryThinkingTrapDao(database: LifepadDatabase): EntryThinkingTrapDao = database.entryThinkingTrapDao()

    @Provides
    fun provideRecurringBillDao(database: LifepadDatabase): RecurringBillDao = database.recurringBillDao()

    @Provides
    fun provideGoalDao(database: LifepadDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideAssetDao(database: LifepadDatabase): AssetDao = database.assetDao()

    @Provides
    fun provideNetWorthSnapshotDao(database: LifepadDatabase): NetWorthSnapshotDao = database.netWorthSnapshotDao()

    @Provides
    fun provideAttachmentDao(database: LifepadDatabase): AttachmentDao = database.attachmentDao()
}
