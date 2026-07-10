package os.kei.core.download.segmented

internal data class RangeSpeedSnapshot(
    val averageBytesPerMs: Double,
    val measuredPeerCount: Int,
)

internal class RangeSpeedTracker {
    private val lock = Any()
    private var nextId = 0L
    private val activeSpeeds = mutableMapOf<Long, Double>()

    fun register(): Long =
        synchronized(lock) {
            nextId += 1L
            activeSpeeds[nextId] = 0.0
            nextId
        }

    fun unregister(id: Long) {
        synchronized(lock) {
            activeSpeeds.remove(id)
        }
    }

    fun update(
        id: Long,
        speedBytesPerMs: Double,
    ): RangeSpeedSnapshot =
        synchronized(lock) {
            if (speedBytesPerMs > 0.0) {
                activeSpeeds[id] = speedBytesPerMs
            }
            val measured = activeSpeeds.values.filter { it > 0.0 }
            if (measured.isEmpty()) {
                RangeSpeedSnapshot(averageBytesPerMs = 0.0, measuredPeerCount = 0)
            } else {
                RangeSpeedSnapshot(
                    averageBytesPerMs = measured.average(),
                    measuredPeerCount = measured.size,
                )
            }
        }
}

internal fun shouldCloseSlowConnection(
    speedBytesPerMs: Double,
    averageBytesPerMs: Double,
    measuredPeerCount: Int,
    ageMs: Long,
    bytes: Long,
    remainingBytes: Long = Long.MAX_VALUE,
): Boolean {
    val relativelySlow =
        measuredPeerCount >= SLOW_CONNECTION_MIN_PEERS &&
        ageMs >= SLOW_CONNECTION_MIN_AGE_MS &&
        bytes >= SLOW_CONNECTION_MIN_BYTES &&
        averageBytesPerMs > 0.0 &&
        speedBytesPerMs > 0.0 &&
        speedBytesPerMs < averageBytesPerMs * SLOW_CONNECTION_RATIO
    val absolutelySlowTail =
        remainingBytes in 1..SLOW_TAIL_MAX_REMAINING_BYTES &&
            ageMs >= SLOW_TAIL_MIN_AGE_MS &&
            bytes >= SLOW_TAIL_MIN_BYTES &&
            speedBytesPerMs > 0.0 &&
            speedBytesPerMs < SLOW_TAIL_MIN_BYTES_PER_MS
    return relativelySlow || absolutelySlowTail
}

internal const val SLOW_CONNECTION_CHECK_INTERVAL_MS = 2_000L
internal const val SLOW_CONNECTION_MIN_AGE_MS = 5_000L
internal const val SLOW_CONNECTION_MIN_BYTES = 1024L * 1024L
internal const val SLOW_CONNECTION_STRIKES = 2
internal const val SLOW_CONNECTION_MIN_PEERS = 4
internal const val SLOW_CONNECTION_RATIO = 0.45
internal const val SLOW_TAIL_MAX_REMAINING_BYTES = 2L * 1024L * 1024L
internal const val SLOW_TAIL_MIN_AGE_MS = 4_000L
internal const val SLOW_TAIL_MIN_BYTES = 256L * 1024L
internal const val SLOW_TAIL_MIN_BYTES_PER_MS = 64.0 * 1024.0 / 1_000.0
