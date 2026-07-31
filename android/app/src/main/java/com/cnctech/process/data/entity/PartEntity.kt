package com.cnctech.process.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parts",
    indices = [Index(value = ["number"], unique = true)],
)
data class PartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val title: String,
    val createdAt: Long,
    val machiningTimeMinutes: Int? = null,
    val programCount: Int? = null,
)
