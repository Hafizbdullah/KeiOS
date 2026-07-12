package os.kei.core.download.segmented

internal const val TAIL_IDLE_WINDOW_BYTES: Long = 2L * 1024L * 1024L
internal const val TAIL_IDLE_TIMEOUT_MS: Long = 1_000L
internal const val TAIL_IDLE_POLL_MS: Long = 100L

internal class TailIdleTracker(
    private val timeoutMs: Long = TAIL_IDLE_TIMEOUT_MS,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private val lock = Any()
    private var active = false
    private var lastProgressAtMs = 0L

    init {
        require(timeoutMs > 0L) { "timeoutMs must be positive" }
    }

    fun activate() {
        synchronized(lock) {
            if (!active) {
                active = true
                lastProgressAtMs = nowMs()
            }
        }
    }

    fun recordProgress() {
        synchronized(lock) {
            if (active) lastProgressAtMs = nowMs()
        }
    }

    fun millisUntilExpiration(): Long =
        synchronized(lock) {
            if (!active) return@synchronized Long.MAX_VALUE
            val elapsedMs = (nowMs() - lastProgressAtMs).coerceAtLeast(0L)
            (timeoutMs - elapsedMs).coerceAtLeast(0L)
        }
}
