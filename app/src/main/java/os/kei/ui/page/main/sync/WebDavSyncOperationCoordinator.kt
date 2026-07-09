package os.kei.ui.page.main.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes local WebDAV mutations so manual and automatic flows share one ETag timeline.
 */
internal object WebDavSyncOperationCoordinator {
    private val mutex = Mutex()

    suspend fun <T> run(operation: suspend () -> T): T = mutex.withLock { operation() }
}
