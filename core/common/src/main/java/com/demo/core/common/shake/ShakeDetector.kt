package com.demo.core.common.shake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

open class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    companion object {
        // 使用 const 关键字，编译期直接内联，性能最优
        const val DEFAULT_THRESHOLD = 3.0f
        const val DEFAULT_INTERVAL_MS = 1000L
    }

    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // 允许通过构造函数调整阈值，但默认使用伴生对象里的配置
    private var threshold = DEFAULT_THRESHOLD
    private var intervalMs = DEFAULT_INTERVAL_MS

    private var lastShakeTime = 0L

    open fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    open fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0] / SensorManager.GRAVITY_EARTH
        val y = event.values[1] / SensorManager.GRAVITY_EARTH
        val z = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(x * x + y * y + z * z) - 1f

        if (gForce > threshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > intervalMs) {
                lastShakeTime = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}