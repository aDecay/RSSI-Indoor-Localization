package com.conviot.rssiindoorlocalization

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conviot.rssiindoorlocalization.data.RssiDatabase
import com.conviot.rssiindoorlocalization.data.entity.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

class LocationDataShowViewModel(
    private val db: RssiDatabase,
    private val context: Context
) : ViewModel() {
    val locationList = mutableStateListOf<Location>()

    private val locationDao = db.locationDao()
    private val rssiRecordDao = db.rssiRecordDao()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            locationList.addAll(locationDao.getAll())
        }
    }

    fun onDeleteAllClick() {
        viewModelScope.launch(Dispatchers.IO) {
            locationDao.deleteAll()
            rssiRecordDao.deleteAll()
            locationList.clear()
        }
    }

    fun onExportClick() {
        viewModelScope.launch(Dispatchers.IO) {
            val locationList = locationDao.getAll()
            val locationContent = StringBuilder("id,timestamp,label,x,y,record_id\n")
            locationList.forEach { location ->
                locationContent.append("${location.id},${location.timestamp},${location.label ?: ""},${location.x},${location.y},${location.recordId}\n")
            }
            saveCsv("location.csv", locationContent.toString())

            val rssiRecordList = rssiRecordDao.getAll()
            val rssiRecordContent = StringBuilder("record_id,ssid,bssid,rssi\n")
            rssiRecordList.forEach { rssiRecord ->
                rssiRecordContent.append("${rssiRecord.recordId},${rssiRecord.ssid},${rssiRecord.bssid},${rssiRecord.rssi}\n")
            }
            saveCsv("rssi_record.csv", rssiRecordContent.toString())
        }
    }

    private fun saveCsv(fileName: String, content: String) {
        val contentResolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName) // 파일 이름
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv") // MIME 타입
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }
        val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val bom = "\uFEFF".toByteArray(Charsets.UTF_8)
                outputStream.write(bom)
                outputStream.write(content.toByteArray())
            }
        }
    }
}
