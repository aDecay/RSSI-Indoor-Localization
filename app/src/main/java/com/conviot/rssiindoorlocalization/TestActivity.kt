package com.conviot.rssiindoorlocalization

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.lifecycle.ViewModelProvider
import coil3.compose.AsyncImage
import com.conviot.rssiindoorlocalization.data.UserPreferencesSerializer
import com.conviot.rssiindoorlocalization.datastore.UserPreferences
import com.conviot.rssiindoorlocalization.ui.theme.RSSIIndoorLocalizationTheme
import java.io.File
import kotlin.random.Random

/** RSSI 데이터를 통한 사용자의 위치 확인 기능을 테스트하는 Activity */
class TestActivity : ComponentActivity() {
    /** onCreate: wifiList를 사용하기 위해 DataCollectActivity와 동일하게 사용 */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
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

    /** RSSI 데이터를 수집하고, 컴포넌트를 표시 */
    @Composable
    fun DataCollector(
        dataCollectViewModel: DataCollectViewModel = ViewModelProvider(
            this,
            DataCollectViewModelFactory(
                applicationContext,
                userPreferencesStore
            )
        )[DataCollectViewModel::class.java],
        modifier: Modifier = Modifier
    ) {
        // 지도 파일 이름을 임시 설정
        val filename = "map"

        // 지도 파일을 설정 (이미 파일이 설정된 경우, 해당 파일로 설정)
        val file = File(applicationContext.filesDir, filename)
        val imageUri = if (file.exists()) {
            Uri.fromFile(file)
        } else {
            null
        }

        // Localiztion 관련 (테스트 여부, 테스트 결과로 얻은 사용자의 위치 좌표)
        val localizationTested = remember { mutableStateOf(false) }
        val localizationX = remember { mutableStateOf(0f) }
        val localizationY = remember { mutableStateOf(0f) }

        /** RSSI 데이터를 통해 사용자의 위치를 확인 */
        fun testLocalization(dataCollectViewModel: DataCollectViewModel) {
            /**
             * ToDo:
             * 1. dataCollectViewModel.wifiList에서 현재 위치의 SSID, BSSID, RSSI 값을 획득
             * 2-1. Feature로 사용된 BSSID가 현재 위치의 BSSID에 있을 경우: 해당 BSSID의 RSSI 값을 설정
             * 2-2. Feature로 사용된 BSSID가 현재 위치의 BSSID에 없을 경우: 결측값의 RSSI 값을 -200으로 설정
             * 2-3. 현재 위치의 BSSID에는 있지만 Feature로 사용된 BSSID에 없을 경우: 넘어감
             * 3. RSSI를 설정한 후, 모델에 해당 값을 보내 사용자의 위치를 계산하고 Localization 관련 변수 설정
             * 4. 해당 변수들로 지도에 점을 찍어 사용자의 현재 위치를 나타냄
             */
            // 임시
            localizationX.value = Random.nextFloat()
            localizationY.value = Random.nextFloat()
            localizationTested.value = true

            Log.d("testLocalization", "X: ${localizationX.value}, Y: ${localizationY.value}")
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 지도 이미지 표시
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinate ->
                        dataCollectViewModel.setImageSize(coordinate)
                    }
            )
            // Localization 후 사용자의 위치 표시
            if (localizationTested.value) {
                LocalizationTestUserPoint(
                    localizationX.value,
                    localizationY.value,
                    dataCollectViewModel
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // 테스트 버튼 표시
            LocalizationTestButton(
                "테스트",
                { testLocalization(dataCollectViewModel) },
                Modifier.width(200.dp)
            )

        }
    }

    /** Localization 후 사용자의 위치를 나타냄 */
    @Composable
    fun LocalizationTestUserPoint(
        localizationX: Float,
        localizationY: Float,
        dataCollectViewModel: DataCollectViewModel,
        modifier: Modifier = Modifier
    ) {
        // 파란 점을 그릴 Canvas
        Canvas(
            modifier = modifier.fillMaxSize().statusBarsPadding()
        ) {
            // 지도 좌표 변환
            val pointX = localizationX * dataCollectViewModel.imageWidth
            val pointY = localizationY * dataCollectViewModel.imageHeight

            // 사용자의 위치에 파란색 점 그리기
            drawCircle(
                color = Color.Blue,
                radius = 3.dp.toPx(), // 점의 반지름
                center = Offset(pointX, pointY)
            )
        }
    }

    /** Localization을 테스트하는 버튼 */
    @Composable
    fun LocalizationTestButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier
    ) {
        Button(
            onClick,
            modifier = modifier
        ) {
            Text(text = text)
        }
    }
}