package com.cnctech.process.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "catalog_item_photos",
    foreignKeys = [
        ForeignKey(
            entity = CatalogItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["catalogItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("catalogItemId"), Index(value = ["catalogItemId", "sortOrder"])],
)
data class CatalogItemPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catalogItemId: Long,
    val filePath: String,
    val sortOrder: Int,
)
