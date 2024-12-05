package com.conviot.rssiindoorlocalization

import LocalizationViewModel
import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.Data
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import coil3.compose.AsyncImage
import com.conviot.rssiindoorlocalization.ui.theme.RSSIIndoorLocalizationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import kotlin.random.Random

/** RSSI 데이터를 통해 사용자의 위치를 확인하는 Activity */
class LocalizationActivity : ComponentActivity() {
    // Thread
    private var localizationJob: Job? = null

    // ViewModel
    private lateinit var dataCollectViewModel: DataCollectViewModel
    private lateinit var localizationViewModel: LocalizationViewModel

    // Localization 설정
    private var isTest: Boolean = false // 테스트 여부 (true면, 특정 record_id 기준으로 실행)
    private var localiationDelayMs: Long = 3000 // localization 간격 (ms)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel 생성
        dataCollectViewModel = ViewModelProvider(
            this,
            DataCollectViewModelFactory(
                applicationContext,
                userPreferencesStore
            )
        )[DataCollectViewModel::class.java]
        localizationViewModel = ViewModelProvider(this)[LocalizationViewModel::class.java]

        // RSSI 수집 권한 설정
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

    override fun onStart() {
        super.onStart()

        // Activity가 Create된 후, Start되면 Coroutine 시작
        localizationJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                // Localization 실행
                localization()

                // 일정 주기마다 반복
                delay(localiationDelayMs)
            }
        }
    }

    override fun onStop() {
        super.onStop()

        // Activity가 Stop되면, Coroutine 종료
        localizationJob?.cancel()
    }

    /** CSV 파일에서 학습에 사용된 BSSID 리스트를 반환 */
    fun loadBssidListFromCsv(context: Context): List<String> {
        // 파일 및 Context
        val csvFileName = "bssid.csv" // CSV 파일 이름
        val assetManager = context.assets

        // CSV 파일 파싱
        return assetManager.open(csvFileName).bufferedReader().use { reader ->
            reader.readLines().flatMap { line ->
                line.split(",").map { it.trim() } // CSV의 각 셀을 BSSID로 추출
            }
        }
    }

    /** 하나의 record_id만 들어있도록 가공된 CSV 파일에서, WifiInfo 리스트를 반환 */
    fun parseCsvToWifiInfoList(context: Context): MutableList<DataCollectActivity.WifiInfo> {
        // 파일 및 Context
        val csvFileName = "rssi_single_record_1.csv"
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

    /** TFLite 모델 실행 */
    suspend fun runModel(
        context: Context,
        interpreter: Interpreter,
        wifiList: SnapshotStateList<DataCollectActivity.WifiInfo>
    ): Pair<Float, Float> = withContext(Dispatchers.IO) {
        // 학습된 BSSID 리스트
        val trainedBssidList = loadBssidListFromCsv(context)
        Log.d("RunModel_TrainedBssidList", trainedBssidList.toString())

        // BSSID-RSSI 매핑 생성
        val rssiMap = wifiList.associate { it.bssid to it.rssi.toFloat() }

        // 학습된 BSSID 순서에 따라 RSSI 값을 정렬 후, 입력값으로 사용
        val inputArray = trainedBssidList.map { bssid ->
            val rssi = rssiMap[bssid] ?: -100f  // rssiMap에 값이 없으면 -100f로 설정
            if (rssi < -85f) -100f else rssi    // 값이 -85f 미만이면 -100f로 설정
        }.toFloatArray()
        Log.d("RunModel_Input", Arrays.toString(inputArray))

        // 출력 크기 설정 (예: [[x, y]])
        val outputArray = Array(1) { FloatArray(2) }

        // 모델 실행
        interpreter.run(inputArray, outputArray)

        // 결과 반환
        Log.d("RunModel_Output", "X: ${outputArray[0][0]}, Y: ${outputArray[0][1]}")
        Pair(outputArray[0][0], outputArray[0][1])
    }

    /** RSSI 데이터를 통해 사용자의 위치를 확인 */
    fun localization() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 모델 로드
                val interpreter = loadModel(this@LocalizationActivity)

                // localization에 사용할 WifiList 설정
                val wifiList: SnapshotStateList<DataCollectActivity.WifiInfo>

                // 테스트 여부에 따라 WifiList를 다르게 설정
                if (isTest) {
                    wifiList =
                        parseCsvToWifiInfoList(this@LocalizationActivity).toMutableStateList()
                } else {
                    wifiList = dataCollectViewModel.wifiList
                }

                // 모델 실행
                val (x, y) = runModel(
                    this@LocalizationActivity,
                    interpreter,
                    wifiList
                )

                // 모델 종료
                interpreter.close()

                // 사용자 위치 설정
                localizationViewModel.updateLocalization(x, y)

                Log.d(
                    "TestLocalization",
                    "X: ${x}, Y: ${y}"
                )
            } catch (e: Exception) {
                Log.e("TestLocalization", "Error during model execution: ${e.message}")
            }
        }
    }

    /** RSSI 데이터를 수집하고, 컴포넌트를 표시 */
    @Composable
    fun DataCollector(modifier: Modifier = Modifier) {
        // 지도 파일 이름을 임시 설정
        val filename = "map"

        // 지도 파일을 설정 (이미 파일이 설정된 경우, 해당 파일로 설정)
        val file = File(applicationContext.filesDir, filename)
        val imageUri = if (file.exists()) {
            Uri.fromFile(file)
        } else {
            null
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
            if (localizationViewModel.localizationTested.value) {
                LocalizationTestUserPoint()
            }
        }
        /*
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
                {
                    testLocalization()
                },
                Modifier.width(200.dp)
            )
        }
        */
    }

    /** Localization 후 사용자의 위치를 나타냄 */
    @Composable
    fun LocalizationTestUserPoint(modifier: Modifier = Modifier) {
        // 파란 점을 그릴 Canvas
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 지도 좌표 변환
            val pointX = localizationViewModel.localizationX.value * dataCollectViewModel.imageWidth * (3962f/9228)
            val pointY =
                localizationViewModel.localizationY.value * dataCollectViewModel.imageHeight

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