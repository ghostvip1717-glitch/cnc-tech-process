package com.cnctech.process.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CatalogType {
    tool,
    plate,
    jaw,
}

@Entity(
    tableName = "catalog_items",
    indices = [Index(value = ["type", "name"], unique = true)],
)
data class CatalogItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: CatalogType,
    val name: String,
    val note: String? = null,
    /** null = stock is not tracked (plates only). */
    val stockQty: Int? = null,
    val minStockThreshold: Int = 3,
)
