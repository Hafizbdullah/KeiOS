package os.kei.ui.page.main.widget.glass

import android.os.SystemClock
import android.view.Surface
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection

enum class AppFloatingDockSide {
    Start,
    End,
}

internal enum class AppPhysicalDockSide {
    Left,
    Right,
}

@Stable
class AppGripAwareDockState internal constructor(
    private val elapsedRealtimeMs: () -> Long = SystemClock::elapsedRealtime,
) {
    private var selectedPhysicalSide by mutableStateOf(AppPhysicalDockSide.Right)
    private var smoothedHorizontalGravity: Float? = null
    private var sensorCandidate: AppPhysicalDockSide? = null
    private var sensorCandidateSinceMs: Long? = null
    private var confirmedSensorSide: AppPhysicalDockSide? = null
    private var touchCandidate: AppPhysicalDockSide? = null
    private var touchCandidateCount = 0
    private var lastTouchMs: Long? = null

    fun layoutSide(layoutDirection: LayoutDirection): AppFloatingDockSide = resolveLogicalDockSide(selectedPhysicalSide, layoutDirection)

    internal fun onSensorSessionStopped() {
        clearSensorTracking()
        resetTouchMemory()
    }

    internal fun recordSensorGravity(
        gravityX: Float,
        gravityY: Float,
        displayRotation: Int,
    ) {
        val horizontalGravity =
            resolveScreenHorizontalGravity(
                gravityX = gravityX,
                gravityY = gravityY,
                displayRotation = displayRotation,
            ) ?: return
        val previousSmoothedGravity = smoothedHorizontalGravity
        val nextSmoothedGravity =
            if (previousSmoothedGravity == null) {
                horizontalGravity
            } else {
                previousSmoothedGravity * SENSOR_SMOOTHING_RETAIN +
                    horizontalGravity * SENSOR_SMOOTHING_INCOMING
            }
        if (!nextSmoothedGravity.isFinite()) return
        smoothedHorizontalGravity = nextSmoothedGravity

        val nextCandidate =
            resolveSensorCandidate(
                horizontalGravity = nextSmoothedGravity,
                previousCandidate = sensorCandidate,
            )
        if (nextCandidate == null) {
            sensorCandidate = null
            sensorCandidateSinceMs = null
            confirmedSensorSide = null
            return
        }

        val nowMs = elapsedRealtimeMs()
        if (sensorCandidate != nextCandidate) {
            sensorCandidate = nextCandidate
            sensorCandidateSinceMs = nowMs
            return
        }

        val candidateSinceMs = sensorCandidateSinceMs ?: nowMs
        val candidateAgeMs = (nowMs - candidateSinceMs).coerceAtLeast(0L)
        if (candidateAgeMs >= APP_GRIP_AWARE_DOCK_SENSOR_CONFIRM_DELAY_MS) {
            confirmedSensorSide = nextCandidate
            selectPhysicalSide(nextCandidate)
            resetTouchMemory()
        }
    }

    internal fun recordPhysicalTouch(touchSide: AppPhysicalDockSide) {
        if (confirmedSensorSide != null && confirmedSensorSide != touchSide) return

        val nowMs = elapsedRealtimeMs()
        val previousTouchMs = lastTouchMs
        val memoryExpired =
            previousTouchMs == null ||
                nowMs < previousTouchMs ||
                nowMs - previousTouchMs > APP_GRIP_AWARE_DOCK_TOUCH_MEMORY_MS
        if (memoryExpired || touchCandidate != touchSide) {
            touchCandidate = touchSide
            touchCandidateCount = 1
        } else {
            touchCandidateCount += 1
        }
        lastTouchMs = nowMs

        if (
            confirmedSensorSide == null &&
            touchCandidateCount >= APP_GRIP_AWARE_DOCK_TOUCH_CONFIRM_COUNT
        ) {
            selectPhysicalSide(touchSide)
            resetTouchMemory()
        }
    }

    private fun selectPhysicalSide(physicalSide: AppPhysicalDockSide) {
        selectedPhysicalSide = physicalSide
    }

    private fun clearSensorTracking() {
        smoothedHorizontalGravity = null
        sensorCandidate = null
        sensorCandidateSinceMs = null
        confirmedSensorSide = null
    }

    private fun resetTouchMemory() {
        touchCandidate = null
        touchCandidateCount = 0
        lastTouchMs = null
    }
}

internal fun resolveScreenHorizontalGravity(
    gravityX: Float,
    gravityY: Float,
    displayRotation: Int,
): Float? {
    val horizontalGravity =
        when (displayRotation) {
            Surface.ROTATION_0 -> gravityX
            Surface.ROTATION_90 -> -gravityY
            Surface.ROTATION_180 -> -gravityX
            Surface.ROTATION_270 -> gravityY
            else -> return null
        }
    return horizontalGravity.takeIf(Float::isFinite)
}

internal fun resolveLogicalDockSide(
    physicalSide: AppPhysicalDockSide,
    layoutDirection: LayoutDirection,
): AppFloatingDockSide =
    when (physicalSide) {
        AppPhysicalDockSide.Left -> {
            if (layoutDirection == LayoutDirection.Ltr) {
                AppFloatingDockSide.Start
            } else {
                AppFloatingDockSide.End
            }
        }

        AppPhysicalDockSide.Right -> {
            if (layoutDirection == LayoutDirection.Ltr) {
                AppFloatingDockSide.End
            } else {
                AppFloatingDockSide.Start
            }
        }
    }

internal fun resolveGripTouchPhysicalSide(
    positionX: Float,
    positionY: Float,
    containerWidth: Int,
    containerHeight: Int,
): AppPhysicalDockSide? {
    if (
        !positionX.isFinite() ||
        !positionY.isFinite() ||
        containerWidth <= 0 ||
        containerHeight <= 0 ||
        positionX !in 0f..containerWidth.toFloat() ||
        positionY !in 0f..containerHeight.toFloat() ||
        positionY < containerHeight * TOUCH_HOT_ZONE_TOP_FRACTION
    ) {
        return null
    }
    return when {
        positionX < containerWidth * TOUCH_LEFT_EDGE_FRACTION -> AppPhysicalDockSide.Left
        positionX > containerWidth * TOUCH_RIGHT_EDGE_FRACTION -> AppPhysicalDockSide.Right
        else -> null
    }
}

private fun resolveSensorCandidate(
    horizontalGravity: Float,
    previousCandidate: AppPhysicalDockSide?,
): AppPhysicalDockSide? =
    when {
        horizontalGravity <= SENSOR_LEFT_THRESHOLD -> AppPhysicalDockSide.Left
        horizontalGravity >= SENSOR_RIGHT_THRESHOLD -> AppPhysicalDockSide.Right
        horizontalGravity in SENSOR_NEUTRAL_MIN..SENSOR_NEUTRAL_MAX -> null
        else -> previousCandidate
    }

internal const val APP_GRIP_AWARE_DOCK_SENSOR_CONFIRM_DELAY_MS = 620L
internal const val APP_GRIP_AWARE_DOCK_TOUCH_CONFIRM_COUNT = 3
internal const val APP_GRIP_AWARE_DOCK_TOUCH_MEMORY_MS = 1_800L

private const val SENSOR_SMOOTHING_RETAIN = 0.86f
private const val SENSOR_SMOOTHING_INCOMING = 0.14f
private const val SENSOR_LEFT_THRESHOLD = -1.25f
private const val SENSOR_RIGHT_THRESHOLD = 1.25f
private const val SENSOR_NEUTRAL_MIN = -0.62f
private const val SENSOR_NEUTRAL_MAX = 0.62f
private const val TOUCH_HOT_ZONE_TOP_FRACTION = 0.56f
private const val TOUCH_LEFT_EDGE_FRACTION = 0.42f
private const val TOUCH_RIGHT_EDGE_FRACTION = 0.58f
