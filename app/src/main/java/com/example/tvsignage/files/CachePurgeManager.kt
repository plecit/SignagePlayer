package com.example.tvsignage.files

import com.example.tvsignage.core.SystemLogger
import com.example.tvsignage.xmds.models.RequiredFile
import java.io.File

class CachePurgeManager(private val storageManager: LocalStorageManager) {

    fun purgeExplicit(purgePaths: List<String>) {
        for (path in purgePaths) {
            val deleted = storageManager.deleteFile(path)
            if (deleted) {
                SystemLogger.i(TAG, "Purged obsolete file: $path")
            }
        }
    }

    fun purgeOrphanedFiles(activeFiles: List<RequiredFile>) {
        val activePathSet = activeFiles.map { it.path.lowercase() }.toSet()
        val mediaRoot = storageManager.mediaDir

        mediaRoot.walkTopDown().forEach { file ->
            if (file.isFile && !file.name.endsWith(".part")) {
                val relPath = file.relativeTo(mediaRoot).path.replace('\\', '/').lowercase()
                if (!activePathSet.contains(relPath)) {
                    val deleted = file.delete()
                    if (deleted) {
                        SystemLogger.i(TAG, "Purged unreferenced cache file: $relPath")
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CachePurgeManager"
    }
}
