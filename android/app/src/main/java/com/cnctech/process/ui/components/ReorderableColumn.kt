package com.cnctech.process.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
    val listState = rememberLazyListState()
    var order by remember(items) { mutableStateOf(items.map(keyOf)) }
    val byId = remember(items) { items.associateBy(keyOf) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(order, key = { it }) { id ->
            val item = byId[id] ?: return@items
            val isDragging = draggingId == id
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset {
                        IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0)
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
                                .pointerInput(id) {
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
                                            val draggedInfo = listState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { it.key == id }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val fingerY =
                                                draggedInfo.offset + draggedInfo.size / 2f + dragOffsetY
                                            val target = listState.layoutInfo.visibleItemsInfo
                                                .firstOrNull { info ->
                                                    info.key != id &&
                                                        info.key is Long &&
                                                        fingerY >= info.offset &&
                                                        fingerY < info.offset + info.size
                                                }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val toKey = target.key as Long
                                            val from = order.indexOf(id)
                                            val to = order.indexOf(toKey)
                                            if (from >= 0 && to >= 0 && from != to) {
                                                val mutable = order.toMutableList()
                                                mutable.removeAt(from)
                                                mutable.add(to, id)
                                                order = mutable
                                                dragOffsetY = 0f
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
