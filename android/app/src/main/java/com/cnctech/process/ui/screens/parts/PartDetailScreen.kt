package com.cnctech.process.ui.screens.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cnctech.process.CncApp
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.SkeletonStack
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncIconTintBg
import com.cnctech.process.ui.theme.CncMuted
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
import com.cnctech.process.ui.theme.CncPrimary
import com.cnctech.process.ui.theme.CncSurface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PartDetailScreen(
    partId: Long,
    onEdit: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenTechProcess: () -> Unit,
    onOpenAssembly: () -> Unit,
) {
    val data by CncApp.instance.partRepository.observePart(partId).collectAsState(initial = null)
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
        val cover = data!!.photos.firstOrNull()

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, CncBorder, RoundedCornerShape(14.dp))
                .background(com.cnctech.process.ui.theme.CncHubGradientTop)
                .clickable(onClick = onOpenGallery),
        ) {
            if (cover != null) {
                AsyncImage(
                    model = File(cover.filePath),
                    contentDescription = "Фото",
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = CncMuted, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Добавить фото", color = CncMuted)
                }
            }
        }

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
