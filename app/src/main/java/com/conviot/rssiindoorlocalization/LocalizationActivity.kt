package com.conviot.rssiindoorlocalization

import LocalizationViewModel
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.input.TextFieldValue
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import com.conviot.rssiindoorlocalization.data.UserPreferencesSerializer
import com.conviot.rssiindoorlocalization.datastore.UserPreferences
import com.conviot.rssiindoorlocalization.manager.LocalizationResponse
import com.conviot.rssiindoorlocalization.manager.Vector3D
import com.conviot.rssiindoorlocalization.manager.computeStepTimeStamp
import com.conviot.rssiindoorlocalization.manager.estimateTurningAngle
import com.conviot.rssiindoorlocalization.manager.sendUdpEnd
import com.conviot.rssiindoorlocalization.manager.sendUdpInitialState
import com.conviot.rssiindoorlocalization.manager.sendUdpLocalization
import com.conviot.rssiindoorlocalization.manager.sendUdpUpdateState
import com.conviot.rssiindoorlocalization.ui.theme.RSSIIndoorLocalizationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Delegate
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import kotlin.math.cos
import kotlin.math.sin


/** RSSI 데이터를 통해 사용자의 위치를 확인하는 Activity */
class LocalizationActivity : ComponentActivity(), SensorEventListener {
    private val dataStore by lazy { applicationContext.userPreferencesStore }

    // Thread
    private var localizationJob: Job? = null

    // ViewModel
    private lateinit var dataCollectViewModel: DataCollectViewModel
    private lateinit var localizationViewModel: LocalizationViewModel

    // Localization 설정
    private var isTest: Boolean = false // 테스트 여부 (true면, 특정 record_id 기준으로 실행)
    private val localizationDelayMs: Long = 3000 // localization 간격 (ms)
    private val deadReckoningDelayMs: Long = 50

    // Sensor
    private lateinit var sensorManager: SensorManager
    private lateinit var accelSensor: Sensor
    private lateinit var gyroSensor: Sensor
    private lateinit var magSensor: Sensor
    private lateinit var oriSensor: Sensor
    private val samplingPeriodUs = 10000

    //prisvate var wifiListTime = mutableListOf<FloatArray>()

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

        // IMU Sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!
        oriSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)!!
        sensorManager.registerListener(this, accelSensor, samplingPeriodUs)
        sensorManager.registerListener(this, gyroSensor, samplingPeriodUs)
        sensorManager.registerListener(this, magSensor, samplingPeriodUs)
        sensorManager.registerListener(this, oriSensor, samplingPeriodUs)

        lifecycleScope.launch {
            val initX = dataStore.data.map { ref ->
                ref.initX
            }.first()
            val initY = dataStore.data.map { ref ->
                ref.initY
            }.first()
            val initOri = dataStore.data.map { ref ->
                ref.initOri
            }.first()
            val serverAddr = dataStore.data.map { ref ->
                ref.server
            }.first()
            val serverPort = dataStore.data.map { ref ->
                ref.port
            }.first()
            val deadReckoning = dataStore.data.map { ref ->
                ref.deadReckoning
            }.first()
            val WiFi = dataStore.data.map { ref ->
                ref.wiFi
            }.first()

            if (!deadReckoning && !WiFi) {
                Toast.makeText(applicationContext, "위치 추정 방식 설정을 확인해주세요", Toast.LENGTH_SHORT).show()
                this@LocalizationActivity.finish()
            }

            localizationViewModel.setServerInfo(serverAddr, serverPort.toInt())
            localizationViewModel.updateLocalization(initX, initY, initOri)
            localizationViewModel.setLocalizationMethod(deadReckoning, WiFi)

            var result = false

            withContext(Dispatchers.IO) {
                result = sendUdpInitialState(initX, initY, initOri, serverAddr, serverPort.toInt())
            }

            if (result) {
                Toast.makeText(applicationContext, "서버 초기화 완료", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "서버 초기화 실패 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        enableEdgeToEdge()
        setContent {
            RSSIIndoorLocalizationTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    DataCollector()
                    FloatingActionButton(
                        onClick = {
                            Toast.makeText(applicationContext, "현재 근처에 ${localizationViewModel.currentLandmark.value}가 있습니다.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .offset(y = 16.dp),
                    ) {
                        Text(
                            text = localizationViewModel.currentLandmark.value,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .padding(16.dp)
                        )
                    }
                    FloatingActionButton (
                        onClick = { localizationViewModel.setIsFollowing(true) },
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text(text = if (localizationViewModel.isFollowing.value) "고정 해제" else "화면 고정")
                    }

                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // RSSI-Based
        if (localizationViewModel.isWiFi.value) {
            localizationJob = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    // Localization 실행
                    localization()

                    // 일정 주기마다 반복
                    delay(localizationDelayMs)
                }
            }
        }

        // Dead Reckoning
        if (localizationViewModel.isDeadReckoning.value) {
            lifecycleScope.launch {
                while (isActive) {
                    // Delay
                    var result: LocalizationResponse

                    delay(deadReckoningDelayMs)
                    withContext(Dispatchers.IO) {
                        val accData = localizationViewModel.accData
                        val gyroData = localizationViewModel.gyroData
                        val magData = localizationViewModel.magData
                        val oriData = localizationViewModel.oriData

                        // CSV
                        val sb = StringBuilder()
                        sb.append("acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,ori_x,ori_y,ori_z\n")

                        val minSize = minOf(accData.size, gyroData.size, oriData.size)
                        Log.d("minsize", "${accData.size}, ${gyroData.size}, ${oriData.size}")
                        for (i in 0 until minSize) {
                            val acc = accData[i]
                            val gyro = gyroData[i]
                            val ori = oriData[i]

                            sb.append("${acc.x},${acc.y},${acc.z},")
                            sb.append("${gyro.x},${gyro.y},${gyro.z},")
                            sb.append("${ori.x},${ori.y},${ori.z}\n")
                        }

                        // Transmit & Update
                        result = sendUdpLocalization(
                            sb.toString(),
                            localizationViewModel.serverAddress.value,
                            localizationViewModel.serverPort.value
                        )

                        if (result.isStepped) {
                            localizationViewModel.updateLocalization(
                                result.x,
                                result.y,
                                result.radian
                            )
                            localizationViewModel.accData.clear()
                            localizationViewModel.gyroData.clear()
                            localizationViewModel.oriData.clear()

                            localizationViewModel.setCurrentLandmark(result.landmark)
                            Log.d(
                                "TestLocalization",
                                "X: ${localizationViewModel.localizationX}, Y: ${localizationViewModel.localizationY}"
                            )
                        } else {
                            // No Step Dectected
                        }
                    }


                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelSensor.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
        gyroSensor.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
        magSensor.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
        oriSensor.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
    }

    override fun onStop() {
        super.onStop()

        sensorManager.unregisterListener(this)

        // Activity가 Stop되면, Coroutine 종료
        localizationJob?.cancel()

        lifecycleScope.launch {
            var result = false
            withContext(Dispatchers.IO) {
                result = sendUdpEnd(localizationViewModel.serverAddress.value, localizationViewModel.serverPort.value)
            }
            if (result) {
                Toast.makeText(applicationContext, "서버에 파일이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "서버에 파일 종료 실패", Toast.LENGTH_SHORT).show()
            }
        }
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
        // 파일 및 Context (assets 폴더의 rssi_single_record_*.csv 파일 참조)
        val csvFileName = "rssi_single_record_689.csv"
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
        // val modelPath = "model.tflite"
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

        // FlexDelegate를 사용하여 모델 로드
        val options = Interpreter.Options()

// FlexDelegate 추가
        val delegate: Delegate = FlexDelegate()
        options.addDelegate(delegate)

        // Interpreter 생성
        Interpreter(byteBuffer, options)
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

        /*
        val size = wifiListTime.size
        if (size == 0 || !wifiListTime[size - 1].contentEquals(inputArray)) {
            wifiListTime.add(inputArray.copyOf())
            if (size >= 3)
                wifiListTime.removeAt(0)
        }

        // 출력 크기 설정 (예: [[x, y]])
        val outputArray = Array(1) { FloatArray(2) }

        if (wifiListTime.size == 3) {
            // 모델 실행
            interpreter.run(Array(1) {wifiListTime.toTypedArray()}, outputArray)

            // 결과 반환
            Log.d("RunModel_Output", "X: ${outputArray[0][0]}, Y: ${outputArray[0][1]}")
            Pair(outputArray[0][0], outputArray[0][1])
        } else {
            Pair(-1.0f, -1.0f)
        }
         */
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

                Log.d(
                    "WifiList",
                    "${wifiList}"
                )

                // 모델 실행
                val (x, y) = runModel(
                    this@LocalizationActivity,
                    interpreter,
                    wifiList
                )

                // 모델 종료
                interpreter.close()

                if (wifiList.isNotEmpty()) {
                    val WiFiOnly = localizationViewModel.isWiFi.value && !localizationViewModel.isDeadReckoning.value

                    withContext(Dispatchers.IO) {
                        val response = sendUdpUpdateState(x, y, WiFiOnly, localizationViewModel.serverAddress.value, localizationViewModel.serverPort.value)
                        localizationViewModel.updateLocalization(response.x, response.y, response.radian)
                    }
                }
            } catch (e: Exception) {
                Log.e("TestLocalization", "Error during model execution: ${e.message}")
            }
        }
    }

    /** RSSI 데이터를 수집하고, 컴포넌트를 표시 */
    @Composable
    fun DataCollector() {
        // 상태 변수 정의
        var scale by remember { mutableFloatStateOf(1.0f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val state = rememberTransformableState { zoomChange, offsetChange, _ ->
            scale *= zoomChange
            offset += offsetChange
            localizationViewModel.setIsFollowing(false)
        }

        // 지도 파일 이름을 임시 설정
        val filename = "map"

        // 지도 파일을 설정 (이미 파일이 설정된 경우, 해당 파일로 설정)
        val file = File(applicationContext.filesDir, filename)
        val imageUri = if (file.exists()) {
            Uri.fromFile(file)
        } else {
            null
        }

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true // 실제로 이미지를 디코딩하지 않음
        }
        BitmapFactory.decodeFile(imageUri?.path.toString(), options)

        // 화면 크기 가져오기
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp

        // 지도 이미지 크기 상태
        var imageWidth by remember { mutableStateOf(0f) }
        var imageHeight by remember { mutableStateOf(0f) }

        // 고정된 상태일 때만 offset 업데이트
        if (localizationViewModel.isFollowing.value) {
            val offsetX = ((0.5f - localizationViewModel.localizationX.value) * imageWidth)

            offset = Offset(
                x = offsetX,
                y = 0.0f
            )
            scale = 1.0f
        }

        Box(
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .height((screenHeight.dp.value * 0.8f).dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x * scale,
                    translationY = offset.y * scale,
                )
                .transformable(state = state)
                .background(Color.White)
        ) {
            // 지도 이미지 표시
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinate ->
                        imageWidth = coordinate.size.width.toFloat()
                        imageHeight = coordinate.size.height.toFloat()
                        dataCollectViewModel.setImageSize(coordinate)
                    },
                contentScale = ContentScale.FillHeight // 이미지 크기를 조정하지 않도록 설정
            )
            // Localization 후 사용자의 위치 표시
            if (localizationViewModel.localizationTested.value) {
                LocalizationTestUserPoint()
            }
        }

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
            val pointX =
                localizationViewModel.localizationX.value * dataCollectViewModel.imageWidth
            val pointY =
                localizationViewModel.localizationY.value * dataCollectViewModel.imageHeight

            val center = Offset(pointX, pointY) // 사용자의 위치
            val radius = 15.dp.toPx()
            val fieldAngle = 60f // 방향성 영역의 각도 (단위: degree)

            // 방향을 나타내는 반투명 영역 (부채꼴)
            drawArc(
                color = Color.Blue.copy(alpha = 0.3f), // 반투명 색상
                startAngle = Math.toDegrees(localizationViewModel.orientation.value.toDouble()).toFloat() - fieldAngle / 2.0f, // 시작 각도
                sweepAngle = fieldAngle, // 부채꼴 각도
                useCenter = true, // 중심점을 포함
                topLeft = Offset(center.x - radius, center.y - radius), // 부채꼴 영역 시작점
                size = androidx.compose.ui.geometry.Size(2 * radius, 2 * radius), // 부채꼴 크기
                style = Fill
            )

            // 사용자의 위치에 파란색 점 그리기
            drawCircle(
                color = Color.Blue,
                radius = 5.dp.toPx(), // 점의 반지름
                center = center
            )

            // 외곽 원 (사용자 위치 강조)
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = 5.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
//                Log.d("accel", "${event.values[0]}, ${event.values[1]}, ${event.values[2]}")
                localizationViewModel.accData.add(Vector3D(event.values[0], event.values[1], event.values[2]))
            }
            Sensor.TYPE_GYROSCOPE -> {
//                Log.d("gyro", "${event.values[0]}, ${event.values[1]}, ${event.values[2]}")
                localizationViewModel.gyroData.add(Vector3D(event.values[0], event.values[1], event.values[2]))
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
//                Log.d("mag", "${event.values[0]}, ${event.values[1]}, ${event.values[2]}")
                localizationViewModel.magData.add(Vector3D(event.values[0], event.values[1], event.values[2]))
            }
            Sensor.TYPE_ORIENTATION -> {
                Log.d("ori", "${event.values[0]}, ${event.values[1]}, ${event.values[2]}")
                localizationViewModel.oriData.add(Vector3D(event.values[0], event.values[1], event.values[2]))
            }
            else -> {
                // DO NOTHING
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
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
