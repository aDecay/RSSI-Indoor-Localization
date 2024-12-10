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
import coil3.compose.AsyncImage
import com.conviot.rssiindoorlocalization.manager.Vector3D
import com.conviot.rssiindoorlocalization.manager.computeStepTimeStamp
import com.conviot.rssiindoorlocalization.manager.estimateTurningAngle
import com.conviot.rssiindoorlocalization.ui.theme.RSSIIndoorLocalizationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import kotlin.math.cos
import kotlin.math.sin

/** RSSI 데이터를 통해 사용자의 위치를 확인하는 Activity */
class LocalizationActivity : ComponentActivity(), SensorEventListener {
    // Thread
    private var localizationJob: Job? = null
    private var deadReckoningJob: Job? = null

    // ViewModel
    private lateinit var dataCollectViewModel: DataCollectViewModel
    private lateinit var localizationViewModel: LocalizationViewModel

    // Localization 설정
    private var isTest: Boolean = false // 테스트 여부 (true면, 특정 record_id 기준으로 실행)
    private val localizationDelayMs: Long = 3000 // localization 간격 (ms)
    private val deadReckoningDelayMs: Long = 100

    // Sensor
    private lateinit var sensorManager: SensorManager
    private lateinit var accelSensor: Sensor
    private lateinit var gyroSensor: Sensor
    private lateinit var magSensor: Sensor
    private val samplingPeriodUs = 10000

    private var accelerationThreshold = 0.1f
    private var weinbergGain = 0.65f
    private var frequency = 100.0f

    //private var wifiListTime = mutableListOf<FloatArray>()

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
        sensorManager.registerListener(this, accelSensor, samplingPeriodUs)
        sensorManager.registerListener(this, gyroSensor, samplingPeriodUs)
        sensorManager.registerListener(this, magSensor, samplingPeriodUs)

        enableEdgeToEdge()
        setContent {
            RSSIIndoorLocalizationTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    DataCollector()
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

        // TODO 3초마다 WIFI + DR 통합
        // Activity가 Create된 후, Start되면 Coroutine 시작
//        localizationJob = CoroutineScope(Dispatchers.Main).launch {
//            while (isActive) {
//                // Localization 실행
//                localization()
//
//                // 일정 주기마다 반복
//                delay(localizationDelayMs)
//            }
//        }
        deadReckoningJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                // Dead Reckoning 실행
                val steps = computeStepTimeStamp(localizationViewModel.accData, accelerationThreshold, weinbergGain, frequency)

                if (steps.isNotEmpty()) {
                    // 사용자의 위치를 업데이트
                    for (step in steps) {
                        localizationViewModel.addOrientation(
                            -estimateTurningAngle(localizationViewModel.gyroData.subList(step.start, step.end), frequency)
                        )

                        val dx = step.stepLength / 0.6f / 153.0f * cos(localizationViewModel.orientation.value.toDouble())
                        val dy = step.stepLength / 0.6f / 65.0f * sin(localizationViewModel.orientation.value.toDouble())

                        localizationViewModel.addLocalization(
                            dx.toFloat(),
                            dy.toFloat()
                        )
                    }

                    localizationViewModel.accData.clear()
                    localizationViewModel.gyroData.clear()
                    localizationViewModel.magData.clear()

                    Log.d(
                        "TestLocalization",
                        "X: ${localizationViewModel.localizationX}, Y: ${localizationViewModel.localizationY}"
                    )
                } else {
                    // No Step Dectected
                }

                delay(deadReckoningDelayMs)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelSensor.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
        gyroSensor.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
        magSensor.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
    }

    override fun onStop() {
        super.onStop()

        sensorManager.unregisterListener(this)

        // Activity가 Stop되면, Coroutine 종료
        localizationJob?.cancel()
        deadReckoningJob?.cancel()

        // 파일 경로 및 이름 설정
        val fileName = "localization_data.csv"
        val resolver = applicationContext.contentResolver

        // ContentValues 설정
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) // Download 폴더에 저장
        }

        // 파일 생성
        val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.writer().use { writer ->
                    writer.append("x,y,rad\n") // 헤더 작성
                    localizationViewModel.DRResult.forEach { (x, y, rad) ->
                        writer.append("$x,$y,$rad\n") // 데이터 작성
                    }
                }
                outputStream.flush()
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

                // 사용자 위치 설정
                localizationViewModel.updateLocalization(x, y)

                // 랜드마크 확인
                localizationViewModel.checkLandmark()

                Log.d(
                    "TestLocalization",
                    "X: ${x}, Y: ${y}, Landmark Name: ${localizationViewModel.currentLandmark.value?.name}"
                )
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
        }

        Box(
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .height((screenHeight.dp.value * 0.8f).dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
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
        if (localizationViewModel.currentLandmark.value != null) {
            Column(
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp)
            ) {
                // 랜드마크 이름 표시
                Text(
                    localizationViewModel.currentLandmark.value!!.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
