package com.example.tvsignage.files

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object Md5Checksum {

    fun calculateMd5(file: File): String {
        if (!file.exists() || !file.isFile) return ""

        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192)

        FileInputStream(file).use { fis ->
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }

        val md5Bytes = digest.digest()
        val sb = StringBuilder()
        for (b in md5Bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    fun verify(file: File, expectedMd5: String): Boolean {
        if (expectedMd5.isBlank()) return true // No hash provided by CMS
        val actual = calculateMd5(file)
        return actual.equals(expectedMd5.trim(), ignoreCase = true)
    }
}
