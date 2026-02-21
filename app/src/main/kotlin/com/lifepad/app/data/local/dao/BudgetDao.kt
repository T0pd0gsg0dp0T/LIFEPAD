package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifepad.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

data class BudgetWithSpendingRaw(
    val id: Long,
    val categoryId: Long,
    val limitAmount: Double,
    val period: String,
    val categoryName: String,
    val spent: Double
)

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY createdAt DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getBudgetForCategory(categoryId: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getBudgetById(id: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT b.id, b.categoryId, b.limitAmount, b.period,
               c.name AS categoryName,
               COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) AS spent
        FROM budgets b
        INNER JOIN categories c ON c.id = b.categoryId
        LEFT JOIN transactions t ON t.categoryId = b.categoryId
            AND t.transactionDate >= :startDate
            AND t.transactionDate <= :endDate
        GROUP BY b.id
        ORDER BY c.name ASC
    """)
    fun getBudgetsWithSpending(startDate: Long, endDate: Long): Flow<List<BudgetWithSpendingRaw>>
}
