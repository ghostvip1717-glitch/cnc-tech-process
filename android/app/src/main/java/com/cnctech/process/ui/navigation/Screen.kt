package com.cnctech.process.ui.navigation

import com.cnctech.process.data.entity.CatalogType

sealed class Screen {
    data object Parts : Screen()
    data class Part(val partId: Long) : Screen()
    data class PartEdit(val partId: Long) : Screen()
    data class PartPhotoViewer(val partId: Long, val startIndex: Int) : Screen()
    data class TechProcess(val partId: Long) : Screen()
    data class Setup(val partId: Long, val setupId: Long) : Screen()
    data class SetupEdit(val partId: Long, val setupId: Long) : Screen()
    data class SetupPhotoViewer(val setupId: Long, val startIndex: Int) : Screen()
    data class OperationPhotoViewer(val operationId: Long, val startIndex: Int) : Screen()
    data class Assembly(val partId: Long) : Screen()
    data class Catalog(val activeType: CatalogType = CatalogType.tool) : Screen()
    data class CatalogItemDetail(val catalogItemId: Long) : Screen()
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
    Screen.Parts, is Screen.Catalog, Screen.Settings -> true
    else -> false
}

fun Screen.isPhotoViewer(): Boolean =
    this is Screen.PartPhotoViewer ||
        this is Screen.CatalogPhotoViewer ||
        this is Screen.SetupPhotoViewer ||
        this is Screen.OperationPhotoViewer

fun Screen.rootSection(): RootSection = when (this) {
    Screen.Parts, is Screen.Part, is Screen.PartEdit,
    is Screen.PartPhotoViewer, is Screen.TechProcess, is Screen.Setup, is Screen.SetupEdit,
    is Screen.SetupPhotoViewer, is Screen.OperationPhotoViewer,
    is Screen.Assembly -> RootSection.Parts
    is Screen.Catalog, is Screen.CatalogItemDetail, is Screen.CatalogPhotoViewer -> RootSection.Catalog
    Screen.Settings -> RootSection.Settings
}

fun Screen.title(): String = when (this) {
    Screen.Parts -> "Детали"
    is Screen.Part -> "Карточка детали"
    is Screen.PartEdit -> "Изменить деталь"
    is Screen.PartPhotoViewer -> ""
    is Screen.TechProcess -> "Техпроцесс"
    is Screen.Setup -> "Установ"
    is Screen.SetupEdit -> "Изменить установ"
    is Screen.SetupPhotoViewer -> ""
    is Screen.OperationPhotoViewer -> ""
    is Screen.Assembly -> "Сборка"
    is Screen.Catalog -> when (activeType) {
        CatalogType.tool -> "Инструмент"
        CatalogType.plate -> "Пластины"
        CatalogType.jaw -> "Кулачки"
    }
    is Screen.CatalogItemDetail -> "Справочник"
    is Screen.CatalogPhotoViewer -> ""
    Screen.Settings -> "Настройки"
}

fun RootSection.toScreen(): Screen = when (this) {
    RootSection.Parts -> Screen.Parts
    RootSection.Catalog -> Screen.Catalog()
    RootSection.Settings -> Screen.Settings
}
