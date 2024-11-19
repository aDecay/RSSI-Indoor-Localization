package com.conviot.rssiindoorlocalization.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.conviot.rssiindoorlocalization.data.entity.Location

@Dao
interface LocationDao {
    @Query("SELECT * FROM location")
    suspend fun getAll(): List<Location>

    @Insert
    suspend fun insert(location: Location)

    @Delete
    suspend fun delete(location: Location)

    @Query("DELETE FROM location")
    suspend fun deleteAll()
}