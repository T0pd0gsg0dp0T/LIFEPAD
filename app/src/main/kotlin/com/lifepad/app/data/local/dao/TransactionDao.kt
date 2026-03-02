package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifepad.app.data.local.entity.TransactionEntity
import com.lifepad.app.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

data class SpendingByCategoryRow(
    val categoryId: Long?,
    val categoryName: String,
    val total: Double
)

data class MonthlyTotalRow(
    val yearMonth: String,
    val income: Double,
    val expense: Double
)

data class DailySpendingRow(
    val day: Long,
    val total: Double
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transactionDate BETWEEN :startDate AND :endDate ORDER BY transactionDate DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY transactionDate DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY transactionDate DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY transactionDate DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE linkedEntryId = :entryId")
    fun getTransactionsLinkedToEntry(entryId: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transactions SET accountId = NULL WHERE accountId = :accountId")
    suspend fun clearAccountForDeletedAccount(accountId: Long)

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0.0)
        FROM transactions
    """)
    fun getNetBalance(): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0.0)
        FROM transactions
        WHERE transactionDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getNetBalanceForPeriod(startDate: Long, endDate: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = :type AND transactionDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalByType(type: TransactionType, startDate: Long, endDate: Long): Double

    @Query("SELECT * FROM transactions WHERE description LIKE '%' || :query || '%' ORDER BY transactionDate DESC")
    suspend fun searchTransactions(query: String): List<TransactionEntity>

    @Query("""
        SELECT t.categoryId, COALESCE(c.name, 'Uncategorized') AS categoryName, SUM(t.amount) AS total
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.categoryId
        WHERE t.type = 'EXPENSE' AND t.transactionDate BETWEEN :startDate AND :endDate
        GROUP BY t.categoryId
        ORDER BY total DESC
    """)
    suspend fun getSpendingByCategory(startDate: Long, endDate: Long): List<SpendingByCategoryRow>

    @Query("""
        SELECT t.transactionDate / 86400000 * 86400000 AS day, SUM(t.amount) AS total
        FROM transactions t
        WHERE t.type = 'EXPENSE' AND t.transactionDate BETWEEN :startDate AND :endDate
        GROUP BY day
        ORDER BY day ASC
    """)
    suspend fun getDailySpending(startDate: Long, endDate: Long): List<DailySpendingRow>
}
