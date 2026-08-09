package com.cnctech.process.data.repository

import android.net.Uri
import androidx.room.withTransaction
import com.cnctech.process.data.db.AppDatabase
import com.cnctech.process.data.entity.PartEntity
import com.cnctech.process.data.entity.PartPhotoEntity
import com.cnctech.process.data.photo.PhotoStorage
import com.cnctech.process.data.rules.TechProcessRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest

data class PartWithPhotos(
    val part: PartEntity,
    val photos: List<PartPhotoEntity>,
)

class PartRepository(
    private val db: AppDatabase,
    private val photos: PhotoStorage,
) {
    private val partDao = db.partDao()
    private val tpDao = db.techProcessDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeParts(q: String): Flow<List<PartWithPhotos>> {
        return partDao.observeParts(q.trim()).mapLatest { parts ->
            parts.map { part -> PartWithPhotos(part, partDao.getPhotos(part.id)) }
        }
    }

    fun observePart(partId: Long): Flow<PartWithPhotos?> {
        return combine(
            partDao.observePart(partId),
            partDao.observePhotos(partId),
        ) { part, photosList ->
            part?.let { PartWithPhotos(it, photosList) }
        }
    }

    suspend fun create(number: String, title: String): AppResult<Long> {
        val n = number.trim()
        val t = title.trim()
        if (n.isEmpty()) return AppResult.Err("Номер обязателен")
        if (t.isEmpty()) return AppResult.Err("Название обязательно")
        return try {
            val id = partDao.insert(
                PartEntity(
                    number = n,
                    title = t,
                    createdAt = System.currentTimeMillis(),
                    machiningTimeMinutes = null,
                    programCount = null,
                ),
            )
            AppResult.Ok(id)
        } catch (_: Exception) {
            AppResult.Err("Номер уже используется или ошибка сохранения")
        }
    }

    suspend fun update(
        partId: Long,
        number: String,
        title: String,
        machiningTimeMinutes: Int?,
        programCount: Int?,
        note: String? = null,
    ): AppResult<Unit> {
        val existing = partDao.getPart(partId) ?: return AppResult.Err("Деталь не найдена")
        val n = number.trim()
        val t = title.trim()
        if (n.isEmpty()) return AppResult.Err("Номер обязателен")
        if (t.isEmpty()) return AppResult.Err("Название обязательно")
        if (machiningTimeMinutes != null && machiningTimeMinutes < 0) {
            return AppResult.Err("Время обработки не может быть отрицательным")
        }
        if (programCount != null && programCount < 0) {
            return AppResult.Err("Количество программ не может быть отрицательным")
        }
        val noteTrimmed = note?.trim()?.takeIf { it.isNotEmpty() }
        if (noteTrimmed != null && noteTrimmed.length > 1000) {
            return AppResult.Err("Заметка: максимум 1000 символов")
        }
        return try {
            partDao.update(
                existing.copy(
                    number = n,
                    title = t,
                    machiningTimeMinutes = machiningTimeMinutes,
                    programCount = programCount,
                    note = noteTrimmed,
                ),
            )
            AppResult.Ok(Unit)
        } catch (_: Exception) {
            AppResult.Err("Номер уже используется или ошибка сохранения")
        }
    }

    suspend fun delete(partId: Long): AppResult<Unit> {
        val part = partDao.getPart(partId) ?: return AppResult.Err("Деталь не найдена")
        db.withTransaction {
            val photoFiles = partDao.getPhotos(partId).map { it.filePath }
            val tp = tpDao.getByPartId(partId)
            if (tp != null) {
                val setups = tpDao.getSetups(tp.id)
                for (setup in setups) {
                    val ops = tpDao.getOperations(setup.id)
                    val opPhotoFiles = ops.flatMap { tpDao.getOperationPhotos(it.id) }.map { it.filePath }
                    val setupPhotoFiles = tpDao.getSetupPhotos(setup.id).map { it.filePath }
                    tpDao.deleteOperationsForSetup(setup.id)
                    tpDao.deleteSetup(setup.id)
                    opPhotoFiles.forEach { photos.deleteFile(it) }
                    setupPhotoFiles.forEach { photos.deleteFile(it) }
                    ops.forEach { photos.deleteDir(photos.operationDir(it.id)) }
                    photos.deleteDir(photos.setupDir(setup.id))
                }
                tpDao.delete(tp.id)
            }
            partDao.delete(part.id)
            photoFiles.forEach { photos.deleteFile(it) }
            photos.deleteDir(photos.partDir(partId))
        }
        return AppResult.Ok(Unit)
    }

    suspend fun addPhoto(partId: Long, uri: Uri): AppResult<Long> {
        if (partDao.getPart(partId) == null) return AppResult.Err("Деталь не найдена")
        return try {
            val file = photos.copyFromUri(uri, photos.partDir(partId))
            val nextOrder = partDao.maxPhotoSortOrder(partId) + 1
            val id = partDao.insertPhoto(
                PartPhotoEntity(partId = partId, filePath = file.absolutePath, sortOrder = nextOrder),
            )
            AppResult.Ok(id)
        } catch (e: Exception) {
            AppResult.Err(e.message ?: "Не удалось сохранить фото")
        }
    }

    suspend fun deletePhoto(photoId: Long): AppResult<Unit> {
        val photo = partDao.getPhoto(photoId) ?: return AppResult.Err("Фото не найдено")
        partDao.deletePhoto(photoId)
        photos.deleteFile(photo.filePath)
        return AppResult.Ok(Unit)
    }

    suspend fun reorderPhotos(partId: Long, orderedIds: List<Long>): AppResult<Unit> {
        val existing = partDao.getPhotos(partId)
        val orderById = TechProcessRules.validateReorderIds(orderedIds, existing.map { it.id })
            ?: return AppResult.Err("Некорректный порядок фото")
        db.withTransaction {
            for (photo in existing) {
                val newOrder = orderById[photo.id] ?: continue
                if (photo.sortOrder != newOrder) {
                    partDao.updatePhoto(photo.copy(sortOrder = newOrder))
                }
            }
        }
        return AppResult.Ok(Unit)
    }
}
