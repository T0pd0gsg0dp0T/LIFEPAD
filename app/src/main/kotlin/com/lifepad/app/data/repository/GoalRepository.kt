package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.GoalDao
import com.lifepad.app.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao
) {
    fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

    fun getActiveGoals(): Flow<List<GoalEntity>> = goalDao.getActiveGoals()

    fun getTotalMonthlyContributions(): Flow<Double> = goalDao.getTotalMonthlyContributions()

    suspend fun getGoalById(id: Long): GoalEntity? = goalDao.getGoalById(id)

    suspend fun saveGoal(goal: GoalEntity): Long {
        val updated = goal.copy(updatedAt = System.currentTimeMillis())
        return goalDao.insert(updated)
    }

    suspend fun updateProgress(goalId: Long, newCurrentAmount: Double) {
        val safeAmount = newCurrentAmount.coerceAtLeast(0.0)
        val goal = goalDao.getGoalById(goalId) ?: return
        val updated = goal.copy(
            currentAmount = safeAmount,
            isCompleted = safeAmount >= goal.targetAmount,
            updatedAt = System.currentTimeMillis()
        )
        goalDao.update(updated)
    }

    suspend fun deleteGoal(id: Long) = goalDao.deleteById(id)
}
