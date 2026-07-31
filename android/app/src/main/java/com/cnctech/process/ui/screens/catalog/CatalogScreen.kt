package com.cnctech.process.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Square
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
import com.cnctech.process.data.repository.AppResult
import com.cnctech.process.ui.components.ConfirmDialog
import com.cnctech.process.ui.components.CncBottomSheet
import com.cnctech.process.ui.components.CncFab
import com.cnctech.process.ui.components.CncTextField
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.PrimaryButton
import com.cnctech.process.ui.components.SkeletonStack
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
    onOpenGallery: (Long, String) -> Unit,
    onError: (String) -> Unit,
) {
    val repo = CncApp.instance.catalogRepository
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        Tab(CatalogType.tool, "Инструмент"),
        Tab(CatalogType.plate, "Пластины"),
        Tab(CatalogType.jaw, "Кулачки"),
    )
    var activeType by remember { mutableStateOf(CatalogType.tool) }
    var search by remember { mutableStateOf("") }
    val typeFlow = remember { MutableStateFlow(CatalogType.tool) }
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
    var busy by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CatalogItemEntity?>(null) }

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
                                activeType = tab.type
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
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items, key = { it.item.id }) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
                                .background(CncSurface)
                                .clickable { onOpenGallery(row.item.id, row.item.name) }
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
                                Text(row.item.name, color = CncOnSurface, fontWeight = FontWeight.SemiBold)
                                row.item.note?.let {
                                    Text(it, color = CncOnSurfaceSecondary, fontSize = 13.sp)
                                }
                            }
                            IconButton(onClick = {
                                editing = row.item
                                name = row.item.name
                                note = row.item.note.orEmpty()
                                sheetMode = "edit"
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Изменить", tint = CncPrimary)
                            }
                            IconButton(onClick = { pendingDelete = row.item }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = CncDanger)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            CncFab(onClick = {
                editing = null
                name = ""
                note = ""
                sheetMode = "create"
            })
        }
    }

    CncBottomSheet(
        open = sheetMode != null,
        title = if (sheetMode == "edit") "Изменить позицию" else "Новая позиция",
        onDismiss = { if (!busy) sheetMode = null },
    ) {
        CncTextField(value = name, onValueChange = { name = it }, label = "Название")
        Spacer(modifier = Modifier.height(12.dp))
        CncTextField(
            value = note,
            onValueChange = { note = it },
            label = "Примечание",
            singleLine = false,
            minLines = 2,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(
            text = if (sheetMode == "edit") "Сохранить" else "Создать",
            busy = busy,
            onClick = {
                scope.launch {
                    busy = true
                    val result = if (sheetMode == "edit" && editing != null) {
                        repo.update(editing!!.id, name, note)
                    } else {
                        repo.create(activeType, name, note)
                    }
                    when (result) {
                        is AppResult.Ok -> sheetMode = null
                        is AppResult.Err -> onError(result.message)
                    }
                    busy = false
                }
            },
        )
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
