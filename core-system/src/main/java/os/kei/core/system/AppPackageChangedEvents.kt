package os.kei.core.system

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class AppPackageChangedEvent(
    val packageName: String,
    val action: String,
    val atMillis: Long = System.currentTimeMillis(),
    val replacing: Boolean = false,
    val uid: Int = -1,
    val dataRemoved: Boolean = false,
    val userInitiated: Boolean = false,
    val archival: Boolean = false,
)

object AppPackageChangedEvents {
    private val _events = MutableSharedFlow<AppPackageChangedEvent>(
        replay = 0,
        extraBufferCapacity = 32,
    )

    val events: SharedFlow<AppPackageChangedEvent> = _events.asSharedFlow()

    fun publish(event: AppPackageChangedEvent) {
        _events.tryEmit(event)
    }
}
