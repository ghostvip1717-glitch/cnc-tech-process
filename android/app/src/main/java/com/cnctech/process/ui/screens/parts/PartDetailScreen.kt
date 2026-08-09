package com.cnctech.process.ui.screens.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cnctech.process.CncApp
import com.cnctech.process.data.repository.AppResult
import com.cnctech.process.ui.components.PhotoStrip
import com.cnctech.process.ui.components.PhotoStripItem
import com.cnctech.process.ui.components.SkeletonStack
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncIconTintBg
import com.cnctech.process.ui.theme.CncMuted
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
import com.cnctech.process.ui.theme.CncPrimary
import com.cnctech.process.ui.theme.CncSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun PartDetailScreen(
    partId: Long,
    onEdit: () -> Unit,
    onOpenPhoto: (Int) -> Unit,
    onOpenTechProcess: () -> Unit,
    onOpenAssembly: () -> Unit,
    onError: (String) -> Unit,
) {
    val repo = CncApp.instance.partRepository
    val data by repo.observePart(partId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val loading = data == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (loading) {
            SkeletonStack(rows = 3, height = 80.dp)
            return
        }
        val part = data!!.part
        val photos = data!!.photos.map { PhotoStripItem(id = it.id, filePath = it.filePath) }

        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "${part.number} — ${part.title}",
                modifier = Modifier.weight(1f),
                color = CncOnSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Изменить деталь", tint = CncPrimary)
            }
        }
        Text(
            text = "Создана: ${formatDate(part.createdAt)}",
            color = CncOnSurfaceSecondary,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))

        part.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text(note, color = CncOnSurfaceSecondary, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Фото", color = CncOnSurface, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        PhotoStrip(
            photos = photos,
            onAdd = { uri ->
                scope.launch {
                    when (val result = repo.addPhoto(partId, uri)) {
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
                    when (val result = repo.reorderPhotos(partId, ids)) {
                        is AppResult.Ok -> Unit
                        is AppResult.Err -> onError(result.message)
                    }
                }
            },
            onOpenViewer = onOpenPhoto,
        )

        Spacer(modifier = Modifier.height(16.dp))
        MenuItem(icon = Icons.Default.AccountTree, label = "Техпроцесс", onClick = onOpenTechProcess)
        Spacer(modifier = Modifier.height(8.dp))
        MenuItem(icon = Icons.Default.Handyman, label = "Сборка", onClick = onOpenAssembly)
    }
}

@Composable
private fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
            .background(CncSurface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(CncIconTintBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = CncPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), color = CncOnSurface, fontWeight = FontWeight.SemiBold)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = CncMuted)
    }
}

private fun formatDate(ms: Long): String =
    SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale("ru")).format(Date(ms))
