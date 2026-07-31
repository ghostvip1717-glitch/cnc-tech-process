package com.cnctech.process.ui.screens.techprocess

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cnctech.process.CncApp
import com.cnctech.process.data.entity.CatalogItemEntity
import com.cnctech.process.data.entity.CatalogType
import com.cnctech.process.data.repository.AppResult
import com.cnctech.process.ui.components.CncBottomSheet
import com.cnctech.process.ui.components.CncFab
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.PrimaryButton
import com.cnctech.process.ui.components.SkeletonStack
import com.cnctech.process.ui.components.fieldColors
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
import com.cnctech.process.ui.theme.CncSurface
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechProcessScreen(
    partId: Long,
    onOpenSetup: (Long) -> Unit,
    onError: (String) -> Unit,
) {
    val tpRepo = CncApp.instance.techProcessRepository
    val catalogRepo = CncApp.instance.catalogRepository
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(partId) {
        when (val r = tpRepo.ensureTechProcess(partId)) {
            is AppResult.Ok -> ready = true
            is AppResult.Err -> onError(r.message)
        }
    }

    val setups by tpRepo.observeSetupSummaries(partId).collectAsState(initial = emptyList())
    var jaws by remember { mutableStateOf<List<CatalogItemEntity>>(emptyList()) }
    LaunchedEffect(Unit) { jaws = catalogRepo.listByType(CatalogType.jaw) }

    var sheetOpen by remember { mutableStateOf(false) }
    var selectedJawId by remember { mutableStateOf<Long?>(null) }
    var jawExpanded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (!ready) {
                SkeletonStack()
            } else if (setups.isEmpty()) {
                EmptyText("Нет установов — нажмите +")
            } else {
                LazyColumn {
                    items(setups, key = { it.setup.id }) { summary ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
                                .background(CncSurface)
                                .clickable { onOpenSetup(summary.setup.id) }
                                .padding(14.dp),
                        ) {
                            Text(
                                "Установ ${summary.label}",
                                color = CncOnSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Кулачки ${summary.jawName} · ${summary.operationCount} операции",
                                color = CncOnSurfaceSecondary,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            CncFab(onClick = {
                selectedJawId = jaws.firstOrNull()?.id
                sheetOpen = true
            })
        }
    }

    CncBottomSheet(open = sheetOpen, title = "Новый установ", onDismiss = { if (!busy) sheetOpen = false }) {
        if (jaws.isEmpty()) {
            EmptyText("Сначала добавьте кулачки в справочник")
        } else {
            val selected = jaws.find { it.id == selectedJawId }
            ExposedDropdownMenuBox(expanded = jawExpanded, onExpandedChange = { jawExpanded = it }) {
                OutlinedTextField(
                    value = selected?.name.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Кулачки") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jawExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = jawExpanded, onDismissRequest = { jawExpanded = false }) {
                    jaws.forEach { jaw ->
                        DropdownMenuItem(
                            text = { Text(jaw.name, color = CncOnSurface) },
                            onClick = {
                                selectedJawId = jaw.id
                                jawExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = "Создать",
                busy = busy,
                enabled = selectedJawId != null,
                onClick = {
                    val jawId = selectedJawId ?: return@PrimaryButton
                    scope.launch {
                        busy = true
                        when (val r = tpRepo.addSetup(partId, jawId)) {
                            is AppResult.Ok -> sheetOpen = false
                            is AppResult.Err -> onError(r.message)
                        }
                        busy = false
                    }
                },
            )
        }
    }
}
