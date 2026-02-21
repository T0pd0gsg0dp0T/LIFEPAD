package com.lifepad.app.util

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun saveFile(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val extension = getExtensionFromUri(uri)
            val fileName = "${UUID.randomUUID()}.$extension"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteFile(filePath: String): Boolean {
        return try {
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
