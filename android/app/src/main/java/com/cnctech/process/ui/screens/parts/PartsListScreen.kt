package com.cnctech.process.ui.screens.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cnctech.process.CncApp
import com.cnctech.process.data.repository.AppResult
import com.cnctech.process.ui.components.CncBottomSheet
import com.cnctech.process.ui.components.CncFab
import com.cnctech.process.ui.components.CncTextField
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.PrimaryButton
import com.cnctech.process.ui.components.SkeletonStack
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
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
    onOpenPart: (Long) -> Unit,
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

    // debounce search
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
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(parts, key = { it.part.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
                                .background(CncSurface)
                                .clickable { onOpenPart(item.part.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val cover = item.photos.firstOrNull()?.filePath
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.cnctech.process.ui.theme.CncSkeleton),
                            ) {
                                if (cover != null) {
                                    AsyncImage(
                                        model = File(cover),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${item.part.number} — ${item.part.title}",
                                    color = CncOnSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                )
                                Text(
                                    "Фото: ${item.photos.size}",
                                    color = CncOnSurfaceSecondary,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
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
                        is AppResult.Ok -> sheetOpen = false
                        is AppResult.Err -> onError(r.message)
                    }
                    busy = false
                }
            },
        )
    }
}
