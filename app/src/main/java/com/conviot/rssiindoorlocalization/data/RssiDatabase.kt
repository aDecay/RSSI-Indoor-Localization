package com.conviot.rssiindoorlocalization.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.conviot.rssiindoorlocalization.data.dao.LocationDao
import com.conviot.rssiindoorlocalization.data.dao.RssiRecordDao
import com.conviot.rssiindoorlocalization.data.entity.Location
import com.conviot.rssiindoorlocalization.data.entity.RssiRecord

@Database(entities = [Location::class, RssiRecord::class], version = 1)
abstract class RssiDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun rssiRecordDao(): RssiRecordDao
}