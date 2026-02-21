package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepad.app.data.local.entity.AssessmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assessment: AssessmentEntity): Long

    @Delete
    suspend fun delete(assessment: AssessmentEntity)

    @Query("SELECT * FROM assessments WHERE id = :id")
    suspend fun getById(id: Long): AssessmentEntity?

    @Query("SELECT * FROM assessments WHERE type = :type ORDER BY date DESC")
    fun getByType(type: String): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments ORDER BY date DESC")
    fun getAll(): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE type = :type ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(type: String): AssessmentEntity?

    @Query("SELECT * FROM assessments WHERE type = :type ORDER BY date ASC")
    suspend fun getTrend(type: String): List<AssessmentEntity>
}
