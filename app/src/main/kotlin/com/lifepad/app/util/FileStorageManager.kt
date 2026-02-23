package com.lifepad.app.util

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun saveFile(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val extension = getExtensionFromUri(uri)
                val fileName = if (extension.isNullOrBlank()) {
                    UUID.randomUUID().toString()
                } else {
                    "${UUID.randomUUID()}.$extension"
                }
                val file = File(context.filesDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                file.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getExtensionFromUri(uri: Uri): String? {
        return context.contentResolver.getType(uri)?.let { mimeType ->
            android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        }
    }
}
