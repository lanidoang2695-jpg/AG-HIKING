package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2

data class CompassData(
    val azimuthDegrees: Float = 0f,
    val cardinal: String = "U",
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val isSensorAvailable: Boolean = true
)

class CompassSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _compassData = MutableStateFlow(CompassData(isSensorAvailable = hasSensors()))
    val compassData: StateFlow<CompassData> = _compassData.asStateFlow()

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var currentFilteredAzimuth = 0f
    private val alpha = 0.15f // low pass filter smoothing

    fun hasSensors(): Boolean {
        return rotationVectorSensor != null || (accelerometerSensor != null && magneticSensor != null)
    }

    fun startListening() {
        if (sensorManager == null) return
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magneticSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientation)
            updateAzimuth(Math.toDegrees(orientation[0].toDouble()).toFloat(), event.accuracy)
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, gravity, 0, 3)
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, geomagnetic, 0, 3)
            }

            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                SensorManager.getOrientation(rotationMatrix, orientation)
                updateAzimuth(Math.toDegrees(orientation[0].toDouble()).toFloat(), event.accuracy)
            }
        }
    }

    private fun updateAzimuth(rawAzimuth: Float, accuracy: Int) {
        val normalized = (rawAzimuth + 360f) % 360f

        // Handle 0/360 wrap-around smoothing
        var diff = normalized - currentFilteredAzimuth
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f

        currentFilteredAzimuth = (currentFilteredAzimuth + alpha * diff + 360f) % 360f

        val cardinal = when (currentFilteredAzimuth) {
            in 22.5f..67.5f -> "TL" // NE
            in 67.5f..112.5f -> "T"  // E
            in 112.5f..157.5f -> "TG" // SE
            in 157.5f..202.5f -> "S"  // S
            in 202.5f..247.5f -> "BD" // SW
            in 247.5f..292.5f -> "B"  // W
            in 292.5f..337.5f -> "BL" // NW
            else -> "U" // N
        }

        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

        _compassData.value = CompassData(
            azimuthDegrees = currentFilteredAzimuth,
            cardinal = cardinal,
            pitch = pitch,
            roll = roll,
            accuracy = accuracy,
            isSensorAvailable = true
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _compassData.value = _compassData.value.copy(accuracy = accuracy)
    }
}
