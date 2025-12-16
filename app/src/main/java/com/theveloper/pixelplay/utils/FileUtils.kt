package com.theveloper.pixelplay.utils

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object FileUtils {

    fun computeSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun getLastModified(file: File): Long = file.lastModified()
}