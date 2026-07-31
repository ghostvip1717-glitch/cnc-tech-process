package com.cnctech.process.data.photo

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

class PhotoStorage(private val context: Context) {
    private val photosRoot: File
        get() = File(context.filesDir, "photos").also { it.mkdirs() }

    fun partDir(partId: Long): File =
        File(photosRoot, "parts/$partId").also { it.mkdirs() }

    fun catalogDir(catalogItemId: Long): File =
        File(photosRoot, "catalog/$catalogItemId").also { it.mkdirs() }

    fun copyFromUri(uri: Uri, targetDir: File, extensionHint: String? = null): File {
        targetDir.mkdirs()
        val ext = extensionHint?.takeIf { it.isNotBlank() } ?: guessExtension(uri) ?: "jpg"
        val target = File(targetDir, "${UUID.randomUUID()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Не удалось прочитать выбранный файл")
        return target
    }

    fun deleteFile(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    fun deleteDir(dir: File) {
        if (!dir.exists()) return
        dir.walkBottomUp().forEach { it.delete() }
    }

    fun copyFile(source: File, targetDir: File, preferredName: String? = null): File {
        targetDir.mkdirs()
        val ext = source.extension.ifBlank { "jpg" }
        val target = File(targetDir, preferredName ?: "${UUID.randomUUID()}.$ext")
        source.copyTo(target, overwrite = true)
        return target
    }

    fun clearAllPhotos() {
        deleteDir(photosRoot)
        photosRoot.mkdirs()
    }

    fun photosRootDir(): File = photosRoot

    private fun guessExtension(uri: Uri): String? {
        val type = context.contentResolver.getType(uri) ?: return null
        return when (type) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/jpeg", "image/jpg" -> "jpg"
            else -> type.substringAfterLast('/', missingDelimiterValue = "").ifBlank { null }
        }
    }
}
