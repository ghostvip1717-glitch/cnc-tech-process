package com.cnctech.process.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Vertical list with drag-handle reorder. Calls [onReorder] with new id order when drop completes.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    keyOf: (T) -> Long,
    onReorder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, dragHandle: @Composable () -> Unit) -> Unit,
) {
    var order by remember(items) { mutableStateOf(items.map(keyOf)) }
    val byId = remember(items) { items.associateBy(keyOf) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        order.forEachIndexed { index, id ->
            val item = byId[id] ?: return@forEachIndexed
            val isDragging = draggingId == id
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset {
                        IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0)
                    }
                    .onGloballyPositioned { coords ->
                        if (itemHeightPx == 0) itemHeightPx = coords.size.height
                    },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val handle: @Composable () -> Unit = {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .pointerInput(id, order, itemHeightPx) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingId = id
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            draggingId = null
                                            dragOffsetY = 0f
                                            onReorder(order)
                                        },
                                        onDragCancel = {
                                            draggingId = null
                                            dragOffsetY = 0f
                                            order = items.map(keyOf)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val height = if (itemHeightPx > 0) {
                                                itemHeightPx.toFloat()
                                            } else {
                                                with(density) { 80.dp.toPx() }
                                            }
                                            val from = order.indexOf(id)
                                            if (from < 0) return@detectDragGesturesAfterLongPress
                                            val shift = (dragOffsetY / height).roundToInt()
                                            val to = (from + shift).coerceIn(0, order.lastIndex)
                                            if (to != from) {
                                                val mutable = order.toMutableList()
                                                mutable.removeAt(from)
                                                mutable.add(to, id)
                                                order = mutable
                                                dragOffsetY -= (to - from) * height
                                            }
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            DragHandleIcon()
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        itemContent(item, handle)
                    }
                }
            }
        }
    }
}
