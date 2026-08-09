package com.cnctech.process.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "operation_photos",
    foreignKeys = [
        ForeignKey(
            entity = OperationEntity::class,
            parentColumns = ["id"],
            childColumns = ["operationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("operationId"), Index(value = ["operationId", "sortOrder"])],
)
data class OperationPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationId: Long,
    val filePath: String,
    val sortOrder: Int = 0,
)
