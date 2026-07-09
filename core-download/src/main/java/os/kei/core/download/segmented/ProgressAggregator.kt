package os.kei.core.download.segmented

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

internal class ProgressAggregator(
    private val totalBytes: Long,
    private val activeConnections: Int,
    private val parallel: Boolean,
    private val intervalMs: Long,
    private val onProgress: suspend (SegmentedDownloadProgress) -> Unit,
) {
    private val downloaded = AtomicLong(0L)
    private val mutex = Mutex()
    private var lastEmitMs = Long.MIN_VALUE
    private var lastEmittedBytes = Long.MIN_VALUE

    val downloadedBytes: Long
        get() = downloaded.get()

    suspend fun addBytes(bytes: Long) {
        if (bytes <= 0L) return
        downloaded.addAndGet(bytes)
        emit(force = false)
    }

    suspend fun emit(force: Boolean) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val current = downloaded.get()
            val isComplete = totalBytes > 0L && current >= totalBytes
            val due = intervalMs == 0L || lastEmitMs == Long.MIN_VALUE || now - lastEmitMs >= intervalMs
            if (!force && !isComplete && !due) return@withLock
            if (current == lastEmittedBytes && (!force || isComplete)) return@withLock
            lastEmitMs = now
            lastEmittedBytes = current
            onProgress(
                SegmentedDownloadProgress(
                    downloadedBytes = current,
                    totalBytes = totalBytes,
                    activeConnections = activeConnections,
                    parallel = parallel,
                )
            )
        }
    }
}
