package com.cnctech.process.data.repository

import androidx.room.withTransaction
import com.cnctech.process.data.db.AppDatabase
import com.cnctech.process.data.entity.CatalogType
import com.cnctech.process.data.entity.OperationEntity
import com.cnctech.process.data.entity.SetupEntity
import com.cnctech.process.data.entity.TechProcessEntity
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
        ) { setup, operations ->
            setup to operations
        }.mapLatest { (setup, operations) ->
            if (setup == null) {
                null
            } else {
                val jaw = catalogDao.getById(setup.jawId)
                SetupDetail(
                    setup = setup,
                    jawName = jaw?.name ?: "—",
                    operations = operations,
                    label = TechProcessRules.setupOrderLabel(setup.order),
                )
            }
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
        // maxSetupOrder returns -1 when empty → +1 = 0; when has max N → N+1. Good.
        // Wait: maxSetupOrder returns -1 empty, +1 = 0. If has 0, max=0, +1=1. Correct.
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

    suspend fun deleteSetup(setupId: Long): AppResult<Unit> {
        if (tpDao.getSetup(setupId) == null) return AppResult.Err("Установ не найден")
        db.withTransaction {
            tpDao.deleteOperationsForSetup(setupId)
            tpDao.deleteSetup(setupId)
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
        tpDao.deleteOperation(operationId)
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
