package com.conviot.rssiindoorlocalization.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Location (
    @PrimaryKey (autoGenerate = true)
    val id: Long,
    @ColumnInfo
    val timestamp: Long,
    @ColumnInfo
    val label: String?,
    @ColumnInfo
    val x: Float,
    @ColumnInfo
    val y: Float,
    @ColumnInfo(name = "record_id")
    val recordId: Long
)