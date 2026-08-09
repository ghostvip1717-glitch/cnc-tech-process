package com.cnctech.process.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tech_processes",
    foreignKeys = [
        ForeignKey(
            entity = PartEntity::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["partId"], unique = true)],
)
data class TechProcessEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partId: Long,
)
