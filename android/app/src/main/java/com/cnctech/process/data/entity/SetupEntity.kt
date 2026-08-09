package com.cnctech.process.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "setups",
    foreignKeys = [
        ForeignKey(
            entity = TechProcessEntity::class,
            parentColumns = ["id"],
            childColumns = ["techProcessId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CatalogItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["jawId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("techProcessId"),
        Index("jawId"),
        Index(value = ["techProcessId", "order"]),
    ],
)
data class SetupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val techProcessId: Long,
    val order: Int,
    val jawId: Long,
    val note: String? = null,
)
