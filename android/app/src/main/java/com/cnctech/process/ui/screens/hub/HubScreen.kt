package com.cnctech.process.ui.screens.hub

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncHubGradientTop
import com.cnctech.process.ui.theme.CncIconTintBg
import com.cnctech.process.ui.theme.CncMuted
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncPrimary
import com.cnctech.process.ui.theme.CncSurface

@Composable
fun HubScreen(
    onOpenParts: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Выберите раздел", color = CncMuted, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(16.dp))
        HubTile(
            icon = Icons.Default.PrecisionManufacturing,
            title = "Детали",
            subtitle = "Карточки, техпроцесс, сборка",
            onClick = onOpenParts,
        )
        Spacer(modifier = Modifier.height(14.dp))
        HubTile(
            icon = Icons.Default.Build,
            title = "Инструмент",
            subtitle = "Справочник, пластины, кулачки",
            onClick = onOpenCatalog,
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onOpenBackup) {
            Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = CncPrimary)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Резервная копия", color = CncPrimary)
        }
    }
}

@Composable
private fun HubTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, CncBorder, RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(CncHubGradientTop, CncSurface)))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CncIconTintBg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = CncPrimary, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(title, color = CncOnSurface, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, color = CncMuted, fontSize = 14.sp)
    }
}
