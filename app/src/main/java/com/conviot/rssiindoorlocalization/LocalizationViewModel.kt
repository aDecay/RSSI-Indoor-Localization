import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.conviot.rssiindoorlocalization.manager.Vector3D
import kotlin.math.pow
import kotlin.math.sqrt

/** Localization 로직에서 변수 저장을 담당하는 ViewModel */
class LocalizationViewModel : ViewModel() {
    // X
    private val _localizationX = mutableFloatStateOf(0.25f)
    val localizationX: State<Float> get() = _localizationX

    // Y
    private val _localizationY = mutableFloatStateOf(0.25f)
    val localizationY: State<Float> get() = _localizationY

    // Orientation (Radian)
    private var _orientation = mutableFloatStateOf(0f)
    val orientation: State<Float> get() = _orientation

    // Localization 여부
    private val _localizationTested = mutableStateOf(true)
    val localizationTested: State<Boolean> get() = _localizationTested

    // 화면 고정 여부
    private val _isFollowing = mutableStateOf(true)
    val isFollowing: State<Boolean> get() = _isFollowing

    fun setIsFollowing(isFollowing: Boolean) {
        _isFollowing.value = isFollowing
    }

    // 랜드마크 정보
    class Landmark(var x: Float, var y: Float, var radius: Float, var name: String) {}

    // 랜드마크 리스트
    private val _landmarkList = arrayOf<Landmark>(
        Landmark(0.9388f, 0.2935f, 0.0854f, "정문"),
        Landmark(0.7564f, 0.7176f, 0.1246f, "엘리베이터(정문)"),
        Landmark(0.4000f, 0.1944f, 0.0574f, "엘리베이터(화장실)"),
        Landmark(0.2472f, 0.9268f, 0.0778f, "엘리베이터(후문)"),
        Landmark(0.6518f, 0.1314f, 0.1384f, "카페"),
        Landmark(0.1805f, 0.6694f, 0.0680f, "후문"),
        Landmark(0.4398f, 0.5481f, 0.0546f, "에스컬레이터"),
        Landmark(0.0037f, 0.3962f, 0.1344f, "소공연장"),
        Landmark(0.3000f, 0.1370f, 0.0473f, "자판기")
    )

    // 사용자가 위치한 랜드마크
    private val _currentLandmark = mutableStateOf<Landmark?>(null)
    val currentLandmark: State<Landmark?> get() = _currentLandmark

    // IMU Raw Data
    val accData = mutableListOf<Vector3D>()
    val gyroData = mutableListOf<Vector3D>()
    val magData = mutableListOf<Vector3D>()

    /** 사용자의 위치를 업데이트 */
    fun updateLocalization(x: Float, y: Float) {
        _localizationX.floatValue = x.coerceIn(0.0f, 1.0f)
        _localizationY.floatValue = y.coerceIn(0.0f, 1.0f)
        _localizationTested.value = true
    }

    fun addLocalization(x: Float, y: Float) {
        _localizationX.floatValue = (_localizationX.floatValue + x).coerceIn(0.0f, 1.0f)
        _localizationY.floatValue = (_localizationY.floatValue + y).coerceIn(0.0f, 1.0f)
    }

    /** 사용자의 위치를 0으로 초기화 */
    fun resetLocalization() {
        _localizationX.floatValue = 0f
        _localizationY.floatValue = 0f
        _localizationTested.value = false
    }

    /** 사용자가 랜드마크에 위치해있는지 확인 후 설정 */
    fun checkLandmark() {
        // localizationX, localizationY가 업데이트된 후에만 호출 가능하도록 설정
        if (!_localizationTested.value) {
            return
        }

        for (landmark: Landmark in _landmarkList) {
            val distance = sqrt(
                (landmark.x - localizationX.value * (3962f / 9228)).pow(2) + (landmark.y - localizationY.value).pow(2)
            )

            if (distance <= landmark.radius) {
                _currentLandmark.value = landmark
            }
        }
    }

    fun addOrientation(radian: Float) {
        _orientation.value += radian
        _orientation.value %= 2 * Math.PI.toFloat()
    }
}
