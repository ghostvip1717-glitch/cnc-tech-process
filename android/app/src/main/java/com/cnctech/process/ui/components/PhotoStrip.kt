package com.cnctech.process.ui.components

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.cnctech.process.CncApp
import com.cnctech.process.data.photo.CaptureTarget
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncMuted
import com.cnctech.process.ui.theme.CncPrimary
import com.cnctech.process.ui.theme.CncSkeleton
import com.cnctech.process.ui.theme.CncSurface
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PhotoStripItem(val id: Long, val filePath: String)

@Composable
fun PhotoStrip(
    photos: List<PhotoStripItem>,
    onAdd: (Uri) -> Unit,
    onDelete: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onOpenViewer: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val photoStorage = CncApp.instance.photoStorage
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    var chooserOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PhotoStripItem?>(null) }
    var cameraCapture by remember { mutableStateOf<CaptureTarget?>(null) }

    var order by remember(photos) { mutableStateOf(photos.map { it.id }) }
    val byId = remember(photos) { photos.associateBy { it.id } }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var itemWidthPx by remember { mutableIntStateOf(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let(onAdd)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val capture = cameraCapture
        cameraCapture = null
        if (saved && capture != null) {
            onAdd(capture.uri)
            scope.launch {
                delay(10_000)
                photoStorage.deleteFile(capture.file)
            }
        } else {
            capture?.file?.let(photoStorage::deleteFile)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        order.forEach { id ->
            val photo = byId[id] ?: return@forEach
            val index = photos.indexOfFirst { it.id == id }
            val isDragging = draggingId == id
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset {
                        IntOffset(if (isDragging) dragOffsetX.roundToInt() else 0, 0)
                    }
                    .onGloballyPositioned { coords ->
                        if (itemWidthPx == 0) itemWidthPx = coords.size.width
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AsyncImage(
                    model = File(photo.filePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CncSkeleton)
                        .pointerInput(photo.id, index) {
                            detectTapGestures(
                                onTap = {
                                    if (index >= 0) onOpenViewer(index)
                                },
                                onLongPress = {
                                    pendingDelete = photo
                                },
                            )
                        },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .pointerInput(id, order, itemWidthPx) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = id
                                    dragOffsetX = 0f
                                },
                                onDragEnd = {
                                    draggingId = null
                                    dragOffsetX = 0f
                                    onReorder(order)
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffsetX = 0f
                                    order = photos.map { it.id }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                    val width = (
                                        if (itemWidthPx > 0) itemWidthPx.toFloat() else with(density) { 72.dp.toPx() }
                                        ) + with(density) { 10.dp.toPx() }
                                    val from = order.indexOf(id)
                                    if (from < 0) return@detectDragGesturesAfterLongPress
                                    val shift = (dragOffsetX / width).roundToInt()
                                    val to = (from + shift).coerceIn(0, order.lastIndex)
                                    if (to != from) {
                                        val next = order.toMutableList()
                                        next.removeAt(from)
                                        next.add(to, id)
                                        order = next
                                        dragOffsetX -= (to - from) * width
                                    }
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    DragHandleIcon()
                }
            }
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, CncBorder, RoundedCornerShape(8.dp))
                .background(CncSurface)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { chooserOpen = true })
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Add, contentDescription = "Добавить фото", tint = CncPrimary)
                Text("+", color = CncMuted)
            }
        }
    }

    CncBottomSheet(
        open = chooserOpen,
        title = "Добавить фото",
        onDismiss = { chooserOpen = false },
    ) {
        if (hasCamera) {
            PrimaryButton(
                text = "Сделать фото",
                onClick = {
                    chooserOpen = false
                    val capture = photoStorage.createCaptureUri(context)
                    cameraCapture = capture
                    cameraLauncher.launch(capture.uri)
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        PrimaryButton(
            text = "Выбрать из галереи",
            onClick = {
                chooserOpen = false
                picker.launch("image/*")
            },
        )
    }

    ConfirmDialog(
        open = pendingDelete != null,
        title = "Удалить фото?",
        message = "Удалить это фото? Действие нельзя отменить",
        onCancel = { pendingDelete = null },
        onConfirm = {
            val id = pendingDelete?.id ?: return@ConfirmDialog
            pendingDelete = null
            onDelete(id)
        },
    )
}
