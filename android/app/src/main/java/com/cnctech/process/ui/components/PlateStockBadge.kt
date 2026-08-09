package com.cnctech.process.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cnctech.process.ui.theme.CncDanger
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
import com.cnctech.process.ui.theme.CncSkeleton

@Composable
fun PlateStockBadge(
    stockQty: Int?,
    minStockThreshold: Int,
) {
    if (stockQty == null) return
    val low = stockQty < minStockThreshold
    val bg = if (low) CncDanger.copy(alpha = 0.14f) else CncSkeleton
    val fg = if (low) CncDanger else CncOnSurfaceSecondary
    val label = if (low) "Осталось: $stockQty шт" else "$stockQty шт"
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (low) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = CncDanger,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
