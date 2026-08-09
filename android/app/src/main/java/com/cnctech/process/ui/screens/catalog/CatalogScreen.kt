package com.cnctech.process.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Square
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cnctech.process.CncApp
import com.cnctech.process.data.entity.CatalogItemEntity
import com.cnctech.process.data.entity.CatalogType
import com.cnctech.process.data.prefs.LayoutMode
import com.cnctech.process.data.repository.CatalogItemWithPhoto
import com.cnctech.process.data.repository.AppResult
import com.cnctech.process.ui.components.ConfirmDialog
import com.cnctech.process.ui.components.CncBottomSheet
import com.cnctech.process.ui.components.CncFab
import com.cnctech.process.ui.components.CncTextField
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.PlateStockBadge
import com.cnctech.process.ui.components.PrimaryButton
import com.cnctech.process.ui.components.fieldColors
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncDanger
import com.cnctech.process.ui.theme.CncMuted
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
import com.cnctech.process.ui.theme.CncPrimary
import com.cnctech.process.ui.theme.CncSkeleton
import com.cnctech.process.ui.theme.CncSurface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.io.File

private data class Tab(val type: CatalogType, val label: String)

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun CatalogScreen(
    activeType: CatalogType,
    layoutMode: LayoutMode,
    onActiveTypeChange: (CatalogType) -> Unit,
    onOpenItem: (Long) -> Unit,
    onCreated: (Long) -> Unit,
    onError: (String) -> Unit,
) {
    val repo = CncApp.instance.catalogRepository
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        Tab(CatalogType.tool, "Инструмент"),
        Tab(CatalogType.plate, "Пластины"),
        Tab(CatalogType.jaw, "Кулачки"),
    )
    var search by remember { mutableStateOf("") }
    val typeFlow = remember { MutableStateFlow(activeType) }
    val queryFlow = remember { MutableStateFlow("") }

    LaunchedEffect(activeType) { typeFlow.value = activeType }
    LaunchedEffect(search) {
        delay(350)
        queryFlow.value = search.trim()
    }

    val items by remember {
        combine(typeFlow, queryFlow) { t, q -> t to q }
            .flatMapLatest { (t, q) -> repo.observeByType(t, q) }
    }.collectAsState(initial = emptyList())

    var sheetMode by remember { mutableStateOf<String?>(null) } // create|edit
    var editing by remember { mutableStateOf<CatalogItemEntity?>(null) }
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var stockQtyText by remember { mutableStateOf("") }
    var minStockText by remember { mutableStateOf("3") }
    var busy by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CatalogItemEntity?>(null) }

    val formType = editing?.type ?: activeType

    var allTools by remember { mutableStateOf<List<CatalogItemEntity>>(emptyList()) }
    var allPlates by remember { mutableStateOf<List<CatalogItemEntity>>(emptyList()) }
    LaunchedEffect(sheetMode, editing?.id, formType) {
        if (sheetMode == "edit" && editing != null) {
            when (formType) {
                CatalogType.plate -> allTools = repo.listByType(CatalogType.tool)
                CatalogType.tool -> allPlates = repo.listByType(CatalogType.plate)
                CatalogType.jaw -> Unit
            }
        }
    }

    val compatPlateId =
        if (sheetMode == "edit" && editing != null && formType == CatalogType.plate) editing!!.id else -1L
    val compatToolId =
        if (sheetMode == "edit" && editing != null && formType == CatalogType.tool) editing!!.id else -1L
    val linkedTools by repo.observeCompatibleTools(compatPlateId).collectAsState(initial = emptyList())
    val linkedPlates by repo.observeCompatiblePlates(compatToolId).collectAsState(initial = emptyList())
    val linkedToolIds = remember(linkedTools) { linkedTools.map { it.id }.toSet() }
    val linkedPlateIds = remember(linkedPlates) { linkedPlates.map { it.id }.toSet() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tabs.forEach { tab ->
                    val selected = tab.type == activeType
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, if (selected) CncPrimary else CncBorder, RoundedCornerShape(10.dp))
                            .background(if (selected) CncPrimary.copy(alpha = 0.1f) else CncSurface)
                            .clickable {
                                onActiveTypeChange(tab.type)
                                sheetMode = null
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = when (tab.type) {
                                CatalogType.tool -> Icons.Default.Build
                                CatalogType.plate -> Icons.Default.Square
                                CatalogType.jaw -> Icons.Default.Hardware
                            },
                            contentDescription = null,
                            tint = if (selected) CncPrimary else CncMuted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            tab.label,
                            color = if (selected) CncPrimary else CncOnSurface,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск по названию") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = fieldColors(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (items.isEmpty()) {
                EmptyText("Позиции не найдены")
            } else {
                when (layoutMode) {
                    LayoutMode.List -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(items, key = { it.item.id }) { row ->
                                CatalogListRow(
                                    row = row,
                                    onOpenItem = { onOpenItem(row.item.id) },
                                    onEdit = {
                                        editing = row.item
                                        name = row.item.name
                                        note = row.item.note.orEmpty()
                                        stockQtyText = row.item.stockQty?.toString().orEmpty()
                                        minStockText = row.item.minStockThreshold.toString()
                                        sheetMode = "edit"
                                    },
                                    onDelete = { pendingDelete = row.item },
                                )
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                    LayoutMode.GridLarge, LayoutMode.GridCompact -> {
                        val compact = layoutMode == LayoutMode.GridCompact
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (compact) 3 else 2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            gridItems(items, key = { it.item.id }) { row ->
                                CatalogGridCell(
                                    row = row,
                                    onOpenItem = { onOpenItem(row.item.id) },
                                    compact = compact,
                                )
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            CncFab(onClick = {
                editing = null
                name = ""
                note = ""
                stockQtyText = ""
                minStockText = "3"
                sheetMode = "create"
            })
        }
    }

    CncBottomSheet(
        open = sheetMode != null,
        title = if (sheetMode == "edit") "Изменить позицию" else "Новая позиция",
        onDismiss = { if (!busy) sheetMode = null },
    ) {
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

            if (formType == CatalogType.plate) {
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

            if (sheetMode == "edit" && editing != null && formType == CatalogType.plate) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Совместимый инструмент",
                    color = CncOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (allTools.isEmpty()) {
                    Text("Нет позиций инструмента", color = CncOnSurfaceSecondary, fontSize = 13.sp)
                } else {
                    allTools.forEach { tool ->
                        val checked = tool.id in linkedToolIds
                        CompatibilityRow(
                            title = tool.name,
                            checked = checked,
                            onCheckedChange = { next ->
                                scope.launch {
                                    repo.setCompatible(
                                        toolId = tool.id,
                                        plateId = editing!!.id,
                                        linked = next,
                                    )
                                }
                            },
                        )
                    }
                }
            }

            if (sheetMode == "edit" && editing != null && formType == CatalogType.tool) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Совместимые пластины",
                    color = CncOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (allPlates.isEmpty()) {
                    Text("Нет пластин", color = CncOnSurfaceSecondary, fontSize = 13.sp)
                } else {
                    allPlates.forEach { plate ->
                        val checked = plate.id in linkedPlateIds
                        CompatibilityRow(
                            title = plate.name,
                            checked = checked,
                            badge = {
                                PlateStockBadge(
                                    stockQty = plate.stockQty,
                                    minStockThreshold = plate.minStockThreshold,
                                )
                            },
                            onCheckedChange = { next ->
                                scope.launch {
                                    repo.setCompatible(
                                        toolId = editing!!.id,
                                        plateId = plate.id,
                                        linked = next,
                                    )
                                }
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = if (sheetMode == "edit") "Сохранить" else "Создать",
                busy = busy,
                onClick = {
                    scope.launch {
                        busy = true
                        val plateStock = if (formType == CatalogType.plate) {
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

                        var createdId: Long? = null
                        val result = if (sheetMode == "edit" && editing != null) {
                            when (val r = repo.update(editing!!.id, name, note)) {
                                is AppResult.Err -> r
                                is AppResult.Ok -> {
                                    if (stockPair != null) {
                                        repo.updateStock(
                                            editing!!.id,
                                            stockPair.first,
                                            stockPair.second,
                                        )
                                    } else {
                                        AppResult.Ok(Unit)
                                    }
                                }
                            }
                        } else {
                            when (val r = repo.create(activeType, name, note)) {
                                is AppResult.Err -> r
                                is AppResult.Ok -> {
                                    createdId = r.value
                                    if (stockPair != null) {
                                        repo.updateStock(r.value, stockPair.first, stockPair.second)
                                    } else {
                                        AppResult.Ok(Unit)
                                    }
                                }
                            }
                        }
                        when (result) {
                            is AppResult.Ok -> {
                                sheetMode = null
                                createdId?.let(onCreated)
                            }
                            is AppResult.Err -> onError(result.message)
                        }
                        busy = false
                    }
                },
            )
        }
    }

    ConfirmDialog(
        open = pendingDelete != null,
        title = "Удалить позицию?",
        message = "Удалить «${pendingDelete?.name.orEmpty()}»? Действие нельзя отменить",
        busy = busy,
        onCancel = { pendingDelete = null },
        onConfirm = {
            val item = pendingDelete ?: return@ConfirmDialog
            scope.launch {
                busy = true
                when (val r = repo.delete(item.id)) {
                    is AppResult.Ok -> pendingDelete = null
                    is AppResult.Err -> {
                        pendingDelete = null
                        onError(r.message)
                    }
                }
                busy = false
            }
        },
    )
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


@Composable
private fun CatalogListRow(
    row: CatalogItemWithPhoto,
    onOpenItem: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
            .background(CncSurface)
            .clickable(onClick = onOpenItem)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CncSkeleton),
        ) {
            row.coverPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    row.item.name,
                    color = CncOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (row.item.type == CatalogType.plate) {
                    PlateStockBadge(
                        stockQty = row.item.stockQty,
                        minStockThreshold = row.item.minStockThreshold,
                    )
                }
            }
            row.item.note?.let {
                Text(it, color = CncOnSurfaceSecondary, fontSize = 13.sp)
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Изменить", tint = CncPrimary)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = CncDanger)
        }
    }
}

@Composable
private fun CatalogGridCell(
    row: CatalogItemWithPhoto,
    onOpenItem: () -> Unit,
    compact: Boolean,
) {
    if (compact) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, CncBorder, RoundedCornerShape(10.dp))
                .background(CncSkeleton)
                .clickable(onClick = onOpenItem),
        ) {
            row.coverPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Text(
                text = row.item.name,
                color = CncOnSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(CncSurface.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
                .background(CncSurface)
                .clickable(onClick = onOpenItem),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(CncSkeleton),
            ) {
                row.coverPath?.let { path ->
                    AsyncImage(
                        model = File(path),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = row.item.name,
                    color = CncOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                )
                if (row.item.type == CatalogType.plate) {
                    Spacer(modifier = Modifier.height(6.dp))
                    PlateStockBadge(
                        stockQty = row.item.stockQty,
                        minStockThreshold = row.item.minStockThreshold,
                    )
                }
            }
        }
    }
}
