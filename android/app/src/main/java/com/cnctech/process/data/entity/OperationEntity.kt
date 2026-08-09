package com.cnctech.process.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "operations",
    foreignKeys = [
        ForeignKey(
            entity = SetupEntity::class,
            parentColumns = ["id"],
            childColumns = ["setupId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CatalogItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["toolId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CatalogItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["plateId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("setupId"),
        Index("toolId"),
        Index("plateId"),
        Index(value = ["setupId", "order"]),
    ],
)
data class OperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setupId: Long,
    val order: Int,
    val opNumber: String,
    val title: String,
    val toolId: Long,
    val plateId: Long,
    val comment: String? = null,
)
