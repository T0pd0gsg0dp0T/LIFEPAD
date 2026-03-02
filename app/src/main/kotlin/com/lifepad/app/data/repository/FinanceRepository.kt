package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.AccountDao
import com.lifepad.app.data.local.dao.BudgetDao
import com.lifepad.app.data.local.dao.BudgetWithSpendingRaw
import com.lifepad.app.data.local.dao.CategoryDao
import com.lifepad.app.data.local.dao.DailySpendingRow
import com.lifepad.app.data.local.dao.HashtagDao
import com.lifepad.app.data.local.dao.SpendingByCategoryRow
import com.lifepad.app.data.local.dao.TransactionDao
import com.lifepad.app.data.local.entity.AccountEntity
import com.lifepad.app.data.local.entity.BudgetEntity
import com.lifepad.app.data.local.entity.CategoryEntity
import com.lifepad.app.data.local.entity.CategoryType
import com.lifepad.app.data.local.entity.ItemType
import com.lifepad.app.data.local.entity.HashtagUsageName
import com.lifepad.app.data.local.entity.TransactionEntity
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.domain.parser.HashtagParser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val hashtagDao: HashtagDao,
    private val budgetDao: BudgetDao
) {
    // Transactions
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByCategory(categoryId)

    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByAccount(accountId)

    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByType(type)

    fun observeTransaction(transactionId: Long): Flow<TransactionEntity?> =
        transactionDao.observeTransactionById(transactionId)

    fun getTransactionsLinkedToEntry(entryId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsLinkedToEntry(entryId)

    suspend fun getTransactionById(transactionId: Long): TransactionEntity? =
        transactionDao.getTransactionById(transactionId)

    suspend fun searchTransactions(query: String): List<TransactionEntity> =
        transactionDao.searchTransactions(query)

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        val updatedTransaction = transaction.copy(updatedAt = System.currentTimeMillis())
        val transactionId = transactionDao.insert(updatedTransaction)
        val savedTransactionId = if (transaction.id == 0L) transactionId else transaction.id

        // Sync hashtags from description
        val hashtags = HashtagParser.extractHashtags(transaction.description)
        hashtagDao.syncHashtagsForItem(ItemType.TRANSACTION, savedTransactionId, hashtags)

        return savedTransactionId
    }

    suspend fun deleteTransaction(transactionId: Long) {
        transactionDao.deleteById(transactionId)
    }

    fun getNetBalance(): Flow<Double> = transactionDao.getNetBalance()

    suspend fun getNetBalanceForPeriod(startDate: Long, endDate: Long): Double =
        transactionDao.getNetBalanceForPeriod(startDate, endDate)

    suspend fun getTotalByType(type: TransactionType, startDate: Long, endDate: Long): Double =
        transactionDao.getTotalByType(type, startDate, endDate)

    fun getHashtagsForTransaction(transactionId: Long) =
        hashtagDao.observeHashtagsForItem(ItemType.TRANSACTION, transactionId)

    // Categories
    fun getAllCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun getAllCategoriesSnapshot(): List<CategoryEntity> = categoryDao.getAllCategoriesOnce()

    fun getActiveCategories(): Flow<List<CategoryEntity>> = categoryDao.getActiveCategories()

    fun getCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    fun getArchivedCategories(): Flow<List<CategoryEntity>> = categoryDao.getArchivedCategories()

    fun getDefaultCategories(): Flow<List<CategoryEntity>> = categoryDao.getDefaultCategories()

    suspend fun getCategoryById(categoryId: Long): CategoryEntity? = categoryDao.getCategoryById(categoryId)

    suspend fun saveCategory(category: CategoryEntity): Long {
        return if (category.id == 0L) {
            categoryDao.insert(category)
        } else {
            categoryDao.update(category)
            category.id
        }
    }

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)

    suspend fun updateCategories(categories: List<CategoryEntity>) = categoryDao.updateAll(categories)

    suspend fun archiveCategory(categoryId: Long) = categoryDao.archive(categoryId)

    suspend fun unarchiveCategory(categoryId: Long) = categoryDao.unarchive(categoryId)

    fun observeHashtagNamesForTransactions(): Flow<List<HashtagUsageName>> =
        hashtagDao.observeHashtagNamesForItemType(ItemType.TRANSACTION)

    suspend fun deleteCategory(categoryId: Long) = categoryDao.deleteById(categoryId)

    // Accounts
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    fun getTotalBalance(): Flow<Double?> = accountDao.getTotalBalance()

    suspend fun getDefaultAccount(): AccountEntity? = accountDao.getDefaultAccount()

    suspend fun getAccountById(accountId: Long): AccountEntity? = accountDao.getAccountById(accountId)

    suspend fun saveAccount(account: AccountEntity): Long = accountDao.insert(account)

    suspend fun deleteAccount(accountId: Long) {
        val account = accountDao.getAccountById(accountId) ?: return
        transactionDao.clearAccountForDeletedAccount(accountId)
        accountDao.delete(account)
    }

    suspend fun updateAccountBalance(accountId: Long, amount: Double) =
        accountDao.updateBalance(accountId, amount)

    // Budgets
    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    suspend fun getBudgetById(id: Long): BudgetEntity? = budgetDao.getBudgetById(id)

    suspend fun getBudgetForCategory(categoryId: Long): BudgetEntity? =
        budgetDao.getBudgetForCategory(categoryId)

    suspend fun saveBudget(budget: BudgetEntity): Long {
        val updated = budget.copy(updatedAt = System.currentTimeMillis())
        return budgetDao.insert(updated)
    }

    suspend fun deleteBudget(budgetId: Long) = budgetDao.deleteById(budgetId)

    fun getBudgetsWithSpending(startDate: Long, endDate: Long): Flow<List<BudgetWithSpendingRaw>> =
        budgetDao.getBudgetsWithSpending(startDate, endDate)

    // Stats
    suspend fun getSpendingByCategory(startDate: Long, endDate: Long): List<SpendingByCategoryRow> =
        transactionDao.getSpendingByCategory(startDate, endDate)

    suspend fun getDailySpending(startDate: Long, endDate: Long): List<DailySpendingRow> =
        transactionDao.getDailySpending(startDate, endDate)
}
