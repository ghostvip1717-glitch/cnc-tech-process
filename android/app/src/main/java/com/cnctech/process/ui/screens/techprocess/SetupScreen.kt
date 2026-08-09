package com.cnctech.process.ui.screens.techprocess

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cnctech.process.CncApp
import com.cnctech.process.data.entity.CatalogItemEntity
import com.cnctech.process.data.entity.CatalogType
import com.cnctech.process.data.entity.OperationEntity
import com.cnctech.process.data.repository.AppResult
import com.cnctech.process.ui.components.CatalogDropdown
import com.cnctech.process.ui.components.ConfirmDialog
import com.cnctech.process.ui.components.CncBottomSheet
import com.cnctech.process.ui.components.CncFab
import com.cnctech.process.ui.components.CncTextField
import com.cnctech.process.ui.components.DangerButton
import com.cnctech.process.ui.components.DragHandleIcon
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.PhotoStrip
import com.cnctech.process.ui.components.PhotoStripItem
import com.cnctech.process.ui.components.PrimaryButton
import com.cnctech.process.ui.components.SkeletonStack
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncDanger
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
import com.cnctech.process.ui.theme.CncSurface
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    setupId: Long,
    onDeleted: () -> Unit,
    onError: (String) -> Unit,
    onOpenCatalogPhoto: (Long, Int) -> Unit = { _, _ -> },
    onOpenSetupPhoto: (Int) -> Unit = {},
    onOpenOperationPhoto: (Long, Int) -> Unit = { _, _ -> },
) {
    val tpRepo = CncApp.instance.techProcessRepository
    val catalogRepo = CncApp.instance.catalogRepository
    val scope = rememberCoroutineScope()
    val detail by tpRepo.observeSetupDetail(setupId).collectAsState(initial = null)

    var jaws by remember { mutableStateOf<List<CatalogItemEntity>>(emptyList()) }
    var tools by remember { mutableStateOf<List<CatalogItemEntity>>(emptyList()) }
    var plates by remember { mutableStateOf<List<CatalogItemEntity>>(emptyList()) }
    var coverById by remember { mutableStateOf<Map<Long, String?>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val loadedJaws = catalogRepo.listByType(CatalogType.jaw)
        val loadedTools = catalogRepo.listByType(CatalogType.tool)
        val loadedPlates = catalogRepo.listByType(CatalogType.plate)
        jaws = loadedJaws
        tools = loadedTools
        plates = loadedPlates
        val ids = (loadedJaws + loadedTools + loadedPlates).map { it.id }.distinct()
        coverById = ids.associateWith { id ->
            catalogRepo.getPhotos(id).firstOrNull()?.filePath
        }
    }

    var busy by remember { mutableStateOf(false) }
    var confirmSetupDelete by remember { mutableStateOf(false) }
    var pendingOpDelete by remember { mutableStateOf<OperationEntity?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }

    var setupNote by remember { mutableStateOf("") }
    var setupNoteInitializedFor by remember { mutableStateOf<Long?>(null) }

    var opNumber by remember { mutableStateOf("") }
    var opTitle by remember { mutableStateOf("") }
    var toolId by remember { mutableStateOf<Long?>(null) }
    var plateId by remember { mutableStateOf<Long?>(null) }
    var comment by remember { mutableStateOf("") }

    var editBuffers by remember { mutableStateOf<Map<Long, OpEdit>>(emptyMap()) }

    LaunchedEffect(detail?.setup?.id) {
        val setup = detail?.setup ?: return@LaunchedEffect
        if (setupNoteInitializedFor != setup.id) {
            setupNote = setup.note.orEmpty()
            setupNoteInitializedFor = setup.id
        }
    }

    var opOrder by remember { mutableStateOf<List<Long>>(emptyList()) }
    var draggingOpId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()

    LaunchedEffect(detail?.operations) {
        val ops = detail?.operations.orEmpty()
        opOrder = ops.map { it.id }
        editBuffers = ops.associate { op ->
            op.id to OpEdit(op.opNumber, op.title, op.toolId, op.plateId, op.comment.orEmpty())
        }
    }

    val createCompatiblePlates by catalogRepo.observeCompatiblePlates(toolId ?: -1L)
        .collectAsState(initial = emptyList())
    val createPlateOptions = remember(toolId, createCompatiblePlates, plates) {
        if (toolId != null && createCompatiblePlates.isNotEmpty()) createCompatiblePlates else plates
    }
    val createPlateOptionIds = remember(createPlateOptions) { createPlateOptions.map { it.id }.toSet() }
    LaunchedEffect(toolId, createPlateOptionIds) {
        if (plateId != null && plateId !in createPlateOptionIds) {
            plateId = null
        }
    }

    val opsById = remember(detail?.operations) {
        detail?.operations.orEmpty().associateBy { it.id }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (detail == null) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                SkeletonStack()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            ) {
                item(key = "header") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Установ ${detail!!.label}",
                            color = CncOnSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CatalogDropdown(
                            label = "Кулачки",
                            type = CatalogType.jaw,
                            items = jaws,
                            selectedId = detail!!.setup.jawId,
                            onSelect = { id ->
                                scope.launch {
                                    when (val result = tpRepo.updateSetupJaw(setupId, id)) {
                                        is AppResult.Ok -> Unit
                                        is AppResult.Err -> onError(result.message)
                                    }
                                }
                            },
                            onError = onError,
                            coverPath = coverById[detail!!.setup.jawId],
                            onOpenCover = { onOpenCatalogPhoto(detail!!.setup.jawId, 0) },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CncTextField(
                            value = setupNote,
                            onValueChange = { setupNote = it },
                            label = "Заметка",
                            singleLine = false,
                            minLines = 3,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PrimaryButton(
                            text = "Сохранить заметку",
                            busy = busy,
                            onClick = {
                                scope.launch {
                                    busy = true
                                    when (val result = tpRepo.updateSetupNote(setupId, setupNote)) {
                                        is AppResult.Ok -> Unit
                                        is AppResult.Err -> onError(result.message)
                                    }
                                    busy = false
                                }
                            },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Фото установа", color = CncOnSurface, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        PhotoStrip(
                            photos = detail!!.photos.map { PhotoStripItem(it.id, it.filePath) },
                            onAdd = { uri ->
                                scope.launch {
                                    when (val result = tpRepo.addSetupPhoto(setupId, uri)) {
                                        is AppResult.Ok -> Unit
                                        is AppResult.Err -> onError(result.message)
                                    }
                                }
                            },
                            onDelete = { id ->
                                scope.launch {
                                    when (val result = tpRepo.deleteSetupPhoto(id)) {
                                        is AppResult.Ok -> Unit
                                        is AppResult.Err -> onError(result.message)
                                    }
                                }
                            },
                            onReorder = { ids ->
                                scope.launch {
                                    when (val result = tpRepo.reorderSetupPhotos(setupId, ids)) {
                                        is AppResult.Ok -> Unit
                                        is AppResult.Err -> onError(result.message)
                                    }
                                }
                            },
                            onOpenViewer = onOpenSetupPhoto,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Операции", color = CncOnSurface, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (detail!!.operations.isEmpty()) {
                            EmptyText("Нет операций — нажмите +")
                        }
                    }
                }

                if (detail!!.operations.isNotEmpty()) {
                    items(opOrder, key = { it }) { opId ->
                        val op = opsById[opId] ?: return@items
                        val buf = editBuffers[op.id]
                            ?: OpEdit(op.opNumber, op.title, op.toolId, op.plateId, op.comment.orEmpty())
                        val isDragging = draggingOpId == opId
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .zIndex(if (isDragging) 1f else 0f)
                                .offset {
                                    IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0)
                                },
                        ) {
                            OperationEditor(
                                operation = op,
                                buffer = buf,
                                tools = tools,
                                allPlates = plates,
                                coverById = coverById,
                                photos = detail!!.operationPhotos[op.id].orEmpty().map {
                                    PhotoStripItem(it.id, it.filePath)
                                },
                                dragHandle = {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .pointerInput(opId, opOrder, listState) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggingOpId = opId
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragEnd = {
                                                        draggingOpId = null
                                                        dragOffsetY = 0f
                                                        scope.launch {
                                                            when (val result = tpRepo.reorderOperations(setupId, opOrder)) {
                                                                is AppResult.Ok -> Unit
                                                                is AppResult.Err -> onError(result.message)
                                                            }
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        draggingOpId = null
                                                        dragOffsetY = 0f
                                                        opOrder = detail?.operations.orEmpty().map { it.id }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount.y
                                                        val draggedInfo = listState.layoutInfo.visibleItemsInfo
                                                            .firstOrNull { it.key == opId }
                                                            ?: return@detectDragGesturesAfterLongPress
                                                        val fingerY = draggedInfo.offset + draggedInfo.size / 2f + dragOffsetY
                                                        val target = listState.layoutInfo.visibleItemsInfo
                                                            .firstOrNull { info ->
                                                                info.key != opId &&
                                                                    info.key is Long &&
                                                                    fingerY >= info.offset &&
                                                                    fingerY < info.offset + info.size
                                                            }
                                                            ?: return@detectDragGesturesAfterLongPress
                                                        val toKey = target.key as Long
                                                        val from = opOrder.indexOf(opId)
                                                        val to = opOrder.indexOf(toKey)
                                                        if (from >= 0 && to >= 0 && from != to) {
                                                            val next = opOrder.toMutableList()
                                                            next.removeAt(from)
                                                            next.add(to, opId)
                                                            opOrder = next
                                                            dragOffsetY = 0f
                                                        }
                                                    },
                                                )
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        DragHandleIcon()
                                    }
                                },
                                onBufferChange = { next -> editBuffers = editBuffers + (op.id to next) },
                                onDelete = { pendingOpDelete = op },
                                onOpenCatalogPhoto = onOpenCatalogPhoto,
                                onOpenOperationPhoto = { index -> onOpenOperationPhoto(op.id, index) },
                                onPhotoAdd = { uri ->
                                    scope.launch {
                                        when (val result = tpRepo.addOperationPhoto(op.id, uri)) {
                                            is AppResult.Ok -> Unit
                                            is AppResult.Err -> onError(result.message)
                                        }
                                    }
                                },
                                onPhotoDelete = { id ->
                                    scope.launch {
                                        when (val result = tpRepo.deleteOperationPhoto(id)) {
                                            is AppResult.Ok -> Unit
                                            is AppResult.Err -> onError(result.message)
                                        }
                                    }
                                },
                                onPhotoReorder = { ids ->
                                    scope.launch {
                                        when (val result = tpRepo.reorderOperationPhotos(op.id, ids)) {
                                            is AppResult.Ok -> Unit
                                            is AppResult.Err -> onError(result.message)
                                        }
                                    }
                                },
                                onSave = { saveBuffer ->
                                    val selectedTool = saveBuffer.toolId
                                    val selectedPlate = saveBuffer.plateId
                                    if (selectedTool == null || selectedPlate == null) {
                                        onError("Выберите инструмент и пластину")
                                    } else {
                                        scope.launch {
                                            when (
                                                val result = tpRepo.updateOperation(
                                                    op.id,
                                                    saveBuffer.opNumber,
                                                    saveBuffer.title,
                                                    selectedTool,
                                                    selectedPlate,
                                                    saveBuffer.comment,
                                                )
                                            ) {
                                                is AppResult.Ok -> Unit
                                                is AppResult.Err -> onError(result.message)
                                            }
                                        }
                                    }
                                },
                                onError = onError,
                            )
                        }
                    }
                }

                item(key = "footer") {
                    Column {
                        Spacer(modifier = Modifier.height(4.dp))
                        DangerButton(text = "Удалить установ", onClick = { confirmSetupDelete = true })
                    }
                }
            }
        }
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            CncFab(onClick = {
                opNumber = ""
                opTitle = ""
                toolId = null
                plateId = null
                comment = ""
                sheetOpen = true
            })
        }
    }

    CncBottomSheet(open = sheetOpen, title = "Новая операция", onDismiss = { if (!busy) sheetOpen = false }) {
        CncTextField(value = opNumber, onValueChange = { opNumber = it }, label = "Номер")
        Spacer(modifier = Modifier.height(8.dp))
        CncTextField(value = opTitle, onValueChange = { opTitle = it }, label = "Что делаем")
        Spacer(modifier = Modifier.height(8.dp))
        CatalogDropdown(
            label = "Инструмент",
            type = CatalogType.tool,
            items = tools,
            selectedId = toolId,
            onSelect = { id -> toolId = id },
            onError = onError,
            coverPath = toolId?.let { coverById[it] },
            onOpenCover = toolId?.let { id -> { onOpenCatalogPhoto(id, 0) } },
        )
        Spacer(modifier = Modifier.height(8.dp))
        CatalogDropdown(
            label = "Пластина",
            type = CatalogType.plate,
            items = createPlateOptions,
            selectedId = plateId,
            onSelect = { id -> plateId = id },
            onError = onError,
            autoLinkToolId = toolId,
            coverPath = plateId?.let { coverById[it] },
            onOpenCover = plateId?.let { id -> { onOpenCatalogPhoto(id, 0) } },
        )
        Spacer(modifier = Modifier.height(8.dp))
        CncTextField(
            value = comment,
            onValueChange = { comment = it },
            label = "Комментарий",
            singleLine = false,
            minLines = 2,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(
            text = "Создать",
            busy = busy,
            enabled = toolId != null && plateId != null,
            onClick = {
                val selectedTool = toolId ?: return@PrimaryButton
                val selectedPlate = plateId ?: return@PrimaryButton
                scope.launch {
                    busy = true
                    when (val result = tpRepo.addOperation(setupId, opNumber, opTitle, selectedTool, selectedPlate, comment)) {
                        is AppResult.Ok -> sheetOpen = false
                        is AppResult.Err -> onError(result.message)
                    }
                    busy = false
                }
            },
        )
    }

    ConfirmDialog(
        open = confirmSetupDelete,
        title = "Удалить установ?",
        message = "Удалить установ ${detail?.label.orEmpty()}? Действие нельзя отменить",
        busy = busy,
        onCancel = { confirmSetupDelete = false },
        onConfirm = {
            scope.launch {
                busy = true
                when (val result = tpRepo.deleteSetup(setupId)) {
                    is AppResult.Ok -> {
                        confirmSetupDelete = false
                        onDeleted()
                    }
                    is AppResult.Err -> onError(result.message)
                }
                busy = false
            }
        },
    )

    ConfirmDialog(
        open = pendingOpDelete != null,
        title = "Удалить операцию?",
        message = "Удалить операцию ${pendingOpDelete?.opNumber.orEmpty()}? Действие нельзя отменить",
        busy = busy,
        onCancel = { pendingOpDelete = null },
        onConfirm = {
            val op = pendingOpDelete ?: return@ConfirmDialog
            scope.launch {
                busy = true
                when (val result = tpRepo.deleteOperation(op.id)) {
                    is AppResult.Ok -> pendingOpDelete = null
                    is AppResult.Err -> onError(result.message)
                }
                busy = false
            }
        },
    )
}

@Composable
private fun OperationEditor(
    operation: OperationEntity,
    buffer: OpEdit,
    tools: List<CatalogItemEntity>,
    allPlates: List<CatalogItemEntity>,
    coverById: Map<Long, String?>,
    photos: List<PhotoStripItem>,
    dragHandle: @Composable () -> Unit,
    onBufferChange: (OpEdit) -> Unit,
    onDelete: () -> Unit,
    onOpenCatalogPhoto: (Long, Int) -> Unit,
    onOpenOperationPhoto: (Int) -> Unit,
    onPhotoAdd: (Uri) -> Unit,
    onPhotoDelete: (Long) -> Unit,
    onPhotoReorder: (List<Long>) -> Unit,
    onSave: (OpEdit) -> Unit,
    onError: (String) -> Unit,
) {
    val catalogRepo = CncApp.instance.catalogRepository
    val compatiblePlates by catalogRepo.observeCompatiblePlates(buffer.toolId ?: -1L)
        .collectAsState(initial = emptyList())
    val plateOptions = remember(buffer.toolId, compatiblePlates, allPlates) {
        if (buffer.toolId != null && compatiblePlates.isNotEmpty()) compatiblePlates else allPlates
    }
    val plateOptionIds = remember(plateOptions) { plateOptions.map { it.id }.toSet() }
    LaunchedEffect(buffer.toolId, plateOptionIds) {
        if (buffer.plateId != null && buffer.plateId !in plateOptionIds) {
            onBufferChange(buffer.copy(plateId = null))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
            .background(CncSurface)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            dragHandle()
            Text(
                "Операция ${operation.opNumber}",
                color = CncOnSurfaceSecondary,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = CncDanger)
            }
        }
        CncTextField(
            value = buffer.opNumber,
            onValueChange = { onBufferChange(buffer.copy(opNumber = it)) },
            label = "Номер",
        )
        Spacer(modifier = Modifier.height(8.dp))
        CncTextField(
            value = buffer.title,
            onValueChange = { onBufferChange(buffer.copy(title = it)) },
            label = "Что делаем",
        )
        Spacer(modifier = Modifier.height(8.dp))
        CatalogDropdown(
            label = "Инструмент",
            type = CatalogType.tool,
            items = tools,
            selectedId = buffer.toolId,
            onSelect = { id -> onBufferChange(buffer.copy(toolId = id)) },
            onError = onError,
            coverPath = buffer.toolId?.let { coverById[it] },
            onOpenCover = buffer.toolId?.let { id -> { onOpenCatalogPhoto(id, 0) } },
        )
        Spacer(modifier = Modifier.height(8.dp))
        CatalogDropdown(
            label = "Пластина",
            type = CatalogType.plate,
            items = plateOptions,
            selectedId = buffer.plateId,
            onSelect = { id -> onBufferChange(buffer.copy(plateId = id)) },
            onError = onError,
            autoLinkToolId = buffer.toolId,
            coverPath = buffer.plateId?.let { coverById[it] },
            onOpenCover = buffer.plateId?.let { id -> { onOpenCatalogPhoto(id, 0) } },
        )
        Spacer(modifier = Modifier.height(8.dp))
        CncTextField(
            value = buffer.comment,
            onValueChange = { onBufferChange(buffer.copy(comment = it)) },
            label = "Комментарий",
            singleLine = false,
            minLines = 2,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Фото операции", color = CncOnSurfaceSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        PhotoStrip(
            photos = photos,
            onAdd = onPhotoAdd,
            onDelete = onPhotoDelete,
            onReorder = onPhotoReorder,
            onOpenViewer = onOpenOperationPhoto,
        )
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            text = "Сохранить",
            enabled = buffer.toolId != null && buffer.plateId != null,
            onClick = { onSave(buffer) },
        )
    }
}

private data class OpEdit(
    val opNumber: String,
    val title: String,
    val toolId: Long?,
    val plateId: Long?,
    val comment: String,
)
