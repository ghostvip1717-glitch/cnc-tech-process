package com.cnctech.process.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "catalog_compatibility",
    primaryKeys = ["toolId", "plateId"],
    foreignKeys = [
        ForeignKey(
            entity = CatalogItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["toolId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CatalogItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["plateId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plateId")],
)
data class CatalogCompatibilityEntity(
    val toolId: Long,
    val plateId: Long,
)
