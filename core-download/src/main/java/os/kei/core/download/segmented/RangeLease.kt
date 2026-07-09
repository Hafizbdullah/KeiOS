package os.kei.core.download.segmented

import kotlin.math.max

internal fun rangeLeaseMs(
    part: DownloadPart,
    initialPartSizeBytes: Long,
): Long {
    if (part.retryCount == 0 && part.length > MIN_DYNAMIC_LEASE_PART_BYTES) return 0L
    if (part.length > initialPartSizeBytes * 4L) return 0L

    var leaseMs = RANGE_LEASE_MS
    if (part.length <= MIN_DYNAMIC_LEASE_PART_BYTES) {
        leaseMs = RANGE_LEASE_MS / 2L
    }
    if (part.retryCount > 0) {
        leaseMs /= (part.retryCount + 1L)
    }
    return max(MIN_RANGE_LEASE_MS, leaseMs)
}

private const val RANGE_LEASE_MS = 8_000L
private const val MIN_RANGE_LEASE_MS = 2_000L
private const val MIN_DYNAMIC_LEASE_PART_BYTES = 512L * 1024L
