package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.AssessmentDao
import com.lifepad.app.data.local.entity.AssessmentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssessmentRepository @Inject constructor(
    private val assessmentDao: AssessmentDao
) {
    suspend fun save(assessment: AssessmentEntity): Long =
        assessmentDao.insert(assessment)

    suspend fun delete(assessment: AssessmentEntity) =
        assessmentDao.delete(assessment)

    suspend fun getById(id: Long): AssessmentEntity? =
        assessmentDao.getById(id)

    fun getByType(type: String): Flow<List<AssessmentEntity>> =
        assessmentDao.getByType(type)

    fun getAll(): Flow<List<AssessmentEntity>> =
        assessmentDao.getAll()

    suspend fun getLatest(type: String): AssessmentEntity? =
        assessmentDao.getLatest(type)

    suspend fun getTrend(type: String): List<AssessmentEntity> =
        assessmentDao.getTrend(type)
}
