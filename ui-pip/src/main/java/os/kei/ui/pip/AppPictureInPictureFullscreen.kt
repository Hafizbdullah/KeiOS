package os.kei.ui.pip

import android.app.Activity
import android.os.OutcomeReceiver

fun Activity.requestAppPictureInPictureFullscreen(
    onResult: () -> Unit,
    onError: (Throwable) -> Unit,
) {
    runCatching {
        requestFullscreenMode(
            Activity.FULLSCREEN_MODE_REQUEST_ENTER,
            object : OutcomeReceiver<Void, Throwable> {
                override fun onResult(result: Void?) {
                    runOnUiThread(onResult)
                }

                override fun onError(error: Throwable) {
                    runOnUiThread {
                        onError(error)
                    }
                }
            },
        )
    }.onFailure { error ->
        onError(error)
    }
}
