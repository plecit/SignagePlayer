package com.example.tvsignage.files

import android.util.Base64
import com.example.tvsignage.core.SystemLogger
import com.example.tvsignage.xmds.XmdsClient
import com.example.tvsignage.xmds.models.MediaInventoryItem
import com.example.tvsignage.xmds.models.RequiredFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class DownloadManager(
    private val xmdsClient: XmdsClient,
    private val storageManager: LocalStorageManager
) {

    private val chunkSize = 512 * 1024L // 512 KB chunks

    suspend fun syncFiles(
        hardwareKey: String,
        requiredFiles: List<RequiredFile>,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): List<MediaInventoryItem> = withContext(Dispatchers.IO) {
        val inventory = mutableListOf<MediaInventoryItem>()
        var completedCount = 0
        val totalCount = requiredFiles.size

        for (rf in requiredFiles) {
            val isAlreadyComplete = storageManager.isFileComplete(rf.path, rf.md5)
            if (isAlreadyComplete) {
                val file = storageManager.getFile(rf.path)
                inventory.add(
                    MediaInventoryItem(
                        fileId = rf.id,
                        type = rf.type,
                        complete = true,
                        bytesDownloaded = file.length(),
                        totalBytes = rf.size,
                        md5Valid = true
                    )
                )
                completedCount++
                onProgress?.invoke(completedCount, totalCount)
                continue
            }

            // Download file in chunks
            val success = downloadSingleFile(hardwareKey, rf)
            val file = storageManager.getFile(rf.path)
            val valid = storageManager.isFileComplete(rf.path, rf.md5)

            inventory.add(
                MediaInventoryItem(
                    fileId = rf.id,
                    type = rf.type,
                    complete = success && valid,
                    bytesDownloaded = if (file.exists()) file.length() else 0L,
                    totalBytes = rf.size,
                    md5Valid = valid
                )
            )

            if (success && valid) {
                completedCount++
            }
            onProgress?.invoke(completedCount, totalCount)
        }

        return@withContext inventory
    }

    private suspend fun downloadSingleFile(hardwareKey: String, fileInfo: RequiredFile): Boolean {
        val partFile = storageManager.getPartFile(fileInfo.path)
        partFile.parentFile?.mkdirs()

        var currentOffset = if (partFile.exists()) partFile.length() else 0L
        val targetSize = fileInfo.size

        SystemLogger.i(TAG, "Starting download for ${fileInfo.path} (id=${fileInfo.id}, offset=$currentOffset, targetSize=$targetSize)")

        try {
            while (currentOffset < targetSize || targetSize == 0L) {
                val base64Chunk = xmdsClient.getFileChunk(
                    hardwareKey = hardwareKey,
                    fileId = fileInfo.id,
                    fileType = fileInfo.type,
                    offset = currentOffset,
                    chunkSize = chunkSize
                )

                if (base64Chunk.isBlank()) {
                    // Reached end of file
                    break
                }

                val bytes = Base64.decode(base64Chunk, Base64.DEFAULT)
                if (bytes.isEmpty()) {
                    break
                }

                FileOutputStream(partFile, true).use { fos ->
                    fos.write(bytes)
                }

                currentOffset += bytes.size
                if (targetSize > 0 && currentOffset >= targetSize) {
                    break
                }
            }

            // Verify MD5 before committing
            if (fileInfo.md5.isNotBlank()) {
                val matches = Md5Checksum.verify(partFile, fileInfo.md5)
                if (!matches) {
                    SystemLogger.e(TAG, "MD5 mismatch for ${fileInfo.path}! Expected ${fileInfo.md5}, deleting corrupt part file.")
                    partFile.delete()
                    return false
                }
            }

            val committed = storageManager.commitPartFile(fileInfo.path)
            if (committed) {
                SystemLogger.i(TAG, "Successfully downloaded and verified ${fileInfo.path}")
                return true
            }
        } catch (e: Exception) {
            SystemLogger.e(TAG, "Exception downloading ${fileInfo.path}: ${e.message}", e)
        }

        return false
    }

    companion object {
        private const val TAG = "DownloadManager"
    }
}
