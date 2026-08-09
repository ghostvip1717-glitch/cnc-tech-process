package com.cnctech.process.ui.screens.parts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cnctech.process.CncApp
import com.cnctech.process.data.repository.AppResult
import com.cnctech.process.ui.components.ConfirmDialog
import com.cnctech.process.ui.components.CncTextField
import com.cnctech.process.ui.components.DangerButton
import com.cnctech.process.ui.components.EmptyText
import com.cnctech.process.ui.components.PrimaryButton
import com.cnctech.process.ui.components.SkeletonStack
import kotlinx.coroutines.launch

@Composable
fun PartEditScreen(
    partId: Long,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    onError: (String) -> Unit,
) {
    val repo = CncApp.instance.partRepository
    val data by repo.observePart(partId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var number by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var machiningTime by remember { mutableStateOf("") }
    var programCount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(data?.part?.id) {
        val part = data?.part ?: return@LaunchedEffect
        if (!initialized) {
            number = part.number
            title = part.title
            machiningTime = part.machiningTimeMinutes?.toString().orEmpty()
            programCount = part.programCount?.toString().orEmpty()
            note = part.note.orEmpty()
            initialized = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (data == null) {
            SkeletonStack(rows = 2)
            return
        }
        if (data?.part == null) {
            EmptyText("Деталь не найдена")
            return
        }
        CncTextField(value = number, onValueChange = { number = it }, label = "Номер")
        Spacer(modifier = Modifier.height(12.dp))
        CncTextField(value = title, onValueChange = { title = it }, label = "Название")
        Spacer(modifier = Modifier.height(12.dp))
        CncTextField(
            value = machiningTime,
            onValueChange = { machiningTime = it.filter { ch -> ch.isDigit() } },
            label = "Время обработки, мин",
        )
        Spacer(modifier = Modifier.height(12.dp))
        CncTextField(
            value = programCount,
            onValueChange = { programCount = it.filter { ch -> ch.isDigit() } },
            label = "Количество программ",
        )
        Spacer(modifier = Modifier.height(12.dp))
        CncTextField(
            value = note,
            onValueChange = { note = it },
            label = "Заметка",
            singleLine = false,
            minLines = 3,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(
            text = "Сохранить",
            busy = busy,
            onClick = {
                scope.launch {
                    busy = true
                    when (
                        val r = repo.update(
                            partId = partId,
                            number = number,
                            title = title,
                            machiningTimeMinutes = machiningTime.trim().toIntOrNull(),
                            programCount = programCount.trim().toIntOrNull(),
                            note = note,
                        )
                    ) {
                        is AppResult.Ok -> onSaved()
                        is AppResult.Err -> onError(r.message)
                    }
                    busy = false
                }
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        DangerButton(text = "Удалить деталь", onClick = { confirmDelete = true }, enabled = !busy)
    }

    ConfirmDialog(
        open = confirmDelete,
        title = "Удалить деталь?",
        message = "Удалить «${data?.part?.number ?: ""} — ${data?.part?.title ?: ""}»? Действие нельзя отменить",
        busy = busy,
        onCancel = { confirmDelete = false },
        onConfirm = {
            scope.launch {
                busy = true
                when (val r = repo.delete(partId)) {
                    is AppResult.Ok -> {
                        confirmDelete = false
                        onDeleted()
                    }
                    is AppResult.Err -> onError(r.message)
                }
                busy = false
            }
        },
    )
}
