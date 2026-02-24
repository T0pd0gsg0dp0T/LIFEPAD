package com.lifepad.app.util

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lifepad.app.data.local.LifepadDatabase

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun createFullBackup(destination: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val dbFile = context.getDatabasePath(LifepadDatabase.DATABASE_NAME)
        val walFile = File(dbFile.parentFile, "${dbFile.name}-wal")
        val shmFile = File(dbFile.parentFile, "${dbFile.name}-shm")
        val filesDir = context.filesDir
        val sharedPrefsDir = File(context.filesDir.parentFile, "shared_prefs")

        try {
            resolver.openOutputStream(destination)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                    if (dbFile.exists()) {
                        addFile(zip, dbFile, "database/${dbFile.name}")
                    }
                    if (walFile.exists()) {
                        addFile(zip, walFile, "database/${walFile.name}")
                    }
                    if (shmFile.exists()) {
                        addFile(zip, shmFile, "database/${shmFile.name}")
                    }
                    if (filesDir.exists()) {
                        addDirectory(zip, filesDir, "files")
                    }
                    if (sharedPrefsDir.exists()) {
                        addDirectory(zip, sharedPrefsDir, "shared_prefs")
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to open backup destination."))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addDirectory(zip: ZipOutputStream, directory: File, basePath: String) {
        val children = directory.listFiles() ?: return
        for (child in children) {
            val entryName = "$basePath/${child.name}"
            if (child.isDirectory) {
                addDirectory(zip, child, entryName)
            } else {
                addFile(zip, child, entryName)
            }
        }
    }

    private fun addFile(zip: ZipOutputStream, file: File, entryName: String) {
        BufferedInputStream(file.inputStream()).use { input ->
            val entry = ZipEntry(entryName)
            entry.time = file.lastModified()
            zip.putNextEntry(entry)
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                zip.write(buffer, 0, count)
            }
            zip.closeEntry()
        }
    }
}
