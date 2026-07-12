package os.kei.ui.page.main.widget.glass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
fun rememberAppGripAwareDockState(enabled: Boolean): AppGripAwareDockState {
    val context = LocalContext.current
    val view = LocalView.current
    val state = remember(enabled) { AppGripAwareDockState() }
    val sensorManager =
        remember(context.applicationContext) {
            context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        }
    val sensor =
        remember(sensorManager) {
            sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

    LifecycleResumeEffect(enabled, sensorManager, sensor, view, state) {
        if (!enabled || sensorManager == null || sensor == null) {
            state.onSensorSessionStopped()
            onPauseOrDispose {
                state.onSensorSessionStopped()
            }
        } else {
            val listener =
                object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        state.recordSensorGravity(
                            gravityX = event.values.getOrNull(0) ?: Float.NaN,
                            gravityY = event.values.getOrNull(1) ?: Float.NaN,
                            displayRotation = currentDisplayRotation(view) ?: return,
                        )
                    }

                    override fun onAccuracyChanged(
                        sensor: Sensor?,
                        accuracy: Int,
                    ) = Unit
                }
            val registered =
                sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_UI,
                )
            if (!registered) state.onSensorSessionStopped()

            onPauseOrDispose {
                if (registered) sensorManager.unregisterListener(listener, sensor)
                state.onSensorSessionStopped()
            }
        }
    }
    return state
}

fun Modifier.appGripAwareDockTouchObserver(
    enabled: Boolean,
    state: AppGripAwareDockState,
): Modifier {
    if (!enabled) return this
    return pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val down =
                    event.changes.firstOrNull { change ->
                        change.pressed && !change.previousPressed
                    } ?: continue
                resolveGripTouchPhysicalSide(
                    positionX = down.position.x,
                    positionY = down.position.y,
                    containerWidth = size.width,
                    containerHeight = size.height,
                )?.let(state::recordPhysicalTouch)
            }
        }
    }
}

internal fun currentDisplayRotation(view: View): Int? =
    runCatching { view.display?.rotation }
        .getOrNull()
