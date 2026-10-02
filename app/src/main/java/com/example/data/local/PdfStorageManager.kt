package com.example.data.local

import android.content.Context
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale

class PdfStorageManager(private val context: Context) {

    private val baseSchedulesDir: File
        get() {
            val dir = File(context.filesDir, "schedules")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun getTermDirectory(term: String): File {
        val safeTerm = term.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifEmpty { "current" }
        val termDir = File(baseSchedulesDir, safeTerm)
        if (!termDir.exists()) termDir.mkdirs()
        return termDir
    }

    fun savePdf(inputStream: InputStream, term: String, fileName: String): File {
        val termDir = getTermDirectory(term)
        val safeName = fileName.replace(Regex("[/\\\\?%*:|\"<>]"), "_").ifEmpty { "schedule.pdf" }
        val finalFile = File(termDir, safeName)

        finalFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return finalFile
    }

    fun savePdf(bytes: ByteArray, term: String, fileName: String): File {
        val termDir = getTermDirectory(term)
        val safeName = fileName.replace(Regex("[/\\\\?%*:|\"<>]"), "_").ifEmpty { "schedule.pdf" }
        val finalFile = File(termDir, safeName)
        finalFile.writeBytes(bytes)
        return finalFile
    }

    fun computeSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun computeSha256(bytes: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(bytes)
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun getDownloadedFilesCount(): Int {
        return try {
            baseSchedulesDir.walkTopDown().filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }.count()
        } catch (e: Exception) {
            0
        }
    }

    fun getTotalStorageSizeBytes(): Long {
        return try {
            baseSchedulesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (e: Exception) {
            0L
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "۰ بایت"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f مگابایت", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f کیلوبایت", kb)
            else -> "$bytes بایت"
        }
    }

    fun deleteAllPdfs(): Boolean {
        return try {
            baseSchedulesDir.deleteRecursively()
            baseSchedulesDir.mkdirs()
            true
        } catch (e: Exception) {
            false
        }
    }
}
