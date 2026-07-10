package os.kei.core.download.segmented

import kotlin.math.ceil

internal const val RANGE_LEASE_MIN_EXPECTED_BYTES_PER_SECOND: Long = 64L * 1024L
internal const val RANGE_LEASE_POLL_MS: Long = 500L

internal fun rangeLeaseMs(
    remainingBytes: Long,
    retryCount: Int,
    minExpectedBytesPerSecond: Long = RANGE_LEASE_MIN_EXPECTED_BYTES_PER_SECOND,
): Long {
    require(retryCount >= 0) { "retryCount cannot be negative" }
    require(minExpectedBytesPerSecond > 0L) { "minExpectedBytesPerSecond must be positive" }
    if (remainingBytes <= 0L) return 0L
    if (retryCount == 0 && remainingBytes > MAX_FRESH_LEASE_BYTES) return 0L

    val expectedMs = ceil(
        remainingBytes.toDouble() * 1_000.0 / minExpectedBytesPerSecond.toDouble(),
    ).toLong()
    val retryAdjusted =
        if (retryCount > 0) {
            expectedMs / (retryCount + 1L)
        } else {
            expectedMs
        }
    return retryAdjusted.coerceIn(MIN_RANGE_LEASE_MS, MAX_RANGE_LEASE_MS)
}

internal class RangeLeaseTracker(
    remainingBytes: Long,
    private val retryCount: Int,
    private val minExpectedBytesPerSecond: Long = RANGE_LEASE_MIN_EXPECTED_BYTES_PER_SECOND,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private val lock = Any()
    private var remainingBytes = remainingBytes.coerceAtLeast(0L)
    private var lastProgressAtMs = nowMs()

    fun recordProgress(remainingBytes: Long) {
        synchronized(lock) {
            this.remainingBytes = remainingBytes.coerceAtLeast(0L)
            lastProgressAtMs = nowMs()
        }
    }

    fun millisUntilExpiration(): Long =
        synchronized(lock) {
            if (remainingBytes <= 0L) return@synchronized Long.MAX_VALUE
            val leaseMs = rangeLeaseMs(
                remainingBytes = remainingBytes,
                retryCount = retryCount,
                minExpectedBytesPerSecond = minExpectedBytesPerSecond,
            )
            if (leaseMs <= 0L) return@synchronized Long.MAX_VALUE
            val elapsedMs = (nowMs() - lastProgressAtMs).coerceAtLeast(0L)
            (leaseMs - elapsedMs).coerceAtLeast(0L)
        }

    fun isExpired(): Boolean = millisUntilExpiration() == 0L
}

private const val MIN_RANGE_LEASE_MS = 4_000L
private const val MAX_RANGE_LEASE_MS = 30_000L
private const val MAX_FRESH_LEASE_BYTES = 2L * 1024L * 1024L
