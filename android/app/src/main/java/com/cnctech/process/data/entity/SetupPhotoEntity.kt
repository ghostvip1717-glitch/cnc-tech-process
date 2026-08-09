package com.cnctech.process.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "setup_photos",
    foreignKeys = [
        ForeignKey(
            entity = SetupEntity::class,
            parentColumns = ["id"],
            childColumns = ["setupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("setupId"), Index(value = ["setupId", "sortOrder"])],
)
data class SetupPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val setupId: Long,
    val filePath: String,
    val sortOrder: Int = 0,
)
