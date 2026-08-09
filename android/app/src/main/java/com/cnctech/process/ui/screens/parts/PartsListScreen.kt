package com.cnctech.process.ui.screens.parts

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cnctech.process.CncApp
import com.cnctech.process.data.entity.PartEntity
import com.cnctech.process.data.prefs.LayoutMode
import com.cnctech.process.data.repository.AppResult
import com.cnctech.process.ui.components.CncBottomSheet
import com.cnctech.process.ui.components.CncFab
import com.cnctech.process.ui.components.CncTextField
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.PrimaryButton
import com.cnctech.process.ui.components.SkeletonStack
import com.cnctech.process.ui.theme.CncBadgeProgramsBg
import com.cnctech.process.ui.theme.CncBadgeProgramsText
import com.cnctech.process.ui.theme.CncBadgeTimeBg
import com.cnctech.process.ui.theme.CncBadgeTimeText
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
import com.cnctech.process.ui.theme.CncSkeleton
import com.cnctech.process.ui.theme.CncSurface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun PartsListScreen(
    layoutMode: LayoutMode,
    onOpenPart: (Long) -> Unit,
    onCreated: (Long) -> Unit,
    onError: (String) -> Unit,
) {
    val repo = CncApp.instance.partRepository
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    val queryFlow = remember { MutableStateFlow("") }
    var loading by remember { mutableStateOf(true) }
    val parts by remember {
        queryFlow.flatMapLatest { q -> repo.observeParts(q) }
    }.collectAsState(initial = emptyList())

    androidx.compose.runtime.LaunchedEffect(search) {
        loading = true
        delay(350)
        queryFlow.value = search.trim()
        loading = false
    }

    var sheetOpen by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск по номеру или названию") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = com.cnctech.process.ui.components.fieldColors(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (loading && parts.isEmpty()) {
                SkeletonStack()
            } else if (parts.isEmpty()) {
                EmptyText("Детали не найдены")
            } else {
                when (layoutMode) {
                    LayoutMode.List -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(parts, key = { it.part.id }) { item ->
                                PartListCard(
                                    part = item.part,
                                    coverPath = item.photos.firstOrNull()?.filePath,
                                    photoCount = item.photos.size,
                                    onClick = { onOpenPart(item.part.id) },
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
                            gridItems(parts, key = { it.part.id }) { item ->
                                PartGridCell(
                                    part = item.part,
                                    coverPath = item.photos.firstOrNull()?.filePath,
                                    photoCount = item.photos.size,
                                    onClick = { onOpenPart(item.part.id) },
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
                number = ""
                title = ""
                sheetOpen = true
            }, contentDescription = "Новая деталь")
        }
    }

    CncBottomSheet(open = sheetOpen, title = "Новая деталь", onDismiss = { if (!busy) sheetOpen = false }) {
        CncTextField(value = number, onValueChange = { number = it }, label = "Номер")
        Spacer(modifier = Modifier.height(12.dp))
        CncTextField(value = title, onValueChange = { title = it }, label = "Название")
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(
            text = "Создать",
            busy = busy,
            onClick = {
                scope.launch {
                    busy = true
                    when (val r = repo.create(number, title)) {
                        is AppResult.Ok -> {
                            sheetOpen = false
                            onCreated(r.value)
                        }
                        is AppResult.Err -> onError(r.message)
                    }
                    busy = false
                }
            },
        )
    }
}

@Composable
private fun PartListCard(
    part: PartEntity,
    coverPath: String?,
    photoCount: Int,
    onClick: () -> Unit,
) {
    val minutes = part.machiningTimeMinutes
    val programs = part.programCount
    val hasBadges = minutes != null || programs != null

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
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CncSkeleton),
        ) {
            if (coverPath != null) {
                AsyncImage(
                    model = File(coverPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${part.number} — ${part.title}",
                color = CncOnSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            if (hasBadges) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (minutes != null) {
                        PartBadge(
                            text = "$minutes мин",
                            background = CncBadgeTimeBg,
                            textColor = CncBadgeTimeText,
                        )
                    }
                    if (programs != null) {
                        PartBadge(
                            text = "$programs прог.",
                            background = CncBadgeProgramsBg,
                            textColor = CncBadgeProgramsText,
                        )
                    }
                }
            } else {
                Text(
                    "Фото: $photoCount",
                    color = CncOnSurfaceSecondary,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun PartBadge(
    text: String,
    background: Color,
    textColor: Color,
) {
    Text(
        text = text,
        color = textColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}


@Composable
private fun PartGridCell(
    part: PartEntity,
    coverPath: String?,
    photoCount: Int,
    onClick: () -> Unit,
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
                .clickable(onClick = onClick),
        ) {
            if (coverPath != null) {
                AsyncImage(
                    model = File(coverPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Text(
                text = part.number,
                color = CncOnSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(CncSurface.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    } else {
        val minutes = part.machiningTimeMinutes
        val programs = part.programCount
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
                .background(CncSurface)
                .clickable(onClick = onClick),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(CncSkeleton),
            ) {
                if (coverPath != null) {
                    AsyncImage(
                        model = File(coverPath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "${part.number} — ${part.title}",
                    color = CncOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                )
                if (minutes != null || programs != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (minutes != null) {
                            PartBadge(
                                text = "$minutes мин",
                                background = CncBadgeTimeBg,
                                textColor = CncBadgeTimeText,
                            )
                        }
                        if (programs != null) {
                            PartBadge(
                                text = "$programs прог.",
                                background = CncBadgeProgramsBg,
                                textColor = CncBadgeProgramsText,
                            )
                        }
                    }
                } else if (photoCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Фото: $photoCount",
                        color = CncOnSurfaceSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
