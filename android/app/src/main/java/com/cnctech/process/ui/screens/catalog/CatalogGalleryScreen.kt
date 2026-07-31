package com.cnctech.process.ui.screens.catalog

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cnctech.process.CncApp
import com.cnctech.process.data.entity.CatalogItemPhotoEntity
import com.cnctech.process.data.photo.CaptureTarget
import com.cnctech.process.data.repository.AppResult
import com.cnctech.process.ui.components.ConfirmDialog
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.PrimaryButton
import com.cnctech.process.ui.components.ReorderableColumn
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncDanger
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncSurface
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CatalogGalleryScreen(
    catalogItemId: Long,
    title: String,
    onError: (String) -> Unit,
    onOpenPhoto: (Int) -> Unit,
) {
    val context = LocalContext.current
    val repo = CncApp.instance.catalogRepository
    val photoStorage = CncApp.instance.photoStorage
    val photos by repo.observePhotos(catalogItemId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CatalogItemPhotoEntity?>(null) }
    var cameraCapture by remember { mutableStateOf<CaptureTarget?>(null) }

    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            when (val r = repo.addPhoto(catalogItemId, uri)) {
                is AppResult.Ok -> Unit
                is AppResult.Err -> onError(r.message)
            }
            busy = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved: Boolean ->
        val capture = cameraCapture
        cameraCapture = null
        if (saved && capture != null) {
            scope.launch {
                busy = true
                when (val r = repo.addPhoto(catalogItemId, capture.uri)) {
                    is AppResult.Ok -> Unit
                    is AppResult.Err -> onError(r.message)
                }
                photoStorage.deleteFile(capture.file)
                busy = false
            }
        } else {
            capture?.file?.let { photoStorage.deleteFile(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(title, color = CncOnSurface)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (hasCamera) {
                PrimaryButton(
                    text = "Сделать фото",
                    busy = busy,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val capture = photoStorage.createCaptureUri(context)
                        cameraCapture = capture
                        cameraLauncher.launch(capture.uri)
                    },
                )
            }
            PrimaryButton(
                text = "Загрузить фото",
                busy = busy,
                modifier = if (hasCamera) Modifier.weight(1f) else Modifier,
                onClick = { picker.launch("image/*") },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (photos.isEmpty()) {
            EmptyText("Нет фото")
        } else {
            ReorderableColumn(
                items = photos,
                keyOf = { it.id },
                onReorder = { ids ->
                    scope.launch {
                        when (val r = repo.reorderPhotos(catalogItemId, ids)) {
                            is AppResult.Ok -> Unit
                            is AppResult.Err -> onError(r.message)
                        }
                    }
                },
            ) { photo, handle ->
                val index = photos.indexOfFirst { it.id == photo.id }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
                        .background(CncSurface)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    handle()
                    AsyncImage(
                        model = File(photo.filePath),
                        contentDescription = null,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = index >= 0) { onOpenPhoto(index) },
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { pendingDelete = photo }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = CncDanger)
                    }
                }
            }
        }
    }

    ConfirmDialog(
        open = pendingDelete != null,
        title = "Удалить фото?",
        message = "Удалить это фото? Действие нельзя отменить",
        busy = busy,
        onCancel = { pendingDelete = null },
        onConfirm = {
            val photo = pendingDelete ?: return@ConfirmDialog
            scope.launch {
                busy = true
                when (val r = repo.deletePhoto(photo.id)) {
                    is AppResult.Ok -> pendingDelete = null
                    is AppResult.Err -> onError(r.message)
                }
                busy = false
            }
        },
    )
}
