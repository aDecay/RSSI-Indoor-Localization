package com.conviot.rssiindoorlocalization

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.conviot.rssiindoorlocalization.ui.theme.RSSIIndoorLocalizationTheme
import java.io.File

class DataCollectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RSSIIndoorLocalizationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DataCollector(
                        Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    data class WifiInfo(val ssid: String, val bssid: String, val rssi: Int)

    @Composable
    fun DataCollector(modifier: Modifier = Modifier) {
        val filename = "map"

        val file = File(applicationContext.filesDir, filename)
        val imageUri = if (file.exists()) {
            Uri.fromFile(file)
        } else {
            null
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = modifier.fillMaxWidth()
            )
            LazyColumn {
                items(10) { index ->
                    WifiItem(WifiInfo("ssid$index", "bssid$index", index))
                }
            }
        }
    }
    
    @Composable
    fun WifiItem(
        wifiInfo: WifiInfo,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .border(BorderStroke(2.dp, SolidColor(Color.Black)))
        ) {
            Text("SSID: ${wifiInfo.ssid}")
            Text("BSSID: ${wifiInfo.bssid}")
            Text("RSSI: ${wifiInfo.rssi}")
        }
    }

    @Composable
    @Preview(showBackground = true)
    fun DataCollectorPreview() {
        RSSIIndoorLocalizationTheme {
            DataCollector()
        }
    }
    
    @Composable
    @Preview(showBackground = true)
    fun WifiItemPreview() {
        RSSIIndoorLocalizationTheme {
            WifiItem(WifiInfo("ssid", "bssid", 10))
        }
    }
}