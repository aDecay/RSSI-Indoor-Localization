package com.conviot.rssiindoorlocalization.ui.theme

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import com.conviot.rssiindoorlocalization.DataCollectActivity

class DataCollectViewModel(
    private val context: Context
) : ViewModel() {
    private val _wifiList = mutableListOf<DataCollectActivity.WifiInfo>()
    val wifiList: SnapshotStateList<DataCollectActivity.WifiInfo> = _wifiList.toMutableStateList()

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

            handler.postDelayed(this, 5000) // 100 쉬고 동작 -> 100 사이에 화면 처리
        }
    }

    init {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)

        thread1.start()
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
}