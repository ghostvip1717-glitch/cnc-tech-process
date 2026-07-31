package com.cnctech.process.ui.screens.techprocess

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cnctech.process.CncApp
import com.cnctech.process.data.entity.CatalogItemEntity
import com.cnctech.process.data.entity.CatalogType
import com.cnctech.process.data.entity.OperationEntity
import com.cnctech.process.data.repository.AppResult
import com.cnctech.process.ui.components.ConfirmDialog
import com.cnctech.process.ui.components.CncBottomSheet
import com.cnctech.process.ui.components.CncFab
import com.cnctech.process.ui.components.CncTextField
import com.cnctech.process.ui.components.DangerButton
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.PrimaryButton
import com.cnctech.process.ui.components.ReorderableColumn
import com.cnctech.process.ui.components.SkeletonStack
import com.cnctech.process.ui.components.fieldColors
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncDanger
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
import com.cnctech.process.ui.theme.CncSurface
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    setupId: Long,
    onDeleted: () -> Unit,
    onError: (String) -> Unit,
) {
    val tpRepo = CncApp.instance.techProcessRepository
    val catalogRepo = CncApp.instance.catalogRepository
    val scope = rememberCoroutineScope()
    val detail by tpRepo.observeSetupDetail(setupId).collectAsState(initial = null)

    var jaws by remember { mutableStateOf<List<CatalogItemEntity>>(emptyList()) }
    var tools by remember { mutableStateOf<List<CatalogItemEntity>>(emptyList()) }
    var plates by remember { mutableStateOf<List<CatalogItemEntity>>(emptyList()) }
    LaunchedEffect(Unit) {
        jaws = catalogRepo.listByType(CatalogType.jaw)
        tools = catalogRepo.listByType(CatalogType.tool)
        plates = catalogRepo.listByType(CatalogType.plate)
    }

    var busy by remember { mutableStateOf(false) }
    var jawExpanded by remember { mutableStateOf(false) }
    var confirmSetupDelete by remember { mutableStateOf(false) }
    var pendingOpDelete by remember { mutableStateOf<OperationEntity?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }

    var opNumber by remember { mutableStateOf("") }
    var opTitle by remember { mutableStateOf("") }
    var toolId by remember { mutableStateOf<Long?>(null) }
    var plateId by remember { mutableStateOf<Long?>(null) }
    var comment by remember { mutableStateOf("") }
    var toolExpanded by remember { mutableStateOf(false) }
    var plateExpanded by remember { mutableStateOf(false) }

    // Inline edit buffers keyed by operation id
    var editBuffers by remember { mutableStateOf<Map<Long, OpEdit>>(emptyMap()) }

    LaunchedEffect(detail?.operations) {
        val ops = detail?.operations.orEmpty()
        editBuffers = ops.associate { op ->
            op.id to OpEdit(op.opNumber, op.title, op.toolId, op.plateId, op.comment.orEmpty())
        }
    }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (detail == null) {
                SkeletonStack()
                return@Column
            }
            Text(
                "Установ ${detail!!.label}",
                color = CncOnSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            ExposedDropdownMenuBox(expanded = jawExpanded, onExpandedChange = { jawExpanded = it }) {
                OutlinedTextField(
                    value = detail!!.jawName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Кулачки") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jawExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = jawExpanded, onDismissRequest = { jawExpanded = false }) {
                    jaws.forEach { jaw ->
                        DropdownMenuItem(
                            text = { Text(jaw.name, color = CncOnSurface) },
                            onClick = {
                                jawExpanded = false
                                scope.launch {
                                    when (val r = tpRepo.updateSetupJaw(setupId, jaw.id)) {
                                        is AppResult.Ok -> Unit
                                        is AppResult.Err -> onError(r.message)
                                    }
                                }
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Операции", color = CncOnSurface, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            if (detail!!.operations.isEmpty()) {
                EmptyText("Нет операций — нажмите +")
            } else {
                ReorderableColumn(
                    items = detail!!.operations,
                    keyOf = { it.id },
                    onReorder = { ids ->
                        scope.launch {
                            when (val r = tpRepo.reorderOperations(setupId, ids)) {
                                is AppResult.Ok -> Unit
                                is AppResult.Err -> onError(r.message)
                            }
                        }
                    },
                ) { op, handle ->
                    val buf = editBuffers[op.id] ?: OpEdit(op.opNumber, op.title, op.toolId, op.plateId, op.comment.orEmpty())
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
                            .background(CncSurface)
                            .padding(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            handle()
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { pendingOpDelete = op }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = CncDanger)
                            }
                        }
                        CncTextField(
                            value = buf.opNumber,
                            onValueChange = { v ->
                                editBuffers = editBuffers + (op.id to buf.copy(opNumber = v))
                            },
                            label = "Номер",
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CncTextField(
                            value = buf.title,
                            onValueChange = { v ->
                                editBuffers = editBuffers + (op.id to buf.copy(title = v))
                            },
                            label = "Что делаем",
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CatalogDropdown(
                            label = "Инструмент",
                            items = tools,
                            selectedId = buf.toolId,
                            onSelect = { id ->
                                editBuffers = editBuffers + (op.id to buf.copy(toolId = id))
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CatalogDropdown(
                            label = "Пластина",
                            items = plates,
                            selectedId = buf.plateId,
                            onSelect = { id ->
                                editBuffers = editBuffers + (op.id to buf.copy(plateId = id))
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CncTextField(
                            value = buf.comment,
                            onValueChange = { v ->
                                editBuffers = editBuffers + (op.id to buf.copy(comment = v))
                            },
                            label = "Комментарий",
                            singleLine = false,
                            minLines = 2,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PrimaryButton(
                            text = "Сохранить",
                            onClick = {
                                scope.launch {
                                    when (
                                        val r = tpRepo.updateOperation(
                                            op.id,
                                            buf.opNumber,
                                            buf.title,
                                            buf.toolId,
                                            buf.plateId,
                                            buf.comment,
                                        )
                                    ) {
                                        is AppResult.Ok -> Unit
                                        is AppResult.Err -> onError(r.message)
                                    }
                                }
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            DangerButton(text = "Удалить установ", onClick = { confirmSetupDelete = true })
            Spacer(modifier = Modifier.height(80.dp))
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            CncFab(onClick = {
                opNumber = ""
                opTitle = ""
                toolId = tools.firstOrNull()?.id
                plateId = plates.firstOrNull()?.id
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
        CatalogDropdown(label = "Инструмент", items = tools, selectedId = toolId, onSelect = { toolId = it })
        Spacer(modifier = Modifier.height(8.dp))
        CatalogDropdown(label = "Пластина", items = plates, selectedId = plateId, onSelect = { plateId = it })
        Spacer(modifier = Modifier.height(8.dp))
        CncTextField(value = comment, onValueChange = { comment = it }, label = "Комментарий", singleLine = false, minLines = 2)
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(
            text = "Создать",
            busy = busy,
            enabled = toolId != null && plateId != null,
            onClick = {
                val t = toolId ?: return@PrimaryButton
                val p = plateId ?: return@PrimaryButton
                scope.launch {
                    busy = true
                    when (val r = tpRepo.addOperation(setupId, opNumber, opTitle, t, p, comment)) {
                        is AppResult.Ok -> sheetOpen = false
                        is AppResult.Err -> onError(r.message)
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
                when (val r = tpRepo.deleteSetup(setupId)) {
                    is AppResult.Ok -> {
                        confirmSetupDelete = false
                        onDeleted()
                    }
                    is AppResult.Err -> onError(r.message)
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
                when (val r = tpRepo.deleteOperation(op.id)) {
                    is AppResult.Ok -> pendingOpDelete = null
                    is AppResult.Err -> onError(r.message)
                }
                busy = false
            }
        },
    )
}

private data class OpEdit(
    val opNumber: String,
    val title: String,
    val toolId: Long,
    val plateId: Long,
    val comment: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogDropdown(
    label: String,
    items: List<CatalogItemEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = items.find { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = fieldColors(),
            shape = RoundedCornerShape(8.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.name, color = CncOnSurface) },
                    onClick = {
                        onSelect(item.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
