import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

/** Localization 로직에서 변수 저장을 담당하는 ViewModel */
class LocalizationViewModel : ViewModel() {
    private val _localizationX = mutableFloatStateOf(0f)
    val localizationX: State<Float> get() = _localizationX

    private val _localizationY = mutableFloatStateOf(0f)
    val localizationY: State<Float> get() = _localizationY

    private val _localizationTested = mutableStateOf(false)
    val localizationTested: State<Boolean> get() = _localizationTested

    fun updateLocalization(x: Float, y: Float) {
        _localizationX.floatValue = x
        _localizationY.floatValue = y
        _localizationTested.value = true
    }

    fun resetLocalization() {
        _localizationX.floatValue = 0f
        _localizationY.floatValue = 0f
        _localizationTested.value = false
    }
}
