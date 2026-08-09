package com.cnctech.process.data.repository

import android.net.Uri
import androidx.room.withTransaction
import com.cnctech.process.data.db.AppDatabase
import com.cnctech.process.data.entity.CatalogCompatibilityEntity
import com.cnctech.process.data.entity.CatalogItemEntity
import com.cnctech.process.data.entity.CatalogItemPhotoEntity
import com.cnctech.process.data.entity.CatalogType
import com.cnctech.process.data.photo.PhotoStorage
import com.cnctech.process.data.rules.TechProcessRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

data class CatalogItemWithPhoto(
    val item: CatalogItemEntity,
    val coverPath: String?,
)

class CatalogRepository(
    private val db: AppDatabase,
    private val photos: PhotoStorage,
) {
    private val dao = db.catalogDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeByType(type: CatalogType, q: String): Flow<List<CatalogItemWithPhoto>> {
        return dao.observeByType(type, q.trim()).mapLatest { items ->
            val firstPhotos = dao.getFirstPhotos().associateBy { it.catalogItemId }
            items.map { CatalogItemWithPhoto(it, firstPhotos[it.id]?.filePath) }
        }
    }

    fun observePhotos(catalogItemId: Long): Flow<List<CatalogItemPhotoEntity>> =
        dao.observePhotos(catalogItemId)

    suspend fun getById(id: Long): CatalogItemEntity? = dao.getById(id)

    suspend fun getByIds(ids: List<Long>): List<CatalogItemEntity> =
        if (ids.isEmpty()) emptyList() else dao.getByIds(ids)

    suspend fun getPhotos(catalogItemId: Long): List<CatalogItemPhotoEntity> =
        dao.getPhotos(catalogItemId)

    suspend fun listByType(type: CatalogType): List<CatalogItemEntity> = dao.listByType(type)

    suspend fun create(type: CatalogType, name: String, note: String?): AppResult<Long> {
        val n = name.trim()
        if (n.isEmpty()) return AppResult.Err("Название обязательно")
        if (n.length > 255) return AppResult.Err("Название: максимум 255 символов")
        val noteTrimmed = note?.trim()?.takeIf { it.isNotEmpty() }
        if (noteTrimmed != null && noteTrimmed.length > 1000) {
            return AppResult.Err("Примечание: максимум 1000 символов")
        }
        if (dao.findByTypeAndName(type, n) != null) {
            return AppResult.Err("Позиция с таким названием уже есть")
        }
        val id = dao.insert(CatalogItemEntity(type = type, name = n, note = noteTrimmed))
        return AppResult.Ok(id)
    }

    suspend fun update(id: Long, name: String, note: String?): AppResult<Unit> {
        val existing = dao.getById(id) ?: return AppResult.Err("Позиция не найдена")
        val n = name.trim()
        if (n.isEmpty()) return AppResult.Err("Название обязательно")
        if (n.length > 255) return AppResult.Err("Название: максимум 255 символов")
        val noteTrimmed = note?.trim()?.takeIf { it.isNotEmpty() }
        if (noteTrimmed != null && noteTrimmed.length > 1000) {
            return AppResult.Err("Примечание: максимум 1000 символов")
        }
        val clash = dao.findByTypeAndName(existing.type, n)
        if (clash != null && clash.id != id) {
            return AppResult.Err("Позиция с таким названием уже есть")
        }
        dao.update(existing.copy(name = n, note = noteTrimmed))
        return AppResult.Ok(Unit)
    }

    suspend fun delete(id: Long): AppResult<Unit> {
        val existing = dao.getById(id) ?: return AppResult.Err("Позиция не найдена")
        val setupUsages = dao.countSetupUsages(id)
        val opUsages = dao.countOperationUsages(id)
        if (setupUsages > 0 || opUsages > 0) {
            return AppResult.Err(
                "Нельзя удалить «${existing.name}»: позиция используется в техпроцессе",
            )
        }
        val photoFiles = dao.getPhotos(id).map { it.filePath }
        dao.delete(id)
        photoFiles.forEach { photos.deleteFile(it) }
        photos.deleteDir(photos.catalogDir(id))
        return AppResult.Ok(Unit)
    }

    suspend fun addPhoto(catalogItemId: Long, uri: Uri): AppResult<Long> {
        if (dao.getById(catalogItemId) == null) return AppResult.Err("Позиция не найдена")
        return try {
            val file = photos.copyFromUri(uri, photos.catalogDir(catalogItemId))
            val nextOrder = dao.maxPhotoSortOrder(catalogItemId) + 1
            val id = dao.insertPhoto(
                CatalogItemPhotoEntity(
                    catalogItemId = catalogItemId,
                    filePath = file.absolutePath,
                    sortOrder = nextOrder,
                ),
            )
            AppResult.Ok(id)
        } catch (e: Exception) {
            AppResult.Err(e.message ?: "Не удалось сохранить фото")
        }
    }

    suspend fun deletePhoto(photoId: Long): AppResult<Unit> {
        val photo = dao.getPhoto(photoId) ?: return AppResult.Err("Фото не найдено")
        dao.deletePhoto(photoId)
        photos.deleteFile(photo.filePath)
        return AppResult.Ok(Unit)
    }

    suspend fun reorderPhotos(catalogItemId: Long, orderedIds: List<Long>): AppResult<Unit> {
        val existing = dao.getPhotos(catalogItemId)
        val orderById = TechProcessRules.validateReorderIds(orderedIds, existing.map { it.id })
            ?: return AppResult.Err("Некорректный порядок фото")
        db.withTransaction {
            for (photo in existing) {
                val newOrder = orderById[photo.id] ?: continue
                if (photo.sortOrder != newOrder) {
                    dao.updatePhoto(photo.copy(sortOrder = newOrder))
                }
            }
        }
        return AppResult.Ok(Unit)
    }

    suspend fun requireType(
        id: Long,
        expected: CatalogType,
        fieldName: String,
    ): AppResult<CatalogItemEntity> {
        val item = dao.getById(id) ?: return AppResult.Err("Не найден $fieldName")
        if (item.type != expected) return AppResult.Err("Некорректный тип $fieldName")
        return AppResult.Ok(item)
    }

    fun observeCompatibleTools(plateId: Long): Flow<List<CatalogItemEntity>> =
        dao.observeCompatibleTools(plateId)

    fun observeCompatiblePlates(toolId: Long): Flow<List<CatalogItemEntity>> =
        dao.observeCompatiblePlates(toolId)

    suspend fun setCompatible(toolId: Long, plateId: Long, linked: Boolean) {
        if (linked) {
            dao.insertCompatibility(CatalogCompatibilityEntity(toolId = toolId, plateId = plateId))
        } else {
            dao.deleteCompatibility(toolId = toolId, plateId = plateId)
        }
    }

    suspend fun updateStock(
        id: Long,
        stockQty: Int?,
        minStockThreshold: Int,
    ): AppResult<Unit> {
        if (stockQty != null && stockQty < 0) {
            return AppResult.Err("Количество не может быть отрицательным")
        }
        if (minStockThreshold < 0) {
            return AppResult.Err("Порог не может быть отрицательным")
        }
        val existing = dao.getById(id) ?: return AppResult.Err("Позиция не найдена")
        dao.update(existing.copy(stockQty = stockQty, minStockThreshold = minStockThreshold))
        return AppResult.Ok(Unit)
    }
}
