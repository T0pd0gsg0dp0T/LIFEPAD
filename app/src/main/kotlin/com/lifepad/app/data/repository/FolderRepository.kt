package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.FolderDao
import com.lifepad.app.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FolderRepository @Inject constructor(
    private val folderDao: FolderDao
) {
    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()

    fun getRootFolders(): Flow<List<FolderEntity>> = folderDao.getRootFolders()

    fun getChildFolders(parentId: Long): Flow<List<FolderEntity>> = folderDao.getChildFolders(parentId)

    suspend fun getFolderById(id: Long): FolderEntity? = folderDao.getFolderById(id)

    suspend fun createFolder(name: String, parentId: Long? = null): Long {
        return folderDao.insert(
            FolderEntity(
                name = name.trim(),
                parentId = parentId
            )
        )
    }

    suspend fun renameFolder(id: Long, name: String) {
        val existing = folderDao.getFolderById(id) ?: return
        folderDao.update(existing.copy(name = name.trim()))
    }

    suspend fun moveFolder(id: Long, newParentId: Long?) {
        val existing = folderDao.getFolderById(id) ?: return
        folderDao.update(existing.copy(parentId = newParentId))
    }

    suspend fun deleteFolder(id: Long) {
        folderDao.deleteById(id)
    }
}
