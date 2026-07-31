package com.cnctech.process.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cnctech.process.ui.theme.CncBorder
import com.cnctech.process.ui.theme.CncDanger
import com.cnctech.process.ui.theme.CncMuted
import com.cnctech.process.ui.theme.CncOnPrimary
import com.cnctech.process.ui.theme.CncOnSurface
import com.cnctech.process.ui.theme.CncOnSurfaceSecondary
import com.cnctech.process.ui.theme.CncPrimary
import com.cnctech.process.ui.theme.CncSkeleton
import com.cnctech.process.ui.theme.CncSurface
import kotlinx.coroutines.delay

@Composable
fun AppHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CncSurface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = CncPrimary,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = CncOnSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        actions()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CncBorder),
    )
}

@Composable
fun CncFab(onClick: () -> Unit, contentDescription: String = "Добавить") {
    FloatingActionButton(
        onClick = onClick,
        containerColor = CncPrimary,
        contentColor = CncOnPrimary,
        shape = CircleShape,
    ) {
        Icon(Icons.Default.Add, contentDescription = contentDescription)
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = CncOnSurface,
    unfocusedTextColor = CncOnSurface,
    focusedBorderColor = CncPrimary,
    unfocusedBorderColor = Color(0xFFCCCCCC),
    focusedLabelColor = CncPrimary,
    unfocusedLabelColor = CncOnSurfaceSecondary,
    cursorColor = CncPrimary,
    focusedContainerColor = CncSurface,
    unfocusedContainerColor = CncSurface,
)

@Composable
fun CncTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        colors = fieldColors(),
        shape = RoundedCornerShape(8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CncBottomSheet(
    open: Boolean,
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!open) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CncSurface,
        contentColor = CncOnSurface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = title,
                color = CncOnSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            content()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ConfirmDialog(
    open: Boolean,
    title: String,
    message: String,
    busy: Boolean = false,
    confirmLabel: String = "Удалить",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!open) return
    Dialog(onDismissRequest = { if (!busy) onCancel() }) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(CncSurface)
                .padding(20.dp),
        ) {
            Text(title, color = CncOnSurface, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = CncOnSurfaceSecondary, fontSize = 15.sp, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel, enabled = !busy) {
                    Text("Отмена", color = CncOnSurfaceSecondary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onConfirm,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = CncDanger),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = CncPrimary, contentColor = CncOnPrimary),
        shape = RoundedCornerShape(8.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text)
        }
    }
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = CncDanger),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(text)
    }
}

@Composable
fun SkeletonBox(modifier: Modifier = Modifier, height: Dp = 64.dp) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shift",
    )
    val brush = Brush.linearGradient(
        colors = listOf(CncSkeleton, Color(0xFFF5F5F5), CncSkeleton),
        start = Offset(shift - 200f, 0f),
        end = Offset(shift, 0f),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(brush),
    )
}

@Composable
fun SkeletonStack(rows: Int = 4, height: Dp = 72.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rows) { SkeletonBox(height = height) }
    }
}

@Composable
fun DragHandleIcon() {
    Icon(
        Icons.Default.DragHandle,
        contentDescription = "Перетащить",
        tint = CncMuted,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
fun EmptyText(text: String) {
    Text(
        text = text,
        color = CncMuted,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

suspend fun SnackbarHostState.showAppError(message: String) {
    showSnackbar(message = message, withDismissAction = true)
}

@Composable
fun AutoDismissSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState) { data ->
        LaunchedEffect(data) {
            delay(2500)
            data.dismiss()
        }
        Snackbar(
            snackbarData = data,
            containerColor = Color(0xFF2A2A2A),
            contentColor = Color.White,
            actionColor = Color.White,
            shape = RoundedCornerShape(12.dp),
        )
    }
}
