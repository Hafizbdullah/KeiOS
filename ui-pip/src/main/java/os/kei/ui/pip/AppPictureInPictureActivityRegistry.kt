package os.kei.ui.pip

import android.app.Activity
import java.lang.ref.WeakReference

class AppPictureInPictureActivityRegistry<T : Activity> {
    private var activeActivity: WeakReference<T>? = null

    fun replaceActive(
        activity: T,
        shouldClose: (T) -> Boolean,
        close: (T) -> Unit,
    ) {
        activeActivity
            ?.get()
            ?.takeIf { current -> current !== activity && shouldClose(current) }
            ?.let(close)
        activeActivity = WeakReference(activity)
    }

    fun clear(activity: T) {
        if (activeActivity?.get() === activity) {
            activeActivity = null
        }
    }

    fun closeActive(
        shouldClose: (T) -> Boolean,
        close: (T) -> Unit,
    ) {
        val current = activeActivity
            ?.get()
            ?.takeIf(shouldClose)
            ?: return
        activeActivity = null
        close(current)
    }
}
