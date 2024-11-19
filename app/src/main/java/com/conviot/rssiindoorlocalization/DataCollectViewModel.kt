package com.conviot.rssiindoorlocalization

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.conviot.rssiindoorlocalization.data.RssiDatabase
import com.conviot.rssiindoorlocalization.data.entity.Location
import com.conviot.rssiindoorlocalization.data.entity.RssiRecord
import com.conviot.rssiindoorlocalization.datastore.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DataCollectViewModel(
    private val context: Context,
    private val dataStore: DataStore<UserPreferences>
) : ViewModel() {
    val db = Room.databaseBuilder(
        context,
        RssiDatabase::class.java, "rssi_db"
    ).build()
    val locationDao = db.locationDao()
    val rssiRecordDao = db.rssiRecordDao()

    private val _wifiList = mutableListOf<DataCollectActivity.WifiInfo>()
    val wifiList: SnapshotStateList<DataCollectActivity.WifiInfo> = _wifiList.toMutableStateList()

    var imageWidth by mutableFloatStateOf(0f)
        private set
    var imageHeight by mutableFloatStateOf(0f)
        private set

    var placeLabel by mutableStateOf("")
        private set
    var dialogState by mutableStateOf(false)
        private set

    private var x = 0f
    private var y = 0f

    private val wifiManager: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val wifiScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                scanSuccess()
            } else {
                scanFailure()
            }
        }
    }

    val handler = Handler(Looper.myLooper()!!)

    val thread1 = object : Thread() {
        override fun run() {
            super.run()

            scanWifi()

            handler.postDelayed(this, 3000)
        }
    }

    private val keywordList: MutableList<String> = mutableListOf()

    init {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)

        thread1.start()

        viewModelScope.launch {
            keywordList.addAll(dataStore.data.first().keywordsList)
        }
    }

    private fun scanWifi() {
        val success = wifiManager.startScan()
        if (!success) {
            scanFailure()
        }
    }

    @SuppressLint("MissingPermission")
    fun scanSuccess() {
        val result = wifiManager.scanResults
        Log.d("Wifi", result.toString())

        wifiList.clear()
        result.forEach { info ->
            wifiList.add(DataCollectActivity.WifiInfo(
                ssid = info.SSID!!,
                bssid = info.BSSID!!,
                rssi = info.level
            ))
        }
    }

    fun scanFailure() {

    }

    fun setImageSize(coordinate: LayoutCoordinates) {
        imageWidth = coordinate.size.width.toFloat()
        imageHeight = coordinate.size.height.toFloat()
    }

    fun mapTapped(offset: Offset) {
        dialogState = true;

        x = offset.x / imageWidth
        y = offset.y / imageHeight

        Log.d("Tap", "(${x}, ${y})")
    }

    fun onPlaceLabelChanged(value: String) {
        placeLabel = value
    }

    fun onDialogDismiss() {
        dialogState = false;
    }

    fun onDialogConfirm() {
        dialogState = false;

        viewModelScope.launch(Dispatchers.IO) {
            val time = System.currentTimeMillis()
            val recordId = rssiRecordDao.findLastRecordId() + 1

            Log.d("Keyword", keywordList.toString())

            locationDao.insert(Location(0, time, placeLabel, x, y, recordId))
            rssiRecordDao.insertAll(
                *wifiList.filter { wifiInfo ->
                    keywordList.any { keyword ->
                        wifiInfo.ssid.contains(keyword, ignoreCase = true)
                    }
                }.map { wifiInfo ->
                    RssiRecord(
                        recordId = recordId,
                        ssid = wifiInfo.ssid,
                        bssid = wifiInfo.bssid,
                        rssi = wifiInfo.rssi
                    )
                }.toTypedArray()
            )
        }
    }
}