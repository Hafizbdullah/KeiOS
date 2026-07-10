package os.kei.core.download.segmented

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

internal fun parseRetryAfterMs(
    value: String?,
    nowEpochMs: Long = System.currentTimeMillis(),
): Long? {
    val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    normalized.toLongOrNull()?.let { seconds ->
        if (seconds < 0L) return null
        return seconds.saturatingMultiply(1_000L)
    }
    val retryAtMs = runCatching {
        ZonedDateTime.parse(normalized, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant()
            .toEpochMilli()
    }.getOrNull() ?: return null
    return (retryAtMs - nowEpochMs).coerceAtLeast(0L)
}

internal fun rangeRetryDelayMs(
    error: Throwable?,
    failureKind: RangeFailureKind,
    retryCounts: RangeRetryCounts,
    baseDelayMs: Long,
): Long {
    val previousRetries = retryCounts.count(failureKind)
    val localDelay =
        when (failureKind) {
            RangeFailureKind.PartialEof -> 0L
            RangeFailureKind.RateLimited -> exponentialDelayMs(
                baseDelayMs = baseDelayMs,
                exponent = previousRetries,
                maxDelayMs = MAX_RATE_LIMIT_RETRY_DELAY_MS,
            )

            RangeFailureKind.Timeout,
            RangeFailureKind.ConnectionReset,
            RangeFailureKind.Transient ->
                if (previousRetries == 0) {
                    0L
                } else {
                    exponentialDelayMs(
                        baseDelayMs = baseDelayMs,
                        exponent = previousRetries - 1,
                        maxDelayMs = MAX_TRANSIENT_RETRY_DELAY_MS,
                    )
                }
        }
    val serverDelay = (error as? SegmentedDownloadHttpException)
        ?.retryAfterMs
        ?.coerceIn(0L, MAX_SERVER_RETRY_AFTER_MS)
        ?: 0L
    return max(localDelay, serverDelay)
}

private fun exponentialDelayMs(
    baseDelayMs: Long,
    exponent: Int,
    maxDelayMs: Long,
): Long {
    if (baseDelayMs <= 0L) return 0L
    val multiplier = 1L shl exponent.coerceIn(0, 5)
    return baseDelayMs
        .saturatingMultiply(multiplier)
        .coerceAtMost(maxDelayMs)
}

private fun Long.saturatingMultiply(multiplier: Long): Long {
    if (this == 0L || multiplier == 0L) return 0L
    if (this > Long.MAX_VALUE / multiplier) return Long.MAX_VALUE
    return this * multiplier
}

private const val MAX_RATE_LIMIT_RETRY_DELAY_MS = 30_000L
private const val MAX_TRANSIENT_RETRY_DELAY_MS = 10_000L
private const val MAX_SERVER_RETRY_AFTER_MS = 120_000L
