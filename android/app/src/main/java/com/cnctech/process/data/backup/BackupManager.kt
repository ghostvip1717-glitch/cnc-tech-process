package com.cnctech.process.data.backup

import android.content.Context
import android.net.Uri
import com.cnctech.process.data.db.AppDatabase
import com.cnctech.process.data.photo.PhotoStorage
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup format: ZIP containing database/cnc_tech_process.db and photos/ tree
 * (mirrors filesDir/photos).
 */
class BackupManager(
    private val context: Context,
    private val photoStorage: PhotoStorage,
) {
    fun suggestedFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return "cnc-tech-process-backup-$stamp.zip"
    }

    fun exportToUri(destUri: Uri) {
        AppDatabase.getInstance(context).close()
        try {
            val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
            val wal = File(dbFile.path + "-wal")
            val shm = File(dbFile.path + "-shm")
            // Checkpoint by closing; copy main db (+ wal/shm if present)
            context.contentResolver.openOutputStream(destUri)?.use { rawOut ->
                ZipOutputStream(BufferedOutputStream(rawOut)).use { zip ->
                    if (dbFile.exists()) {
                        putFile(zip, "database/${AppDatabase.DB_NAME}", dbFile)
                    }
                    if (wal.exists()) putFile(zip, "database/${AppDatabase.DB_NAME}-wal", wal)
                    if (shm.exists()) putFile(zip, "database/${AppDatabase.DB_NAME}-shm", shm)
                    val photosRoot = photoStorage.photosRootDir()
                    if (photosRoot.exists()) {
                        photosRoot.walkTopDown().filter { it.isFile }.forEach { file ->
                            val relative = file.relativeTo(photosRoot).invariantSeparatorsPath
                            putFile(zip, "photos/$relative", file)
                        }
                    }
                }
            } ?: error("Не удалось открыть файл для записи")
        } finally {
            AppDatabase.reopen(context)
        }
    }

    fun importFromUri(sourceUri: Uri) {
        val staging = File(context.cacheDir, "backup_import_${System.currentTimeMillis()}").also {
            it.mkdirs()
        }
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { rawIn ->
                ZipInputStream(BufferedInputStream(rawIn)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name.replace('\\', '/')
                        if (!entry.isDirectory && isSafeZipPath(name)) {
                            val outFile = File(staging, name)
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error("Не удалось открыть архив")

            val dbInZip = File(staging, "database/${AppDatabase.DB_NAME}")
            if (!dbInZip.exists()) {
                error("В архиве нет файла базы данных")
            }

            AppDatabase.closeAndClear()
            val dbFile = context.getDatabasePath(AppDatabase.DB_NAME)
            dbFile.parentFile?.mkdirs()
            // Remove existing db files
            listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm")).forEach {
                if (it.exists()) it.delete()
            }
            dbInZip.copyTo(dbFile, overwrite = true)
            File(staging, "database/${AppDatabase.DB_NAME}-wal").takeIf { it.exists() }
                ?.copyTo(File(dbFile.path + "-wal"), overwrite = true)
            File(staging, "database/${AppDatabase.DB_NAME}-shm").takeIf { it.exists() }
                ?.copyTo(File(dbFile.path + "-shm"), overwrite = true)

            photoStorage.clearAllPhotos()
            val photosInZip = File(staging, "photos")
            if (photosInZip.exists()) {
                photosInZip.copyRecursively(photoStorage.photosRootDir(), overwrite = true)
            }
        } finally {
            staging.deleteRecursively()
            AppDatabase.reopen(context)
        }
    }

    private fun putFile(zip: ZipOutputStream, entryName: String, file: File) {
        zip.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun isSafeZipPath(name: String): Boolean {
        if (name.contains("..")) return false
        if (name.startsWith("/")) return false
        return name.startsWith("database/") || name.startsWith("photos/")
    }
}
