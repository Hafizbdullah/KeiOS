package os.kei.ui.pip

import android.app.Activity
import android.os.Handler
import android.os.Looper

class AppPictureInPictureCloseController(
    private val activity: Activity,
    private val fallbackDelayMs: Long,
    private val shouldFinishStoppedHost: () -> Boolean,
    private val shouldFinishExitedPictureInPicture: () -> Boolean = shouldFinishStoppedHost,
    private val onBeforeFinish: (reason: String) -> Unit,
    private val onAfterFinish: (reason: String) -> Unit = {},
    private val onLog: (message: String) -> Unit = {},
) {
    private val handler = Handler(Looper.getMainLooper())
    private var closeRequested = false
    private val stoppedFallbackRunnable =
        Runnable {
            if (closeRequested || activity.isFinishing || activity.isDestroyed) return@Runnable
            if (!shouldFinishStoppedHost()) return@Runnable
            requestClose(
                removeTask = true,
                reason = APP_PIP_CLOSE_REASON_STOPPED_FALLBACK,
            )
        }
    private val exitedPictureInPictureFallbackRunnable =
        Runnable {
            if (closeRequested || activity.isFinishing || activity.isDestroyed) return@Runnable
            if (!shouldFinishExitedPictureInPicture()) return@Runnable
            requestClose(
                removeTask = true,
                reason = APP_PIP_CLOSE_REASON_EXITED_PIP_FALLBACK,
            )
        }

    val isCloseRequested: Boolean
        get() = closeRequested

    fun cancelStoppedFallback() {
        handler.removeCallbacks(stoppedFallbackRunnable)
        handler.removeCallbacks(exitedPictureInPictureFallbackRunnable)
    }

    fun scheduleStoppedFallback() {
        cancelStoppedFallback()
        handler.postDelayed(stoppedFallbackRunnable, fallbackDelayMs)
    }

    fun scheduleExitedPictureInPictureFallback() {
        cancelStoppedFallback()
        handler.postDelayed(exitedPictureInPictureFallbackRunnable, fallbackDelayMs)
    }

    fun requestClose(
        removeTask: Boolean,
        reason: String,
    ) {
        if (closeRequested && activity.isFinishing) return
        closeRequested = true
        cancelStoppedFallback()
        onLog("close reason=$reason removeTask=$removeTask inPip=${activity.isInPictureInPictureMode}")
        onBeforeFinish(reason)
        if (activity.isInPictureInPictureMode) {
            activity.finish()
        } else if (removeTask) {
            runCatching { activity.finishAndRemoveTask() }
                .onFailure { activity.finish() }
        } else {
            activity.finish()
        }
        onAfterFinish(reason)
    }
}

const val APP_PIP_CLOSE_REASON_APP_ACTION = "app_action"
const val APP_PIP_CLOSE_REASON_HOST_CLOSE = "host_close"
const val APP_PIP_CLOSE_REASON_REPLACE_ACTIVE = "replace_active"
const val APP_PIP_CLOSE_REASON_STOPPED_FALLBACK = "stopped_fallback"
const val APP_PIP_CLOSE_REASON_EXITED_PIP_FALLBACK = "exited_pip_fallback"
