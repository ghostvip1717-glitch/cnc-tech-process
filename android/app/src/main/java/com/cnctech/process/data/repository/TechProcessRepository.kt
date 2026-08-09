package com.cnctech.process.data.repository

import android.net.Uri
import androidx.room.withTransaction
import com.cnctech.process.data.db.AppDatabase
import com.cnctech.process.data.entity.CatalogType
import com.cnctech.process.data.entity.OperationEntity
import com.cnctech.process.data.entity.OperationPhotoEntity
import com.cnctech.process.data.entity.SetupEntity
import com.cnctech.process.data.entity.SetupPhotoEntity
import com.cnctech.process.data.entity.TechProcessEntity
import com.cnctech.process.data.photo.PhotoStorage
import com.cnctech.process.data.rules.TechProcessRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest

data class SetupSummary(
    val setup: SetupEntity,
    val jawName: String,
    val operationCount: Int,
    val label: String,
)

data class SetupDetail(
    val setup: SetupEntity,
    val jawName: String,
    val operations: List<OperationEntity>,
    val label: String,
    val photos: List<SetupPhotoEntity>,
    val operationPhotos: Map<Long, List<OperationPhotoEntity>>,
)

private data class SetupDetailBundle(
    val setup: SetupEntity?,
    val operations: List<OperationEntity>,
    val setupPhotos: List<SetupPhotoEntity>,
    val opPhotos: List<OperationPhotoEntity>,
)

data class RequiredItem(
    val id: Long,
    val type: CatalogType,
    val name: String,
)

data class RequiredItems(
    val tools: List<RequiredItem>,
    val plates: List<RequiredItem>,
    val jaws: List<RequiredItem>,
)

class TechProcessRepository(
    private val db: AppDatabase,
    private val catalogRepository: CatalogRepository,
    private val photos: PhotoStorage,
) {
    private val tpDao = db.techProcessDao()
    private val catalogDao = db.catalogDao()
    private val partDao = db.partDao()

    suspend fun ensureTechProcess(partId: Long): AppResult<Long> {
        if (partDao.getPart(partId) == null) return AppResult.Err("Деталь не найдена")
        val existing = tpDao.getByPartId(partId)
        if (existing != null) return AppResult.Ok(existing.id)
        val id = tpDao.insert(TechProcessEntity(partId = partId))
        return AppResult.Ok(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeSetupSummaries(partId: Long): Flow<List<SetupSummary>> {
        return tpDao.observeByPartId(partId).flatMapLatest { tp ->
            if (tp == null) {
                flowOf(emptyList())
            } else {
                tpDao.observeSetups(tp.id).mapLatest { setups ->
                    setups.map { setup ->
                        val jaw = catalogDao.getById(setup.jawId)
                        val ops = tpDao.getOperations(setup.id)
                        SetupSummary(
                            setup = setup,
                            jawName = jaw?.name ?: "—",
                            operationCount = ops.size,
                            label = TechProcessRules.setupOrderLabel(setup.order),
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeSetupDetail(setupId: Long): Flow<SetupDetail?> {
        return combine(
            tpDao.observeSetup(setupId),
            tpDao.observeOperations(setupId),
            tpDao.observeSetupPhotos(setupId),
            tpDao.observeOperationPhotosForSetup(setupId),
        ) { setup, operations, setupPhotos, opPhotos ->
            SetupDetailBundle(setup, operations, setupPhotos, opPhotos)
        }.mapLatest { bundle ->
            val setup = bundle.setup ?: return@mapLatest null
            val jaw = catalogDao.getById(setup.jawId)
            SetupDetail(
                setup = setup,
                jawName = jaw?.name ?: "—",
                operations = bundle.operations,
                label = TechProcessRules.setupOrderLabel(setup.order),
                photos = bundle.setupPhotos,
                operationPhotos = bundle.opPhotos.groupBy { it.operationId },
            )
        }
    }

    suspend fun addSetup(partId: Long, jawId: Long): AppResult<Long> {
        val tpResult = ensureTechProcess(partId)
        val tpId = when (tpResult) {
            is AppResult.Ok -> tpResult.value
            is AppResult.Err -> return tpResult
        }
        when (val check = catalogRepository.requireType(jawId, CatalogType.jaw, "jawId")) {
            is AppResult.Err -> return check
            is AppResult.Ok -> Unit
        }
        val nextOrder = tpDao.maxSetupOrder(tpId) + 1
        val id = tpDao.insertSetup(
            SetupEntity(techProcessId = tpId, order = nextOrder.coerceAtLeast(0), jawId = jawId),
        )
        return AppResult.Ok(id)
    }

    suspend fun updateSetupJaw(setupId: Long, jawId: Long): AppResult<Unit> {
        val setup = tpDao.getSetup(setupId) ?: return AppResult.Err("Установ не найден")
        when (val check = catalogRepository.requireType(jawId, CatalogType.jaw, "jawId")) {
            is AppResult.Err -> return check
            is AppResult.Ok -> Unit
        }
        tpDao.updateSetup(setup.copy(jawId = jawId))
        return AppResult.Ok(Unit)
    }

    suspend fun updateSetupNote(setupId: Long, note: String?): AppResult<Unit> {
        val setup = tpDao.getSetup(setupId) ?: return AppResult.Err("Установ не найден")
        val trimmed = note?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmed != null && trimmed.length > 1000) {
            return AppResult.Err("Заметка: максимум 1000 символов")
        }
        tpDao.updateSetup(setup.copy(note = trimmed))
        return AppResult.Ok(Unit)
    }

    suspend fun deleteSetup(setupId: Long): AppResult<Unit> {
        if (tpDao.getSetup(setupId) == null) return AppResult.Err("Установ не найден")
        db.withTransaction {
            val ops = tpDao.getOperations(setupId)
            val opPhotoFiles = ops.flatMap { tpDao.getOperationPhotos(it.id) }.map { it.filePath }
            val setupPhotoFiles = tpDao.getSetupPhotos(setupId).map { it.filePath }
            tpDao.deleteOperationsForSetup(setupId)
            tpDao.deleteSetup(setupId)
            opPhotoFiles.forEach { photos.deleteFile(it) }
            setupPhotoFiles.forEach { photos.deleteFile(it) }
            ops.forEach { photos.deleteDir(photos.operationDir(it.id)) }
            photos.deleteDir(photos.setupDir(setupId))
        }
        return AppResult.Ok(Unit)
    }

    suspend fun addOperation(
        setupId: Long,
        opNumber: String,
        title: String,
        toolId: Long,
        plateId: Long,
        comment: String?,
    ): AppResult<Long> {
        if (tpDao.getSetup(setupId) == null) return AppResult.Err("Установ не найден")
        val num = opNumber.trim()
        val tit = title.trim()
        if (num.isEmpty()) return AppResult.Err("Номер операции обязателен")
        if (tit.isEmpty()) return AppResult.Err("Название операции обязательно")
        when (val check = catalogRepository.requireType(toolId, CatalogType.tool, "toolId")) {
            is AppResult.Err -> return check
            is AppResult.Ok -> Unit
        }
        when (val check = catalogRepository.requireType(plateId, CatalogType.plate, "plateId")) {
            is AppResult.Err -> return check
            is AppResult.Ok -> Unit
        }
        val nextOrder = tpDao.maxOperationOrder(setupId) + 1
        val id = tpDao.insertOperation(
            OperationEntity(
                setupId = setupId,
                order = nextOrder.coerceAtLeast(0),
                opNumber = num,
                title = tit,
                toolId = toolId,
                plateId = plateId,
                comment = comment?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        return AppResult.Ok(id)
    }

    suspend fun updateOperation(
        operationId: Long,
        opNumber: String,
        title: String,
        toolId: Long,
        plateId: Long,
        comment: String?,
    ): AppResult<Unit> {
        val existing = tpDao.getOperation(operationId) ?: return AppResult.Err("Операция не найдена")
        val num = opNumber.trim()
        val tit = title.trim()
        if (num.isEmpty()) return AppResult.Err("Номер операции обязателен")
        if (tit.isEmpty()) return AppResult.Err("Название операции обязательно")
        when (val check = catalogRepository.requireType(toolId, CatalogType.tool, "toolId")) {
            is AppResult.Err -> return check
            is AppResult.Ok -> Unit
        }
        when (val check = catalogRepository.requireType(plateId, CatalogType.plate, "plateId")) {
            is AppResult.Err -> return check
            is AppResult.Ok -> Unit
        }
        tpDao.updateOperation(
            existing.copy(
                opNumber = num,
                title = tit,
                toolId = toolId,
                plateId = plateId,
                comment = comment?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        return AppResult.Ok(Unit)
    }

    suspend fun deleteOperation(operationId: Long): AppResult<Unit> {
        if (tpDao.getOperation(operationId) == null) return AppResult.Err("Операция не найдена")
        val photoFiles = tpDao.getOperationPhotos(operationId).map { it.filePath }
        tpDao.deleteOperation(operationId)
        photoFiles.forEach { photos.deleteFile(it) }
        photos.deleteDir(photos.operationDir(operationId))
        return AppResult.Ok(Unit)
    }

    suspend fun reorderOperations(setupId: Long, orderedIds: List<Long>): AppResult<Unit> {
        val existing = tpDao.getOperations(setupId)
        val orderById = TechProcessRules.validateReorderIds(orderedIds, existing.map { it.id })
            ?: return AppResult.Err("Некорректный порядок операций")
        db.withTransaction {
            for (op in existing) {
                val newOrder = orderById[op.id] ?: continue
                if (op.order != newOrder) {
                    tpDao.updateOperation(op.copy(order = newOrder))
                }
            }
        }
        return AppResult.Ok(Unit)
    }

    suspend fun addSetupPhoto(setupId: Long, uri: Uri): AppResult<Long> {
        if (tpDao.getSetup(setupId) == null) return AppResult.Err("Установ не найден")
        return try {
            val file = photos.copyFromUri(uri, photos.setupDir(setupId))
            val nextOrder = tpDao.maxSetupPhotoSortOrder(setupId) + 1
            val id = tpDao.insertSetupPhoto(
                SetupPhotoEntity(
                    setupId = setupId,
                    filePath = file.absolutePath,
                    sortOrder = nextOrder,
                ),
            )
            AppResult.Ok(id)
        } catch (e: Exception) {
            AppResult.Err(e.message ?: "Не удалось сохранить фото")
        }
    }

    suspend fun deleteSetupPhoto(photoId: Long): AppResult<Unit> {
        val photo = tpDao.getSetupPhoto(photoId) ?: return AppResult.Err("Фото не найдено")
        tpDao.deleteSetupPhoto(photoId)
        photos.deleteFile(photo.filePath)
        return AppResult.Ok(Unit)
    }

    suspend fun reorderSetupPhotos(setupId: Long, orderedIds: List<Long>): AppResult<Unit> {
        val existing = tpDao.getSetupPhotos(setupId)
        val orderById = TechProcessRules.validateReorderIds(orderedIds, existing.map { it.id })
            ?: return AppResult.Err("Некорректный порядок фото")
        db.withTransaction {
            for (photo in existing) {
                val newOrder = orderById[photo.id] ?: continue
                if (photo.sortOrder != newOrder) {
                    tpDao.updateSetupPhoto(photo.copy(sortOrder = newOrder))
                }
            }
        }
        return AppResult.Ok(Unit)
    }

    fun observeSetupPhotos(setupId: Long): Flow<List<SetupPhotoEntity>> =
        tpDao.observeSetupPhotos(setupId)

    fun observeOperationPhotos(operationId: Long): Flow<List<OperationPhotoEntity>> =
        tpDao.observeOperationPhotos(operationId)

    suspend fun addOperationPhoto(operationId: Long, uri: Uri): AppResult<Long> {
        if (tpDao.getOperation(operationId) == null) return AppResult.Err("Операция не найдена")
        return try {
            val file = photos.copyFromUri(uri, photos.operationDir(operationId))
            val nextOrder = tpDao.maxOperationPhotoSortOrder(operationId) + 1
            val id = tpDao.insertOperationPhoto(
                OperationPhotoEntity(
                    operationId = operationId,
                    filePath = file.absolutePath,
                    sortOrder = nextOrder,
                ),
            )
            AppResult.Ok(id)
        } catch (e: Exception) {
            AppResult.Err(e.message ?: "Не удалось сохранить фото")
        }
    }

    suspend fun deleteOperationPhoto(photoId: Long): AppResult<Unit> {
        val photo = tpDao.getOperationPhoto(photoId) ?: return AppResult.Err("Фото не найдено")
        tpDao.deleteOperationPhoto(photoId)
        photos.deleteFile(photo.filePath)
        return AppResult.Ok(Unit)
    }

    suspend fun reorderOperationPhotos(operationId: Long, orderedIds: List<Long>): AppResult<Unit> {
        val existing = tpDao.getOperationPhotos(operationId)
        val orderById = TechProcessRules.validateReorderIds(orderedIds, existing.map { it.id })
            ?: return AppResult.Err("Некорректный порядок фото")
        db.withTransaction {
            for (photo in existing) {
                val newOrder = orderById[photo.id] ?: continue
                if (photo.sortOrder != newOrder) {
                    tpDao.updateOperationPhoto(photo.copy(sortOrder = newOrder))
                }
            }
        }
        return AppResult.Ok(Unit)
    }

    suspend fun getRequiredItems(partId: Long): RequiredItems {
        val tp = tpDao.getByPartId(partId) ?: return RequiredItems(emptyList(), emptyList(), emptyList())
        val setups = tpDao.getSetups(tp.id)
        val jawIds = linkedSetOf<Long>()
        val toolIds = linkedSetOf<Long>()
        val plateIds = linkedSetOf<Long>()
        for (setup in setups) {
            jawIds.add(setup.jawId)
            for (op in tpDao.getOperations(setup.id)) {
                toolIds.add(op.toolId)
                plateIds.add(op.plateId)
            }
        }
        suspend fun resolve(ids: Set<Long>, type: CatalogType): List<RequiredItem> {
            return ids.sorted().mapNotNull { id ->
                val item = catalogDao.getById(id) ?: return@mapNotNull null
                RequiredItem(id = item.id, type = type, name = item.name)
            }
        }
        return RequiredItems(
            tools = resolve(toolIds, CatalogType.tool),
            plates = resolve(plateIds, CatalogType.plate),
            jaws = resolve(jawIds, CatalogType.jaw),
        )
    }
}
