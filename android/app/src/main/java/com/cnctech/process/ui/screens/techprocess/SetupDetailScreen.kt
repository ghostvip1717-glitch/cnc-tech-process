package com.cnctech.process.ui.screens.techprocess

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cnctech.process.CncApp
import com.cnctech.process.data.entity.CatalogItemEntity
import com.cnctech.process.data.entity.OperationEntity
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.PlateStockBadge
import com.cnctech.process.ui.components.PrimaryButton
import com.cnctech.process.ui.components.SkeletonStack
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
import com.cnctech.process.ui.theme.CncSkeleton
import com.cnctech.process.ui.theme.CncSurface
import java.io.File

@Composable
fun SetupDetailScreen(
    setupId: Long,
    onEdit: () -> Unit,
    onError: (String) -> Unit,
) {
    val tpRepo = CncApp.instance.techProcessRepository
    val catalogRepo = CncApp.instance.catalogRepository
    val detail by tpRepo.observeSetupDetail(setupId).collectAsState(initial = null)

    var itemsById by remember { mutableStateOf<Map<Long, CatalogItemEntity>>(emptyMap()) }
    var coverById by remember { mutableStateOf<Map<Long, String?>>(emptyMap()) }

    LaunchedEffect(detail) {
        val d = detail ?: return@LaunchedEffect
        val ids = buildSet {
            add(d.setup.jawId)
            d.operations.forEach { op ->
                add(op.toolId)
                add(op.plateId)
            }
        }
        val items = catalogRepo.getByIds(ids.toList())
        itemsById = items.associateBy { it.id }
        coverById = ids.associateWith { id ->
            catalogRepo.getPhotos(id).firstOrNull()?.filePath
        }
    }

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
        Spacer(modifier = Modifier.height(16.dp))

        val jaw = itemsById[detail!!.setup.jawId]
        Text(
            "Кулачки",
            color = CncOnSurfaceSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        CatalogEntityCard(
            name = jaw?.name ?: detail!!.jawName,
            note = jaw?.note,
            coverPath = coverById[detail!!.setup.jawId],
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Операции",
            color = CncOnSurfaceSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val ops = detail!!.operations
        if (ops.isEmpty()) {
            EmptyText("Нет операций")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ops.forEach { op ->
                    OperationCard(
                        operation = op,
                        tool = itemsById[op.toolId],
                        plate = itemsById[op.plateId],
                        toolCover = coverById[op.toolId],
                        plateCover = coverById[op.plateId],
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(text = "Изменить", onClick = onEdit)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CatalogEntityCard(
    name: String,
    note: String?,
    coverPath: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
            .background(CncSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverThumb(coverPath = coverPath)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = CncOnSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            note?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = CncOnSurfaceSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun OperationCard(
    operation: OperationEntity,
    tool: CatalogItemEntity?,
    plate: CatalogItemEntity?,
    toolCover: String?,
    plateCover: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
            .background(CncSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "${operation.opNumber}  ${operation.title}",
            color = CncOnSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        CatalogLine(
            label = "Инструмент",
            name = tool?.name ?: "—",
            coverPath = toolCover,
            trailing = null,
        )
        CatalogLine(
            label = "Пластина",
            name = plate?.name ?: "—",
            coverPath = plateCover,
            trailing = {
                if (plate != null) {
                    PlateStockBadge(
                        stockQty = plate.stockQty,
                        minStockThreshold = plate.minStockThreshold,
                    )
                }
            },
        )
        operation.comment?.takeIf { it.isNotBlank() }?.let { comment ->
            Text(comment, color = CncOnSurfaceSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CatalogLine(
    label: String,
    name: String,
    coverPath: String?,
    trailing: (@Composable () -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = CncOnSurfaceSecondary, fontSize = 12.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CoverThumb(coverPath = coverPath, size = 56.dp)
            Text(
                name,
                color = CncOnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
    }
}

@Composable
private fun CoverThumb(
    coverPath: String?,
    size: Dp = 56.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(CncSkeleton),
    ) {
        coverPath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
