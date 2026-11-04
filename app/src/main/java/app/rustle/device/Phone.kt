package app.rustle.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

/** The phone as his house: how warm the stove is, how light it is, whether the walls are shaking. */
@Immutable
data class Phone(
    val level: Int = 60,
    val charging: Boolean = false,
    val tempC: Float? = null,
    /** Lux from the light sensor; null on phones without one. */
    val lux: Float? = null,
    /** Last time the phone was shaken hard. */
    val shookAt: Long = 0,
    /** Lying screen down, flat. */
    val faceDown: Boolean = false,
    /** Share of storage in use, 0..1: the attic gets dusty. */
    val storage: Float = 0f,
) {
    val low get() = level <= 15 && !charging
    val hot get() = (tempC ?: 0f) >= 41f
    val dark get() = lux != null && lux < 4f
    val sunny get() = lux != null && lux > 6000f
    fun dizzy(now: Long) = now - shookAt < 4000
}

object Device {
    fun battery(ctx: Context): Phone {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return Phone()
        return fromBattery(i, Phone(storage = storage()))
    }

    fun fromBattery(i: Intent, p: Phone): Phone {
        val lvl = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        return p.copy(
            level = if (lvl >= 0) lvl * 100 / scale else p.level,
            charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL,
            tempC = if (temp != Int.MIN_VALUE) temp / 10f else null,
        )
    }

    fun storage(): Float = runCatching {
        val s = StatFs(Environment.getDataDirectory().path)
        1f - s.availableBytes.toFloat() / s.totalBytes.toFloat()
    }.getOrDefault(0f)

    fun hasAccelerometer(ctx: Context) = (ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
}

/** Watches the battery, the light and the accelerometer while the screen is shown. [still] fixes a state for renders. */
@Composable
fun rememberPhone(still: Phone? = null): Phone {
    if (still != null) return still
    val ctx = LocalContext.current
    var phone by remember { mutableStateOf(Device.battery(ctx)) }
    DisposableEffect(Unit) {
        val rcv = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { phone = Device.fromBattery(i, phone) }
        }
        ContextCompat.registerReceiver(ctx, rcv, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        var downSince = 0L
        val lis = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_LIGHT -> {
                        val lux = e.values[0]
                        // Only redraw on a real change: the sensor jitters.
                        val old = phone.lux
                        if (old == null || kotlin.math.abs(lux - old) > maxOf(2f, old * .25f)) phone = phone.copy(lux = lux)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
                        val g = sqrt(x * x + y * y + z * z)
                        val now = System.currentTimeMillis()
                        if (g > 24f) phone = phone.copy(shookAt = now)
                        val down = z < -8.6f && kotlin.math.abs(x) < 2.5f && kotlin.math.abs(y) < 2.5f
                        if (down) { if (downSince == 0L) downSince = now } else downSince = 0L
                        val fd = down && now - downSince > 1200
                        if (fd != phone.faceDown) phone = phone.copy(faceDown = fd)
                    }
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        sm.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { sm.registerListener(lis, it, SensorManager.SENSOR_DELAY_NORMAL) }
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm.registerListener(lis, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose {
            runCatching { ctx.unregisterReceiver(rcv) }
            sm.unregisterListener(lis)
        }
    }
    return phone
}
