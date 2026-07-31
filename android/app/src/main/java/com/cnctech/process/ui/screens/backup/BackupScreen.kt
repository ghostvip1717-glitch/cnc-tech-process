package com.cnctech.process.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cnctech.process.CncApp
import com.cnctech.process.ui.components.ConfirmDialog
import com.cnctech.process.ui.components.DangerButton
import com.cnctech.process.ui.components.PrimaryButton
import com.cnctech.process.ui.theme.CncMuted
import com.cnctech.process.ui.theme.CncOnSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupScreen(
    onError: (String) -> Unit,
    onInfo: (String) -> Unit,
) {
    val backup = CncApp.instance.backupManager
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            runCatching {
                withContext(Dispatchers.IO) { backup.exportToUri(uri) }
                CncApp.instance.rebuildGraph()
            }.onSuccess {
                onInfo("Экспорт завершён")
            }.onFailure {
                onError(it.message ?: "Ошибка экспорта")
                CncApp.instance.rebuildGraph()
            }
            busy = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImportUri = uri
        confirmImport = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            "Все данные хранятся только на этом телефоне. Сделайте резервную копию, чтобы не потерять техпроцессы и фото.",
            color = CncMuted,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text("Экспортировать данные", color = CncOnSurface, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Собирает базу и все фото в один ZIP и сохраняет через системный диалог.",
            color = CncMuted,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PrimaryButton(
            text = "Экспортировать данные",
            busy = busy,
            onClick = { exportLauncher.launch(backup.suggestedFileName()) },
        )
        Spacer(modifier = Modifier.height(28.dp))
        Text("Импортировать данные", color = CncOnSurface, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Полностью заменяет текущую базу и фото данными из ZIP. Слияние не выполняется.",
            color = CncMuted,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        DangerButton(
            text = "Импортировать данные",
            enabled = !busy,
            onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) },
        )
    }

    ConfirmDialog(
        open = confirmImport,
        title = "Заменить все данные?",
        message = "Текущая база и все фото будут полностью заменены данными из архива. Действие нельзя отменить.",
        confirmLabel = "Импортировать",
        busy = busy,
        onCancel = {
            confirmImport = false
            pendingImportUri = null
        },
        onConfirm = {
            val uri = pendingImportUri ?: return@ConfirmDialog
            scope.launch {
                busy = true
                runCatching {
                    withContext(Dispatchers.IO) { backup.importFromUri(uri) }
                    CncApp.instance.rebuildGraph()
                }.onSuccess {
                    confirmImport = false
                    pendingImportUri = null
                    onInfo("Импорт завершён")
                }.onFailure {
                    onError(it.message ?: "Ошибка импорта")
                    CncApp.instance.rebuildGraph()
                }
                busy = false
            }
        },
    )
}
