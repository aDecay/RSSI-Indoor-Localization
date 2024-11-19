package com.conviot.rssiindoorlocalization

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModelProvider
import coil3.compose.AsyncImage
import com.conviot.rssiindoorlocalization.data.UserPreferencesSerializer
import com.conviot.rssiindoorlocalization.datastore.UserPreferences
import com.conviot.rssiindoorlocalization.ui.theme.RSSIIndoorLocalizationTheme
import java.io.File

class DataCollectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                permissions.entries.forEach { permission ->
                    when {
                        permission.value -> {
                            Log.d("Permission", "${permission.key} granted")
                        }
                        shouldShowRequestPermissionRationale(permission.key) -> {
                            Log.d("Permission", "${permission.key} required")
                        }
                        else -> {
                            Log.d("Permission", "${permission.key} denied")
                            finish()
                        }
                    }
                }
            }
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
            )
        )
        enableEdgeToEdge()
        setContent {
            RSSIIndoorLocalizationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DataCollector(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    data class WifiInfo(val ssid: String, val bssid: String, val rssi: Int)

    @Composable
    fun DataCollector(
        dataCollectViewModel: DataCollectViewModel = ViewModelProvider(
            this,
            DataCollectViewModelFactory(
                applicationContext,
                userPreferencesStore
            )
        ).get(DataCollectViewModel::class.java),
        modifier: Modifier = Modifier
    ) {
        val filename = "map"

        val file = File(applicationContext.filesDir, filename)
        val imageUri = if (file.exists()) {
            Uri.fromFile(file)
        } else {
            null
        }

        if (dataCollectViewModel.dialogState) {
            PlaceLabelDialog(
                placeLabel = dataCollectViewModel.placeLabel,
                onPlaceLabelChanged = { dataCollectViewModel.onPlaceLabelChanged(it) },
                onDismissRequest = { dataCollectViewModel.onDialogDismiss() },
                onConfirmation = { dataCollectViewModel.onDialogConfirm() }
            )
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
                modifier = modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            dataCollectViewModel.mapTapped(offset)
                        }
                    }.onGloballyPositioned { coordinate ->
                        dataCollectViewModel.setImageSize(coordinate)
                    }
            )
            LazyColumn {
                items(dataCollectViewModel.wifiList) { wifiInfo ->
                    WifiItem(wifiInfo)
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
    fun PlaceLabelDialog(
        placeLabel: String,
        onPlaceLabelChanged: (String) -> Unit,
        onDismissRequest:  () -> Unit,
        onConfirmation: () -> Unit
    ) {
        Dialog(onDismissRequest = { onDismissRequest() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                TextField(
                    value = placeLabel,
                    onValueChange = onPlaceLabelChanged,
                    placeholder = {
                        Text("장소 이름")
                    },
                    modifier = Modifier.padding(16.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("취소")
                    }
                    TextButton(
                        onClick = { onConfirmation() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("확인")
                    }
                }
            }
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

    @Composable
    @Preview(showBackground = true)
    fun PlaceLabelDialogPreview() {
        PlaceLabelDialog(
            "장소",
            onPlaceLabelChanged = {},
            onDismissRequest = {},
            onConfirmation = {}
        )
    }
}