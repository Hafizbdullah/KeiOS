package os.kei.core.download.segmented

internal const val RATE_PROBE_IDLE_TIMEOUT_MS = 1_000L
internal const val RATE_PROBE_IDLE_POLL_MS = 100L

internal class RateProbeIdleTracker(
    private val timeoutMs: Long = RATE_PROBE_IDLE_TIMEOUT_MS,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private val lock = Any()
    private var lastProgressAtMs = nowMs()

    init {
        require(timeoutMs > 0L) { "timeoutMs must be positive" }
    }

    fun recordProgress() {
        synchronized(lock) {
            lastProgressAtMs = nowMs()
        }
    }

    fun millisUntilExpiration(): Long =
        synchronized(lock) {
            val elapsedMs = (nowMs() - lastProgressAtMs).coerceAtLeast(0L)
            (timeoutMs - elapsedMs).coerceAtLeast(0L)
        }
}
