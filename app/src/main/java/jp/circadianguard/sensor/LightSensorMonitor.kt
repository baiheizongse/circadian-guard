package jp.circadianguard.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Handles registration and reception of the ambient light sensor (TYPE_LIGHT).
 * No permission is required. Receives at SENSOR_DELAY_NORMAL (~200 ms) and
 * passes the lux value to a callback.
 */
class LightSensorMonitor(
    context: Context,
    private val onLux: (Float) -> Unit,
    private val onAvailability: (Boolean) -> Unit,
) : SensorEventListener {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    fun start() {
        val sensor = lightSensor
        if (sensor == null) {
            onAvailability(false)
            return
        }
        onAvailability(true)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LIGHT && event.values.isNotEmpty()) {
            onLux(event.values[0])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
