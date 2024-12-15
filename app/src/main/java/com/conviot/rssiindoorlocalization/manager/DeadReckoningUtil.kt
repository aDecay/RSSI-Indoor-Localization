package com.conviot.rssiindoorlocalization.manager

import kotlin.math.sqrt
import kotlin.math.pow
import com.github.psambit9791.jdsp.filter.Butterworth

data class Vector3D(
    val x: Float,
    val y: Float,
    val z: Float
)

data class StepDetectionResult(
    val accelerationNorm: List<Float>,
    val zeroCrossing: List<Int>,
    val peaks: List<Int>,
    val valleys: List<Int>
)

data class StepLengthResult(
    val stepLength: Float,
    val start: Int,
    val middle: Int,
    val end: Int
)

fun filterAcceleration(
    acceleration: List<Vector3D>,
    frequency: Float = 100.0f,
    butterOrder: Int = 2,
    cutoffStepFrequency: Float = 2.0f
): List<Float> {
    // Normalize by gravity
    val accelerationNorm = acceleration.map { innerList ->
        sqrt(innerList.x * innerList.x + innerList.y * innerList.y + innerList.z * innerList.z).toDouble()
    }.toTypedArray().toDoubleArray()

    // Apply Butterworth filter
    val flt = Butterworth(frequency.toDouble())
    val filteredAcceleration = flt.lowPassFilter(accelerationNorm, butterOrder, cutoffStepFrequency.toDouble())

    // Center the acceleration norm around 0
    val mean = filteredAcceleration.average()
    val centeredAcceleration = filteredAcceleration.map { (it - mean).toFloat() }

    return centeredAcceleration.toList()
}

fun detectSteps(
    acceleration: List<Vector3D>,
    accelerationThreshold: Float,
    frequency: Float
): StepDetectionResult {
    val accelerationNorm = filterAcceleration(acceleration, frequency)
    val zeroCrossing = mutableListOf<Int>()

    // Find zero crossings
    for (i in 0 until accelerationNorm.size - 1) {
        if (accelerationNorm[i] * accelerationNorm[i + 1] < 0) {
            zeroCrossing.add(i)
        }
    }

    // Ensure the wave starts with positive to negative transition
    if (accelerationNorm[zeroCrossing[0]] > 0 && accelerationNorm[zeroCrossing[0] + 1] < 0) {
        zeroCrossing.removeAt(0)
    }

    val peaks = mutableListOf<Int>()
    val valleys = mutableListOf<Int>()

    // Find peaks and valleys
    for (i in zeroCrossing.indices step 2) {
        if (i + 1 < zeroCrossing.size) {
            val tPlus = zeroCrossing[i]
            val tMinus = zeroCrossing[i + 1]
            val maxIndex = (tPlus until tMinus).maxByOrNull { accelerationNorm[it] } ?: continue
            peaks.add(maxIndex)
        }
    }

    for (i in 1 until zeroCrossing.size step 2) {
        if (i + 1 < zeroCrossing.size) {
            val tMinus = zeroCrossing[i]
            val tPlus = zeroCrossing[i + 1]
            val minIndex = (tMinus until tPlus).minByOrNull { accelerationNorm[it] } ?: continue
            valleys.add(minIndex)
        }
    }

    // Filter peaks and valleys based on threshold
    val filteredPeaks = peaks.filter { accelerationNorm[it] > accelerationThreshold }
    val filteredValleys = valleys.filter { accelerationNorm[it] < -accelerationThreshold }

    return StepDetectionResult(
        accelerationNorm,
        zeroCrossing,
        filteredPeaks,
        filteredValleys
    )
}

fun computeStepLength(accelMax: Float, accelMin: Float, weinbergGain: Float): Float {
    return weinbergGain * (accelMax - accelMin).pow(1.0f / 4.0f)
}

fun computeStepTimeStamp(accelData: List<Vector3D>, accelerationThreshold: Float, weinbergGain: Float, frequency: Float): List<StepLengthResult> {
    val (accelNorm, zero, peaks, valleys) = detectSteps(accelData, accelerationThreshold, frequency)

    val steps = mutableListOf<StepLengthResult>()

    for ((index, pair) in peaks.zip(valleys).withIndex()) {
        val (maxIdx, minIdx) = pair
        steps.add(
            StepLengthResult(
                computeStepLength(accelNorm[maxIdx], accelNorm[minIdx], weinbergGain),
                zero[2 * index],
                zero[2 * index + 1],
                zero[2 * index + 2]
            )
        )
    }

    return steps
}

fun estimateTurningAngle(gyroData: List<Vector3D>, frequency: Float): Float {
    var totalAngle = 0.0f
    val dt = 1.0f / frequency

    // TODO Improving Accuracy
    for (row in gyroData) {
        totalAngle += row.z * dt
    }

    return totalAngle
}
