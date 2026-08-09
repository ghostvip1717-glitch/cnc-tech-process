package com.cnctech.process.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    /**
     * Copies [uri] into [targetDir], downscaling to max long side 1600px and JPEG q=85.
     * Applies EXIF orientation into pixels so the saved file displays upright without EXIF.
     * Falls back to raw byte-copy if decode fails.
     */
    suspend fun copyFromUri(uri: Uri, targetDir: File, extensionHint: String? = null): File =
        withContext(Dispatchers.IO) {
            targetDir.mkdirs()
            val downscaled = decodeDownscaledJpeg(uri)
            if (downscaled != null) {
                val target = File(targetDir, "${UUID.randomUUID()}.jpg")
                try {
                    target.outputStream().use { out ->
                        if (!downscaled.compress(Bitmap.CompressFormat.JPEG, 85, out)) {
                            error("compress failed")
                        }
                    }
                    return@withContext target
                } catch (_: Exception) {
                    target.delete()
                } finally {
                    downscaled.recycle()
                }
            }

            val ext = extensionHint?.takeIf { it.isNotBlank() } ?: guessExtension(uri) ?: "jpg"
            val target = File(targetDir, "${UUID.randomUUID()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Не удалось прочитать выбранный файл")
            target
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

    private fun decodeDownscaledJpeg(uri: Uri): Bitmap? {
        return try {
            val orientation = readExifOrientation(uri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            } ?: return null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, MAX_LONG_SIDE)
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, opts)
            } ?: return null

            applyExifOrientation(decoded, orientation)
        } catch (_: Exception) {
            null
        }
    }

    private fun readExifOrientation(uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExifOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            else -> return source
        }
        return try {
            val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            if (rotated !== source) source.recycle()
            rotated
        } catch (_: Exception) {
            source
        }
    }

    private fun computeInSampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (maxOf(w, h) / 2 >= maxSide) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    private fun guessExtension(uri: Uri): String? {
        val type = context.contentResolver.getType(uri) ?: return null
        return when (type) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/jpeg", "image/jpg" -> "jpg"
            else -> type.substringAfterLast('/', missingDelimiterValue = "").ifBlank { null }
        }
    }

    companion object {
        private const val MAX_LONG_SIDE = 1600
    }
}

data class CaptureTarget(
    val uri: Uri,
    val file: File,
)
