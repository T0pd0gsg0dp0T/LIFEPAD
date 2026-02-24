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
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.zip.ZipInputStream

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LifepadDatabase
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

    suspend fun restoreFullBackup(source: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val tempDir = File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply { mkdirs() }

        try {
            resolver.openInputStream(source)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val entryName = entry.name
                        if (entryName.startsWith("/") || entryName.contains("..")) {
                            zip.closeEntry()
                            continue
                        }
                        val target = File(tempDir, entryName)
                        if (!target.canonicalPath.startsWith(tempDir.canonicalPath)) {
                            zip.closeEntry()
                            continue
                        }
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            BufferedOutputStream(target.outputStream()).use { output ->
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count <= 0) break
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        zip.closeEntry()
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to open backup file."))

            val dbFile = context.getDatabasePath(LifepadDatabase.DATABASE_NAME)
            val walFile = File(dbFile.parentFile, "${dbFile.name}-wal")
            val shmFile = File(dbFile.parentFile, "${dbFile.name}-shm")
            val extractedDb = File(tempDir, "database/${dbFile.name}")
            if (!extractedDb.exists()) {
                return@withContext Result.failure(IllegalStateException("Backup is missing the database file."))
            }

            database.close()

            dbFile.parentFile?.mkdirs()
            extractedDb.copyTo(dbFile, overwrite = true)
            val extractedWal = File(tempDir, "database/${walFile.name}")
            val extractedShm = File(tempDir, "database/${shmFile.name}")
            if (extractedWal.exists()) {
                extractedWal.copyTo(walFile, overwrite = true)
            } else {
                walFile.delete()
            }
            if (extractedShm.exists()) {
                extractedShm.copyTo(shmFile, overwrite = true)
            } else {
                shmFile.delete()
            }

            val extractedFiles = File(tempDir, "files")
            replaceDirectoryContents(context.filesDir, extractedFiles)

            val extractedPrefs = File(tempDir, "shared_prefs")
            val prefsDir = File(context.filesDir.parentFile, "shared_prefs")
            replaceDirectoryContents(prefsDir, extractedPrefs)

            tempDir.deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            tempDir.deleteRecursively()
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

    private fun replaceDirectoryContents(targetDir: File, sourceDir: File) {
        if (targetDir.exists()) {
            targetDir.listFiles()?.forEach { it.deleteRecursively() }
        } else {
            targetDir.mkdirs()
        }
        if (sourceDir.exists()) {
            copyDirectory(sourceDir, targetDir)
        }
    }

    private fun copyDirectory(source: File, target: File) {
        source.listFiles()?.forEach { child ->
            val dest = File(target, child.name)
            if (child.isDirectory) {
                dest.mkdirs()
                copyDirectory(child, dest)
            } else {
                child.copyTo(dest, overwrite = true)
            }
        }
    }
}
