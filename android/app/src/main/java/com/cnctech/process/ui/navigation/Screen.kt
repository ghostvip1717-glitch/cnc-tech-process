package com.cnctech.process.ui.navigation

sealed class Screen {
    data object Parts : Screen()
    data class Part(val partId: Long) : Screen()
    data class PartEdit(val partId: Long) : Screen()
    data class PartGallery(val partId: Long) : Screen()
    data class PartPhotoViewer(val partId: Long, val startIndex: Int) : Screen()
    data class TechProcess(val partId: Long) : Screen()
    data class Setup(val partId: Long, val setupId: Long) : Screen()
    data class Assembly(val partId: Long) : Screen()
    data object Catalog : Screen()
    data class CatalogGallery(val catalogItemId: Long, val title: String) : Screen()
    data class CatalogPhotoViewer(val catalogItemId: Long, val startIndex: Int) : Screen()
    data object Settings : Screen()
}

/** Roots shown in the navigation drawer. */
enum class RootSection {
    Parts,
    Catalog,
    Settings,
}

fun Screen.isRoot(): Boolean = when (this) {
    Screen.Parts, Screen.Catalog, Screen.Settings -> true
    else -> false
}

fun Screen.isPhotoViewer(): Boolean =
    this is Screen.PartPhotoViewer || this is Screen.CatalogPhotoViewer

fun Screen.rootSection(): RootSection = when (this) {
    Screen.Parts, is Screen.Part, is Screen.PartEdit, is Screen.PartGallery,
    is Screen.PartPhotoViewer, is Screen.TechProcess, is Screen.Setup, is Screen.Assembly -> RootSection.Parts
    Screen.Catalog, is Screen.CatalogGallery, is Screen.CatalogPhotoViewer -> RootSection.Catalog
    Screen.Settings -> RootSection.Settings
}

fun Screen.title(): String = when (this) {
    Screen.Parts -> "Детали"
    is Screen.Part -> "Карточка детали"
    is Screen.PartEdit -> "Изменить деталь"
    is Screen.PartGallery -> "Фото"
    is Screen.PartPhotoViewer -> ""
    is Screen.TechProcess -> "Техпроцесс"
    is Screen.Setup -> "Установ"
    is Screen.Assembly -> "Сборка"
    Screen.Catalog -> "Инструмент"
    is Screen.CatalogGallery -> "Фото"
    is Screen.CatalogPhotoViewer -> ""
    Screen.Settings -> "Настройки"
}

fun RootSection.toScreen(): Screen = when (this) {
    RootSection.Parts -> Screen.Parts
    RootSection.Catalog -> Screen.Catalog
    RootSection.Settings -> Screen.Settings
}
