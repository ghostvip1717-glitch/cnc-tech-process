package com.cnctech.process.ui.navigation

sealed class Screen {
    data object Hub : Screen()
    data object Parts : Screen()
    data class Part(val partId: Long) : Screen()
    data class PartEdit(val partId: Long) : Screen()
    data class PartGallery(val partId: Long) : Screen()
    data class TechProcess(val partId: Long) : Screen()
    data class Setup(val partId: Long, val setupId: Long) : Screen()
    data class Assembly(val partId: Long) : Screen()
    data object Catalog : Screen()
    data class CatalogGallery(val catalogItemId: Long, val title: String) : Screen()
    data object Backup : Screen()
}

fun Screen.title(): String = when (this) {
    Screen.Hub -> "Техпроцессы ЧПУ"
    Screen.Parts -> "Детали"
    is Screen.Part -> "Карточка детали"
    is Screen.PartEdit -> "Изменить деталь"
    is Screen.PartGallery -> "Фото"
    is Screen.TechProcess -> "Техпроцесс"
    is Screen.Setup -> "Установ"
    is Screen.Assembly -> "Сборка"
    Screen.Catalog -> "Инструмент"
    is Screen.CatalogGallery -> "Фото"
    Screen.Backup -> "Резервная копия"
}
