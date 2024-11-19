package com.conviot.rssiindoorlocalization.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "rssi_record",
    primaryKeys = ["record_id", "bssid"],
)
class RssiRecord (
    @ColumnInfo(name = "record_id") val recordId: Long,
    @ColumnInfo val ssid: String,
    @ColumnInfo val bssid: String,
    @ColumnInfo val rssi: Int
)