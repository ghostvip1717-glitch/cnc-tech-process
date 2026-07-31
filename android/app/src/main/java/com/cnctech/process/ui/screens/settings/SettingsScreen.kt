package com.cnctech.process.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cnctech.process.CncApp
import com.cnctech.process.ui.components.ConfirmDialog
import com.cnctech.process.ui.components.DangerButton
import com.cnctech.process.ui.screens.backup.BackupScreen
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncMuted
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncPrimary
import com.cnctech.process.ui.theme.ThemeVariant
import com.cnctech.process.ui.theme.displayName
import com.cnctech.process.ui.theme.paletteFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onError: (String) -> Unit,
    onInfo: (String) -> Unit,
) {
    val app = CncApp.instance
    val selected = app.themeVariant
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var confirmMigration by remember { mutableStateOf(false) }
    var pendingMigrationUri by remember { mutableStateOf<Uri?>(null) }

    val migrationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingMigrationUri = uri
        confirmMigration = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = "Стиль приложения",
            color = CncOnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        ThemeSwatchGrid(
            selected = selected,
            onSelect = { app.applyThemeVariant(it) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        BackupScreen(onError = onError, onInfo = onInfo)
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Перенос из Telegram-версии", color = CncOnSurface, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Импорт migration.zip, полученного скриптом export_from_sheets.py. Текущие данные будут полностью заменены.",
                color = CncMuted,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            DangerButton(
                text = "Импортировать из старой версии",
                enabled = !busy,
                onClick = { migrationLauncher.launch(arrayOf("application/zip", "*/*")) },
            )
        }
    }

    ConfirmDialog(
        open = confirmMigration,
        title = "Заменить все данные?",
        message = "Текущая база и все фото будут полностью заменены данными из migration.zip. Действие нельзя отменить.",
        confirmLabel = "Импортировать",
        busy = busy,
        onCancel = {
            confirmMigration = false
            pendingMigrationUri = null
        },
        onConfirm = {
            val uri = pendingMigrationUri ?: return@ConfirmDialog
            scope.launch {
                busy = true
                runCatching {
                    val summary = withContext(Dispatchers.IO) {
                        app.migrationImporter.importFromZip(uri)
                    }
                    app.rebuildGraph()
                    summary
                }.onSuccess { summary ->
                    confirmMigration = false
                    pendingMigrationUri = null
                    onInfo(summary)
                }.onFailure {
                    onError(it.message ?: "Ошибка импорта миграции")
                    app.rebuildGraph()
                }
                busy = false
            }
        },
    )
}

@Composable
private fun ThemeSwatchGrid(
    selected: ThemeVariant,
    onSelect: (ThemeVariant) -> Unit,
) {
    val variants = ThemeVariant.entries
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        variants.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { variant ->
                    ThemeSwatch(
                        variant = variant,
                        selected = variant == selected,
                        onClick = { onSelect(variant) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(
    variant: ThemeVariant,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = paletteFor(variant)
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) CncPrimary else CncBorder,
                    shape = RoundedCornerShape(14.dp),
                )
                .background(
                    Brush.verticalGradient(listOf(palette.background, palette.primary)),
                ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = variant.displayName(),
            color = CncOnSurface,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp,
        )
    }
}
