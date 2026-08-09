package com.cnctech.process.ui.screens.assembly

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Square
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cnctech.process.CncApp
import com.cnctech.process.data.repository.RequiredItem
import com.cnctech.process.data.repository.RequiredItems
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.SkeletonStack
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncMuted
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncPrimary
import com.cnctech.process.ui.theme.CncSurface

@Composable
fun AssemblyScreen(
    partId: Long,
    onError: (String) -> Unit,
) {
    var items by remember { mutableStateOf<RequiredItems?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(partId) {
        loading = true
        runCatching {
            CncApp.instance.techProcessRepository.getRequiredItems(partId)
        }.onSuccess {
            items = it
        }.onFailure {
            onError(it.message ?: "Не удалось загрузить сводку")
        }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (loading) {
            SkeletonStack(rows = 3)
            return
        }
        Text("Нужно для изготовления", color = CncMuted, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(12.dp))
        val data = items
        if (data == null || (data.tools.isEmpty() && data.plates.isEmpty() && data.jaws.isEmpty())) {
            EmptyText("Нет данных — добавьте установы и операции")
        } else {
            if (data.tools.isNotEmpty()) {
                Group(title = "Инструмент", icon = Icons.Default.Build, list = data.tools)
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (data.plates.isNotEmpty()) {
                Group(title = "Пластины", icon = Icons.Default.Square, list = data.plates)
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (data.jaws.isNotEmpty()) {
                Group(title = "Кулачки", icon = Icons.Default.Hardware, list = data.jaws)
            }
        }
    }
}

@Composable
private fun Group(title: String, icon: ImageVector, list: List<RequiredItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(12.dp))
            .background(CncSurface)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = CncPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, color = CncOnSurface, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        list.forEach { item ->
            Text("• ${item.name}", color = CncOnSurface, modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}
