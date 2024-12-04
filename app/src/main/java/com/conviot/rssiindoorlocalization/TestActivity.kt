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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
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

    /** TFLite 모델 로드 후 반환 */
    suspend fun loadModel(context: Context): Interpreter = withContext(Dispatchers.IO) {
        // 파일 및 Context
        val modelPath = "model.tflite"
        val assetManager = context.assets

        // 모델 파일 읽기
        val inputStream = assetManager.open(modelPath)
        val modelBytes = inputStream.readBytes()
        inputStream.close()

        // ByteArray -> ByteBuffer 변환
        val byteBuffer = ByteBuffer.allocateDirect(modelBytes.size).apply {
            order(ByteOrder.nativeOrder()) // 플랫폼에 맞는 ByteOrder 사용
            put(modelBytes)
            rewind() // ByteBuffer의 위치를 0으로 초기화
        }

        // Interpreter 생성
        Interpreter(byteBuffer)
    }

    /** CSV 파일에서 학습에 사용된 BSSID 리스트를 반환 */
    fun loadBssidListFromCsv(context: Context): List<String> {
        // 파일 및 Context
        val csvFileName = "bssid.csv" // CSV 파일 이름
        val assetManager = context.assets

        return assetManager.open(csvFileName).bufferedReader().use { reader ->
            reader.readLines().flatMap { line ->
                line.split(",").map { it.trim() } // CSV의 각 셀을 BSSID로 추출
            }
        }
    }

    /** 하나의 record_id만 들어있도록 가공된 CSV 파일에서, WifiInfo 리스트를 반환 */
    fun parseCsvToWifiInfoList(context: Context): MutableList<DataCollectActivity.WifiInfo> {
        // 파일 및 Context
        val csvFileName = "rssi_single_record_550.csv"
        val assetManager = context.assets

        // record_id 하나의 Wifi 리스트
        val wifiInfoList = mutableListOf<DataCollectActivity.WifiInfo>()

        // CSV 파일 읽기
        BufferedReader(
            InputStreamReader(
                assetManager.open(csvFileName),
                Charsets.UTF_8
            )
        ).use { reader ->
            // 첫 번째 라인(헤더) 건너뛰기
            reader.readLine()

            // 데이터 파싱
            reader.lineSequence().forEach { line ->
                val values = line.split(",")
                if (values.size >= 4) {
                    val ssid = values[1]
                    val bssid = values[2]
                    val rssi = values[3].toIntOrNull() ?: -100
                    wifiInfoList.add(DataCollectActivity.WifiInfo(ssid, bssid, rssi))
                }
            }
        }

        Log.d(
            "ParseCsvToWifiInfoList",
            "Index 0: ${wifiInfoList[0].ssid} / ${wifiInfoList[0].bssid} / ${wifiInfoList[0].rssi}"
        )
        return wifiInfoList
    }

    /** TFLite 모델 실행 */
    suspend fun runModel(
        context: Context,
        interpreter: Interpreter,
        wifiList: SnapshotStateList<DataCollectActivity.WifiInfo>
    ): Pair<Float, Float> = withContext(Dispatchers.IO) {
        // 학습된 BSSID 리스트
        val trainedBssidList = loadBssidListFromCsv(context)
        Log.d("RunModel_TrainedBssidList", trainedBssidList.toString())

        val rssiMap = wifiList.associate { it.bssid to it.rssi.toFloat() } // BSSID-RSSI 매핑 생성

        // 학습된 BSSID 순서에 따라 RSSI 값을 정렬 후, 입력값으로 사용
        val inputArray = trainedBssidList.map { bssid ->
            val rssi = rssiMap[bssid] ?: -100f  // rssiMap에 값이 없으면 -100f로 설정
            if (rssi < -85f) -100f else rssi    // 값이 -85f 미만이면 -100f로 설정
        }.toFloatArray()

        Log.d("RunModel_Input", Arrays.toString(inputArray))

        // 출력 크기 맞추기 (예: [[x, y]])
        val outputArray = Array(1) { FloatArray(2) }

        // 모델 실행
        interpreter.run(inputArray, outputArray)

        // 결과 반환
        Log.d("RunModel_Output", "X: ${outputArray[0][0]}, Y: ${outputArray[0][1]}")
        Pair(outputArray[0][0], outputArray[0][1])
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
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // 모델 로드
                    val interpreter = loadModel(this@TestActivity)

                    /*
                    // 모델 실행 (실제로 측정된 WifiInfo 리스트 사용)
                    val (x, y) = runModel(
                        this@TestActivity,
                        interpreter,
                        dataCollectViewModel.wifiList
                    )
                    */

                    // 모델 실행 (테스트 전용, 하나의 record_id에 대한 데이터 사용)
                    val _tempWifiList = parseCsvToWifiInfoList(this@TestActivity)
                    val tempWifiList = _tempWifiList.toMutableStateList()

                    val (x, y) = runModel(
                        this@TestActivity,
                        interpreter,
                        tempWifiList
                    )

                    // 모델 종료
                    interpreter.close()

                    // 사용자 위치 설정
                    localizationX.value = x
                    localizationY.value = y
                    localizationTested.value = true

                    Log.d(
                        "TestLocalization",
                        "X: ${localizationX.value}, Y: ${localizationY.value}"
                    )
                } catch (e: Exception) {
                    Log.e("TestLocalization", "Error during model execution: ${e.message}")
                }
            }
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
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
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