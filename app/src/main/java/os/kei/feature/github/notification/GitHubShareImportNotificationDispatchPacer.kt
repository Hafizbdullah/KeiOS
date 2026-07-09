package os.kei.feature.github.notification

/**
 * Keeps GitHub install updates below the platform's per-package notification enqueue limit.
 */
internal class GitHubShareImportNotificationDispatchPacer(
    private val minimumIntervalMs: Long,
) {
    private var lastDispatchElapsedRealtimeMs: Long? = null

    fun delayUntilReady(atElapsedRealtimeMs: Long): Long {
        val lastDispatch = lastDispatchElapsedRealtimeMs ?: return 0L
        return (minimumIntervalMs - (atElapsedRealtimeMs - lastDispatch)).coerceAtLeast(0L)
    }

    fun markDispatched(atElapsedRealtimeMs: Long) {
        lastDispatchElapsedRealtimeMs = atElapsedRealtimeMs
    }
}
