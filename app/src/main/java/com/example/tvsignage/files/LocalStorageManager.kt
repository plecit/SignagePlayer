package com.example.tvsignage.files

import android.content.Context
import com.example.tvsignage.core.SystemLogger
import java.io.File

class LocalStorageManager(private val context: Context) {

    val mediaDir: File by lazy {
        val dir = File(context.filesDir, "media")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    fun getFile(relativePath: String): File {
        return File(mediaDir, relativePath)
    }

    fun getPartFile(relativePath: String): File {
        return File(mediaDir, "$relativePath.part")
    }

    fun isFileComplete(relativePath: String, expectedMd5: String): Boolean {
        val file = getFile(relativePath)
        if (!file.exists() || file.length() == 0L) return false
        if (expectedMd5.isBlank()) return true
        return Md5Checksum.verify(file, expectedMd5)
    }

    fun commitPartFile(relativePath: String): Boolean {
        val partFile = getPartFile(relativePath)
        val finalFile = getFile(relativePath)

        if (!partFile.exists()) return false

        // Ensure parent directory exists
        finalFile.parentFile?.mkdirs()

        if (finalFile.exists()) {
            finalFile.delete()
        }

        val renamed = partFile.renameTo(finalFile)
        if (!renamed) {
            SystemLogger.w(TAG, "Failed atomic rename for $relativePath, performing copy and delete")
            try {
                partFile.copyTo(finalFile, overwrite = true)
                partFile.delete()
                return true
            } catch (e: Exception) {
                SystemLogger.e(TAG, "Failed fallback copy for $relativePath", e)
                return false
            }
        }
        return true
    }

    fun deleteFile(relativePath: String): Boolean {
        val file = getFile(relativePath)
        val part = getPartFile(relativePath)
        var deleted = false
        if (file.exists()) deleted = file.delete()
        if (part.exists()) deleted = part.delete() || deleted
        return deleted
    }

    companion object {
        private const val TAG = "LocalStorageManager"
    }
}
