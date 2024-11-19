package com.conviot.rssiindoorlocalization.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.conviot.rssiindoorlocalization.data.entity.RssiRecord

@Dao
interface RssiRecordDao {
    @Query("SELECT * FROM rssi_record")
    suspend fun getAll(): List<RssiRecord>

    @Query("SELECT * FROM rssi_record WHERE record_id = :recordId")
    suspend fun findByRssiId(recordId: Long): List<RssiRecord>

    @Query("SELECT distinct record_id FROM rssi_record " +
            "ORDER BY record_id DESC LIMIT 1")
    suspend fun findLastRecordId(): Long

    @Insert
    suspend fun insertAll(vararg records: RssiRecord)

    @Delete
    suspend fun delete(record: RssiRecord)

    @Query("DELETE FROM rssi_record")
    suspend fun deleteAll()
}