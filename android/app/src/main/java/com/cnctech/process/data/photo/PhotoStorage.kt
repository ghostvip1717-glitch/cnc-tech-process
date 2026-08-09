package com.cnctech.process.data.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

class PhotoStorage(private val context: Context) {
    private val photosRoot: File
        get() = File(context.filesDir, "photos").also { it.mkdirs() }

    fun partDir(partId: Long): File =
        File(photosRoot, "parts/$partId").also { it.mkdirs() }

    fun catalogDir(catalogItemId: Long): File =
        File(photosRoot, "catalog/$catalogItemId").also { it.mkdirs() }

    fun setupDir(setupId: Long): File =
        File(photosRoot, "setups/$setupId").also { it.mkdirs() }

    fun operationDir(operationId: Long): File =
        File(photosRoot, "operations/$operationId").also { it.mkdirs() }

    /**
     * Temp JPEG in cacheDir/camera for TakePicture.
     * Returns FileProvider content Uri; [CaptureTarget.file] should be deleted after copy/cancel.
     */
    fun createCaptureUri(context: Context = this.context): CaptureTarget {
        val dir = File(context.cacheDir, "camera").also { it.mkdirs() }
        val file = File(dir, "capture_${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return CaptureTarget(uri = uri, file = file)
    }

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

    fun deleteFile(file: File) {
        runCatching { file.takeIf { it.exists() }?.delete() }
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

data class CaptureTarget(
    val uri: Uri,
    val file: File,
)
