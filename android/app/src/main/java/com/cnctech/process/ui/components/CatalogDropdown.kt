package com.cnctech.process.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.cnctech.process.CncApp
import com.cnctech.process.data.entity.CatalogItemEntity
import com.cnctech.process.data.entity.CatalogType
import com.cnctech.process.data.repository.AppResult
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
import com.cnctech.process.ui.theme.CncPrimary
import com.cnctech.process.ui.theme.CncSurface
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogDropdown(
    label: String,
    type: CatalogType,
    items: List<CatalogItemEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onError: (String) -> Unit,
    /** If non-null and type==plate, newly created plate is setCompatible(toolId, newId, true) */
    autoLinkToolId: Long? = null,
    coverPath: String? = null,
    onOpenCover: (() -> Unit)? = null,
) {
    val repo = CncApp.instance.catalogRepository
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var createOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var createdItems by remember { mutableStateOf<List<CatalogItemEntity>>(emptyList()) }
    val allItems = remember(items, createdItems) {
        (items + createdItems).distinctBy { it.id }
    }
    val selected = allItems.find { it.id == selectedId }

    Column {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selected?.name.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = fieldColors(),
                shape = RoundedCornerShape(8.dp),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                allItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.name, color = CncOnSurface) },
                        onClick = {
                            onSelect(item.id)
                            expanded = false
                        },
                    )
                }
                if (allItems.isNotEmpty()) {
                    HorizontalDivider(color = CncBorder)
                }
                DropdownMenuItem(
                    text = { Text("+ Добавить новый", color = CncPrimary) },
                    onClick = {
                        expanded = false
                        name = ""
                        note = ""
                        createOpen = true
                    },
                )
            }
        }

        if (coverPath != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = File(coverPath),
                contentDescription = "Фото",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onOpenCover != null) { onOpenCover?.invoke() },
            )
        }
    }

    if (createOpen) {
        Dialog(onDismissRequest = { if (!busy) createOpen = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(CncSurface)
                    .padding(20.dp),
            ) {
                Text("Новая позиция", color = CncOnSurface)
                Spacer(modifier = Modifier.height(12.dp))
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
                    text = "Создать",
                    busy = busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            when (val result = repo.create(type, name, note)) {
                                is AppResult.Err -> onError(result.message)
                                is AppResult.Ok -> {
                                    val id = result.value
                                    if (autoLinkToolId != null && type == CatalogType.plate) {
                                        runCatching {
                                            repo.setCompatible(autoLinkToolId, id, true)
                                        }.onFailure {
                                            onError("Не удалось связать пластину с инструментом")
                                        }
                                    }
                                    repo.getById(id)?.let { created ->
                                        createdItems = createdItems + created
                                    }
                                    onSelect(id)
                                    createOpen = false
                                }
                            }
                            busy = false
                        }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Можно оставить примечание пустым",
                    color = CncOnSurfaceSecondary,
                )
            }
        }
    }
}
