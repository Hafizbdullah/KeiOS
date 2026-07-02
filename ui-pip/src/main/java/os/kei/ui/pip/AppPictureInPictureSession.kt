package os.kei.ui.pip

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

const val APP_PIP_EXTRA_SESSION_ID = "os.kei.ui.pip.extra.SESSION_ID"
const val APP_PIP_NO_SESSION_ID = 0L
const val APP_PIP_ACTION_URI_SCHEME = "keios-pip"

object AppPictureInPictureSessionIds {
    private val nextSessionId =
        AtomicLong(
            SystemClock.elapsedRealtimeNanos()
                .takeIf { value -> value > APP_PIP_NO_SESSION_ID }
                ?: 1L
        )

    fun next(): Long {
        return nextSessionId
            .getAndIncrement()
            .takeIf { value -> value != APP_PIP_NO_SESSION_ID }
            ?: nextSessionId.getAndIncrement()
    }
}
