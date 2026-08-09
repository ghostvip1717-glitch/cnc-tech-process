package com.cnctech.process.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.cnctech.process.data.repository.AppResult
import com.cnctech.process.ui.components.CncBottomSheet
import com.cnctech.process.ui.components.CncTextField
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.PhotoStrip
import com.cnctech.process.ui.components.PhotoStripItem
import com.cnctech.process.ui.components.PlateStockBadge
import com.cnctech.process.ui.components.PrimaryButton
import com.cnctech.process.ui.components.SkeletonStack
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
import com.cnctech.process.ui.theme.CncPrimary
import com.cnctech.process.ui.theme.CncSurface
import kotlinx.coroutines.launch

@Composable
fun CatalogItemDetailScreen(
    catalogItemId: Long,
    onEdit: () -> Unit,
    onOpenPhoto: (Int) -> Unit,
    onOpenRelated: (Long) -> Unit,
    onError: (String) -> Unit,
) {
    val repo = CncApp.instance.catalogRepository
    val scope = rememberCoroutineScope()
    val photos by repo.observePhotos(catalogItemId).collectAsState(initial = emptyList())
    var reloadKey by remember { mutableIntStateOf(0) }
    var item by remember { mutableStateOf<CatalogItemEntity?>(null) }
    var loading by remember { mutableStateOf(true) }
    var editOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var stockQtyText by remember { mutableStateOf("") }
    var minStockText by remember { mutableStateOf("3") }
    var busy by remember { mutableStateOf(false) }
    var allTools by remember { mutableStateOf<List<CatalogItemEntity>>(emptyList()) }
    var allPlates by remember { mutableStateOf<List<CatalogItemEntity>>(emptyList()) }

    LaunchedEffect(catalogItemId, reloadKey) {
        loading = true
        item = repo.getById(catalogItemId)
        loading = false
    }

    val current = item
    val compatibleTools by repo.observeCompatibleTools(
        if (current?.type == CatalogType.plate) catalogItemId else -1L,
    ).collectAsState(initial = emptyList())
    val compatiblePlates by repo.observeCompatiblePlates(
        if (current?.type == CatalogType.tool) catalogItemId else -1L,
    ).collectAsState(initial = emptyList())
    val linkedToolIds = remember(compatibleTools) { compatibleTools.map { it.id }.toSet() }
    val linkedPlateIds = remember(compatiblePlates) { compatiblePlates.map { it.id }.toSet() }

    LaunchedEffect(editOpen, current?.id) {
        val itemType = current?.type ?: return@LaunchedEffect
        if (editOpen) {
            when (itemType) {
                CatalogType.plate -> allTools = repo.listByType(CatalogType.tool)
                CatalogType.tool -> allPlates = repo.listByType(CatalogType.plate)
                CatalogType.jaw -> Unit
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        when {
            loading -> {
                SkeletonStack(rows = 3)
                return@Column
            }
            current == null -> {
                EmptyText("Позиция не найдена")
                return@Column
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                current!!.name,
                color = CncOnSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (current.type == CatalogType.plate) {
                PlateStockBadge(
                    stockQty = current.stockQty,
                    minStockThreshold = current.minStockThreshold,
                )
            }
        }
        current.note?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = CncOnSurfaceSecondary, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Фото", color = CncOnSurface, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        PhotoStrip(
            photos = photos.map { PhotoStripItem(it.id, it.filePath) },
            onAdd = { uri ->
                scope.launch {
                    when (val result = repo.addPhoto(catalogItemId, uri)) {
                        is AppResult.Ok -> Unit
                        is AppResult.Err -> onError(result.message)
                    }
                }
            },
            onDelete = { id ->
                scope.launch {
                    when (val result = repo.deletePhoto(id)) {
                        is AppResult.Ok -> Unit
                        is AppResult.Err -> onError(result.message)
                    }
                }
            },
            onReorder = { ids ->
                scope.launch {
                    when (val result = repo.reorderPhotos(catalogItemId, ids)) {
                        is AppResult.Ok -> Unit
                        is AppResult.Err -> onError(result.message)
                    }
                }
            },
            onOpenViewer = onOpenPhoto,
        )

        Spacer(modifier = Modifier.height(20.dp))
        val related = when (current.type) {
            CatalogType.tool -> compatiblePlates
            CatalogType.plate -> compatibleTools
            CatalogType.jaw -> emptyList()
        }
        Text("Совместимость", color = CncOnSurface, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        if (related.isEmpty()) {
            EmptyText("Связанные позиции не указаны")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                related.forEach { relatedItem ->
                    RelatedRow(item = relatedItem, onClick = { onOpenRelated(relatedItem.id) })
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(
            text = "Изменить",
            onClick = {
                onEdit()
                name = current.name
                note = current.note.orEmpty()
                stockQtyText = current.stockQty?.toString().orEmpty()
                minStockText = current.minStockThreshold.toString()
                editOpen = true
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    CncBottomSheet(
        open = editOpen && current != null,
        title = "Изменить позицию",
        onDismiss = { if (!busy) editOpen = false },
    ) {
        current?.let { editItem ->
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            CncTextField(value = name, onValueChange = { name = it }, label = "Название")
            Spacer(modifier = Modifier.height(12.dp))
            CncTextField(
                value = note,
                onValueChange = { note = it },
                label = "Примечание",
                singleLine = false,
                minLines = 2,
            )

            if (editItem.type == CatalogType.plate) {
                Spacer(modifier = Modifier.height(12.dp))
                CncTextField(
                    value = stockQtyText,
                    onValueChange = { stockQtyText = it.filter { ch -> ch.isDigit() } },
                    label = "Количество на складе",
                )
                Spacer(modifier = Modifier.height(12.dp))
                CncTextField(
                    value = minStockText,
                    onValueChange = { minStockText = it.filter { ch -> ch.isDigit() } },
                    label = "Мин. остаток для предупреждения",
                )
            }

            if (editItem.type == CatalogType.plate) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Совместимый инструмент", color = CncOnSurface, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                if (allTools.isEmpty()) {
                    Text("Нет позиций инструмента", color = CncOnSurfaceSecondary, fontSize = 13.sp)
                } else {
                    allTools.forEach { tool ->
                        CompatibilityRow(
                            title = tool.name,
                            checked = tool.id in linkedToolIds,
                            onCheckedChange = { next ->
                                scope.launch {
                                    repo.setCompatible(toolId = tool.id, plateId = editItem.id, linked = next)
                                }
                            },
                        )
                    }
                }
            }

            if (editItem.type == CatalogType.tool) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Совместимые пластины", color = CncOnSurface, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                if (allPlates.isEmpty()) {
                    Text("Нет пластин", color = CncOnSurfaceSecondary, fontSize = 13.sp)
                } else {
                    allPlates.forEach { plate ->
                        CompatibilityRow(
                            title = plate.name,
                            checked = plate.id in linkedPlateIds,
                            badge = {
                                PlateStockBadge(
                                    stockQty = plate.stockQty,
                                    minStockThreshold = plate.minStockThreshold,
                                )
                            },
                            onCheckedChange = { next ->
                                scope.launch {
                                    repo.setCompatible(toolId = editItem.id, plateId = plate.id, linked = next)
                                }
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = "Сохранить",
                busy = busy,
                onClick = {
                    scope.launch {
                        busy = true
                        val plateStock = if (editItem.type == CatalogType.plate) {
                            parsePlateStock(stockQtyText, minStockText)
                        } else {
                            null
                        }
                        if (plateStock is AppResult.Err) {
                            onError(plateStock.message)
                            busy = false
                            return@launch
                        }
                        val stockPair = (plateStock as? AppResult.Ok)?.value
                        val result = when (val update = repo.update(editItem.id, name, note)) {
                            is AppResult.Err -> update
                            is AppResult.Ok -> {
                                if (stockPair != null) {
                                    repo.updateStock(editItem.id, stockPair.first, stockPair.second)
                                } else {
                                    AppResult.Ok(Unit)
                                }
                            }
                        }
                        when (result) {
                            is AppResult.Ok -> {
                                editOpen = false
                                reloadKey++
                            }
                            is AppResult.Err -> onError(result.message)
                        }
                        busy = false
                    }
                },
            )
        }
        }
    }
}

@Composable
private fun RelatedRow(
    item: CatalogItemEntity,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
            .background(CncSurface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            item.name,
            color = CncOnSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (item.type == CatalogType.plate) {
            PlateStockBadge(
                stockQty = item.stockQty,
                minStockThreshold = item.minStockThreshold,
            )
        }
    }
}

private fun parsePlateStock(
    stockQtyText: String,
    minStockText: String,
): AppResult<Pair<Int?, Int>> {
    val stockTrim = stockQtyText.trim()
    val stockQty = if (stockTrim.isEmpty()) {
        null
    } else {
        stockTrim.toIntOrNull()
            ?: return AppResult.Err("Количество должно быть числом")
    }
    if (stockQty != null && stockQty < 0) {
        return AppResult.Err("Количество не может быть отрицательным")
    }
    val minTrim = minStockText.trim()
    val minStock = if (minTrim.isEmpty()) {
        3
    } else {
        minTrim.toIntOrNull()
            ?: return AppResult.Err("Порог должен быть числом")
    }
    if (minStock < 0) {
        return AppResult.Err("Порог не может быть отрицательным")
    }
    return AppResult.Ok(stockQty to minStock)
}

@Composable
private fun CompatibilityRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    badge: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = CncPrimary,
                uncheckedColor = CncBorder,
            ),
        )
        Text(
            title,
            color = CncOnSurface,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
        )
        badge?.invoke()
    }
}
