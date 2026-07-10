package os.kei.core.download.segmented

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.math.min

internal enum class RangeFailureKind {
    PartialEof,
    Timeout,
    ConnectionReset,
    RateLimited,
    Transient,
}

internal data class RangeRetryCounts(
    val partialEof: Int = 0,
    val timeout: Int = 0,
    val connectionReset: Int = 0,
    val rateLimited: Int = 0,
    val transient: Int = 0,
) {
    fun count(kind: RangeFailureKind): Int =
        when (kind) {
            RangeFailureKind.PartialEof -> partialEof
            RangeFailureKind.Timeout -> timeout
            RangeFailureKind.ConnectionReset -> connectionReset
            RangeFailureKind.RateLimited -> rateLimited
            RangeFailureKind.Transient -> transient
        }

    fun increment(kind: RangeFailureKind): RangeRetryCounts =
        when (kind) {
            RangeFailureKind.PartialEof -> copy(partialEof = partialEof + 1)
            RangeFailureKind.Timeout -> copy(timeout = timeout + 1)
            RangeFailureKind.ConnectionReset -> copy(connectionReset = connectionReset + 1)
            RangeFailureKind.RateLimited -> copy(rateLimited = rateLimited + 1)
            RangeFailureKind.Transient -> copy(transient = transient + 1)
        }
}

internal data class RangeRetryBudgets(
    val partialEof: Int,
    val timeout: Int,
    val connectionReset: Int,
    val rateLimited: Int,
    val transient: Int,
) {
    init {
        require(partialEof >= 0) { "partialEof retry budget cannot be negative" }
        require(timeout >= 0) { "timeout retry budget cannot be negative" }
        require(connectionReset >= 0) { "connectionReset retry budget cannot be negative" }
        require(rateLimited >= 0) { "rateLimited retry budget cannot be negative" }
        require(transient >= 0) { "transient retry budget cannot be negative" }
    }

    fun budget(kind: RangeFailureKind): Int =
        when (kind) {
            RangeFailureKind.PartialEof -> partialEof
            RangeFailureKind.Timeout -> timeout
            RangeFailureKind.ConnectionReset -> connectionReset
            RangeFailureKind.RateLimited -> rateLimited
            RangeFailureKind.Transient -> transient
        }

    companion object {
        fun fromBase(maxRetriesPerPart: Int): RangeRetryBudgets {
            require(maxRetriesPerPart >= 0) { "maxRetriesPerPart cannot be negative" }
            if (maxRetriesPerPart == 0) {
                return RangeRetryBudgets(0, 0, 0, 0, 0)
            }
            return RangeRetryBudgets(
                partialEof = max(maxRetriesPerPart * 4, 8),
                timeout = max(maxRetriesPerPart * 8, 16),
                connectionReset = max(maxRetriesPerPart * 8, 16),
                rateLimited = max(maxRetriesPerPart * 16, 64),
                transient = max(maxRetriesPerPart * 8, 16),
            )
        }
    }
}

internal data class DownloadPart(
    val start: Long,
    val endInclusive: Long,
    val retryCount: Int = 0,
    val retryCounts: RangeRetryCounts = RangeRetryCounts(),
    val rateProbe: Boolean = false,
) {
    val length: Long
        get() = endInclusive - start + 1L
}

internal data class PartSchedulerTuning(
    val minDynamicPartSizeBytes: Long = 512L * 1024L,
    val minTailPartSizeBytes: Long = 128L * 1024L,
    val maxDynamicPartSizeBytes: Long = 1L * 1024L * 1024L * 1024L,
    val tailPartsPerConnection: Int = 4,
    val tailWindowInitialMultiplier: Int = 8,
    val tailWindowMinDynamicMultiplier: Int = 2,
    val speedSmoothFactor: Double = 0.35,
    val partSizeTargetDurationMs: Long = 16_000L,
    val startupActiveConnections: Int = 4,
    val rateLimitMinActiveConnections: Int = 2,
    val rateLimitStrikeThreshold: Int = 2,
    val rateLimitWindowMs: Long = 30_000L,
    val rateLimitCooldownMs: Long = 10_000L,
    val rateLimitRecoveryMs: Long = 15_000L,
    val idlePollMs: Long = 50L,
) {
    init {
        require(minDynamicPartSizeBytes > 0L) { "minDynamicPartSizeBytes must be positive" }
        require(minTailPartSizeBytes > 0L) { "minTailPartSizeBytes must be positive" }
        require(maxDynamicPartSizeBytes > 0L) { "maxDynamicPartSizeBytes must be positive" }
        require(tailPartsPerConnection > 0) { "tailPartsPerConnection must be positive" }
        require(tailWindowInitialMultiplier >= 0) { "tailWindowInitialMultiplier cannot be negative" }
        require(tailWindowMinDynamicMultiplier >= 0) { "tailWindowMinDynamicMultiplier cannot be negative" }
        require(speedSmoothFactor in 0.0..1.0) { "speedSmoothFactor must be between 0 and 1" }
        require(partSizeTargetDurationMs > 0L) { "partSizeTargetDurationMs must be positive" }
        require(startupActiveConnections > 0) { "startupActiveConnections must be positive" }
        require(rateLimitMinActiveConnections > 0) { "rateLimitMinActiveConnections must be positive" }
        require(rateLimitStrikeThreshold > 0) { "rateLimitStrikeThreshold must be positive" }
        require(rateLimitWindowMs > 0L) { "rateLimitWindowMs must be positive" }
        require(rateLimitCooldownMs >= 0L) { "rateLimitCooldownMs cannot be negative" }
        require(rateLimitRecoveryMs > 0L) { "rateLimitRecoveryMs must be positive" }
        require(idlePollMs > 0L) { "idlePollMs must be positive" }
    }
}

internal class ActiveDownloadPart internal constructor(
    val workerId: Int,
    val part: DownloadPart,
    val startedMs: Long,
) {
    private val lock = Any()
    private var nextOffset: Long = part.start
    private var cancelAttempt: (() -> Unit)? = null

    fun currentOffset(): Long =
        synchronized(lock) { nextOffset }

    fun currentEndInclusive(): Long =
        part.endInclusive

    fun completedBytes(): Long =
        synchronized(lock) { (nextOffset - part.start).coerceAtLeast(0L) }

    fun isComplete(): Boolean =
        synchronized(lock) { nextOffset > part.endInclusive }

    fun advanceTo(offset: Long) {
        synchronized(lock) {
            nextOffset = max(nextOffset, offset)
        }
    }

    fun setCancelAttempt(cancel: () -> Unit) {
        synchronized(lock) {
            cancelAttempt = cancel
        }
    }

    fun clearCancelAttempt() {
        synchronized(lock) {
            cancelAttempt = null
        }
    }

    fun cancelAttempt() {
        val cancel =
            synchronized(lock) {
                cancelAttempt
            }
        cancel?.invoke()
    }
}

internal data class PartSchedulerStats(
    val retryCount: Int,
    val stealCount: Int,
    val handoffCount: Int,
)

internal class PartScheduler(
    totalBytes: Long,
    private val initialPartSizeBytes: Long,
    maxRetriesPerPart: Int,
    private val concurrency: Int,
    private val tuning: PartSchedulerTuning = PartSchedulerTuning(),
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val retryBudgets: RangeRetryBudgets = RangeRetryBudgets.fromBase(maxRetriesPerPart),
) {
    private val mutex = Mutex()
    private val queue = ArrayDeque<DownloadPart>()
    private val delayed = mutableListOf<DelayedPart>()
    private val activeParts = arrayOfNulls<ActiveDownloadPart>(concurrency)
    private val workerDone = IntArray(concurrency)
    private val workerSpeedBytesPerMs = DoubleArray(concurrency)
    private val workerPartSizeBytes = LongArray(concurrency) { initialPartSizeBytes }
    private val maxDynamicPartSizeBytes: Long
    private val rateLimitFloor = min(concurrency, tuning.rateLimitMinActiveConnections)
    private var activeCount = 0
    private var maxActive = min(concurrency, tuning.startupActiveConnections)
    private var front = 0L
    private var back = totalBytes - 1L
    private var sequence = 0
    private var retryCount = 0
    private var rateLimited = false
    private var rateLimitStrikes = 0
    private var lastRateLimitAtMs = Long.MIN_VALUE
    private var recoverAtMs = Long.MAX_VALUE
    private var probeLimit = 0

    init {
        require(totalBytes > 0L) { "totalBytes must be positive" }
        require(initialPartSizeBytes > 0L) { "initialPartSizeBytes must be positive" }
        require(maxRetriesPerPart >= 0) { "maxRetriesPerPart cannot be negative" }
        require(concurrency > 0) { "concurrency must be positive" }
        maxDynamicPartSizeBytes = min(
            totalBytes,
            max(tuning.maxDynamicPartSizeBytes, initialPartSizeBytes)
                .coerceAtMost(initialPartSizeBytes.saturatingMultiply(256L)),
        )
    }

    val idlePollMs: Long
        get() = tuning.idlePollMs

    suspend fun nextPart(workerId: Int = 0): ActiveDownloadPart? =
        mutex.withLock {
            if (workerId !in activeParts.indices) return@withLock null
            recoverRateLimitLocked(nowMs())
            if (activeParts[workerId] != null || activeCount >= maxActive) return@withLock null
            moveReadyDelayedLocked()
            val queuedPart =
                when {
                    queue.isNotEmpty() -> queue.removeLast()
                    front <= back -> allocateNextPartLocked(workerId)
                    else -> null
                }
                    ?: return@withLock null
            val part = queuedPart.copy(rateProbe = shouldMarkRateProbeLocked())
            val active = ActiveDownloadPart(
                workerId = workerId,
                part = part,
                startedMs = nowMs(),
            )
            activeParts[workerId] = active
            activeCount += 1
            active
        }

    suspend fun finish(
        workerId: Int,
        active: ActiveDownloadPart,
    ) {
        mutex.withLock {
            if (workerId in activeParts.indices && activeParts[workerId] === active) {
                activeParts[workerId] = null
                activeCount = (activeCount - 1).coerceAtLeast(0)
            }
        }
    }

    suspend fun hasInFlight(): Boolean =
        mutex.withLock {
            moveReadyDelayedLocked()
            queue.isNotEmpty() ||
                delayed.isNotEmpty() ||
                front <= back ||
                activeParts.any { it != null }
        }

    suspend fun requeueFailed(
        part: DownloadPart,
        nextStart: Long,
        failureKind: RangeFailureKind,
        delayMs: Long = 0L,
    ): Boolean =
        mutex.withLock {
            if (nextStart > part.endInclusive) return@withLock true
            if (part.retryCounts.count(failureKind) >= retryBudgets.budget(failureKind)) {
                return@withLock false
            }
            retryCount += 1
            val failedPart = part.copy(
                start = nextStart,
                retryCount = part.retryCount + 1,
                retryCounts = part.retryCounts.increment(failureKind),
                rateProbe = false,
            )
            if (delayMs > 0L) {
                delayed += DelayedPart(
                    part = failedPart,
                    readyAtMs = nowMs() + delayMs,
                )
                return@withLock true
            }
            enqueueRetryChunksLocked(failedPart)
            true
        }

    suspend fun recordSuccess(
        workerId: Int,
        part: DownloadPart,
        bytes: Long,
        elapsedMs: Long,
    ) {
        mutex.withLock {
            if (workerId in workerPartSizeBytes.indices && bytes > 0L && elapsedMs > 0L) {
                val speed = bytes.toDouble() / elapsedMs.toDouble()
                workerSpeedBytesPerMs[workerId] =
                    if (workerSpeedBytesPerMs[workerId] == 0.0) {
                        speed
                    } else {
                        workerSpeedBytesPerMs[workerId] * (1.0 - tuning.speedSmoothFactor) +
                            speed * tuning.speedSmoothFactor
                    }
                adjustPartSizeLocked(workerId, bytes, elapsedMs)
                workerDone[workerId] += 1
            }
            if (part.rateProbe && probeLimit == maxActive) {
                probeLimit = 0
                if (maxActive >= concurrency) {
                    rateLimited = false
                }
            }
            if (!rateLimited && maxActive < concurrency) {
                maxActive += 1
            }
        }
    }

    suspend fun recordRateLimit(
        part: DownloadPart,
        delayMs: Long,
    ) {
        mutex.withLock {
            val now = nowMs()
            normalizeMaxActiveLocked()
            val rejectedProbe = part.rateProbe && probeLimit == maxActive
            if (rejectedProbe) {
                if (maxActive > rateLimitFloor) {
                    maxActive -= 1
                }
                probeLimit = 0
                extendRecoveryLocked(now, max(delayMs, tuning.rateLimitRecoveryMs))
            }
            if (lastRateLimitAtMs == Long.MIN_VALUE || now - lastRateLimitAtMs > tuning.rateLimitWindowMs) {
                rateLimitStrikes = 0
            }
            lastRateLimitAtMs = now
            rateLimitStrikes += 1
            if (
                !rejectedProbe &&
                rateLimitStrikes >= tuning.rateLimitStrikeThreshold &&
                maxActive > rateLimitFloor
            ) {
                maxActive -= 1
                probeLimit = 0
                rateLimitStrikes = tuning.rateLimitStrikeThreshold - 1
            }
            rateLimited = true
            extendRecoveryLocked(now, max(delayMs, tuning.rateLimitCooldownMs))
        }
    }

    suspend fun penalize(workerId: Int) {
        if (workerId !in workerPartSizeBytes.indices) return
        mutex.withLock {
            workerPartSizeBytes[workerId] =
                max(workerPartSizeBytes[workerId] / 2L, tuning.minDynamicPartSizeBytes)
        }
    }

    suspend fun stats(): PartSchedulerStats =
        mutex.withLock {
            PartSchedulerStats(
                retryCount = retryCount,
                stealCount = 0,
                handoffCount = 0,
            )
        }

    private fun allocateNextPartLocked(workerId: Int): DownloadPart {
        val remaining = back - front + 1L
        val partSize = nextPartSizeLocked(workerId, remaining)
        sequence += 1
        return if (sequence % 2 == 0) {
            val end = back
            val start = max(front, end - partSize + 1L)
            back = start - 1L
            DownloadPart(start = start, endInclusive = end)
        } else {
            val start = front
            val end = min(back, start + partSize - 1L)
            front = end + 1L
            DownloadPart(start = start, endInclusive = end)
        }
    }

    private fun nextPartSizeLocked(
        workerId: Int,
        remaining: Long,
    ): Long {
        if (remaining > tailWindowBytes()) {
            return min(remaining, workerPartSizeLocked(workerId))
        }
        val targetParts = maxActive.toLong() * tuning.tailPartsPerConnection.toLong()
        val partSize = ceilDiv(remaining, targetParts)
        return clampPartSize(
            size = partSize,
            remaining = remaining,
            maxPartSize = initialPartSizeBytes,
            minPartSize = tuning.minTailPartSizeBytes,
        )
    }

    private fun enqueueRetryChunksLocked(part: DownloadPart) {
        val length = part.length
        val chunkSize = max(length / 2L, initialPartSizeBytes)
            .coerceAtLeast(tuning.minDynamicPartSizeBytes)
            .coerceAtMost(length)
        val chunks = mutableListOf<DownloadPart>()
        var start = part.start
        while (start <= part.endInclusive) {
            val end = min(start + chunkSize - 1L, part.endInclusive)
            chunks += part.copy(start = start, endInclusive = end)
            start = end + 1L
        }
        for (index in chunks.indices.reversed()) {
            queue.addLast(chunks[index])
        }
    }

    private fun moveReadyDelayedLocked() {
        if (delayed.isEmpty()) return
        val now = nowMs()
        val iterator = delayed.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.readyAtMs <= now) {
                queue.addLast(item.part)
                iterator.remove()
            }
        }
    }

    private fun recoverRateLimitLocked(now: Long) {
        normalizeMaxActiveLocked()
        if (!rateLimited || maxActive >= concurrency || now < recoverAtMs) return
        maxActive += 1
        probeLimit = maxActive
        recoverAtMs = now + tuning.rateLimitRecoveryMs
    }

    private fun shouldMarkRateProbeLocked(): Boolean =
        probeLimit == maxActive &&
            probeLimit > rateLimitFloor &&
            activeCount >= probeLimit - 1

    private fun normalizeMaxActiveLocked() {
        maxActive = maxActive.coerceIn(1, concurrency)
    }

    private fun extendRecoveryLocked(
        now: Long,
        delayMs: Long,
    ) {
        val candidate = now.saturatingAdd(delayMs.coerceAtLeast(0L))
        if (recoverAtMs == Long.MAX_VALUE || candidate > recoverAtMs) {
            recoverAtMs = candidate
        }
    }

    private fun adjustPartSizeLocked(
        workerId: Int,
        bytes: Long,
        elapsedMs: Long,
    ) {
        val current = workerPartSizeLocked(workerId)
        if (bytes < min(initialPartSizeBytes, current / 2L)) return
        var target = (bytes.toDouble() / elapsedMs.toDouble() * tuning.partSizeTargetDurationMs.toDouble()).toLong()
        target = clampPartSize(
            size = target,
            remaining = maxDynamicPartSizeBytes,
            maxPartSize = maxDynamicPartSizeBytes,
            minPartSize = tuning.minDynamicPartSizeBytes,
        )
        workerPartSizeBytes[workerId] =
            when {
                workerDone[workerId] == 0 -> target
                target > current -> min(target, current.saturatingMultiply(4L))
                target < current / 2L -> max(target, current / 2L)
                else -> (current + target) / 2L
            }
    }

    private fun tailWindowBytes(): Long =
        max(
            initialPartSizeBytes.saturatingMultiply(tuning.tailWindowInitialMultiplier.toLong()),
            maxActive.toLong()
                .saturatingMultiply(tuning.minDynamicPartSizeBytes)
                .saturatingMultiply(tuning.tailWindowMinDynamicMultiplier.toLong()),
        )

    private fun workerPartSizeLocked(workerId: Int): Long =
        if (workerId in workerPartSizeBytes.indices) {
            workerPartSizeBytes[workerId].takeIf { it > 0L } ?: initialPartSizeBytes
        } else {
            initialPartSizeBytes
        }

    private data class DelayedPart(
        val part: DownloadPart,
        val readyAtMs: Long,
    )
}

private fun clampPartSize(
    size: Long,
    remaining: Long,
    maxPartSize: Long,
    minPartSize: Long,
): Long =
    size
        .coerceAtLeast(minPartSize)
        .coerceAtMost(maxPartSize)
        .coerceAtMost(remaining)

private fun ceilDiv(
    value: Long,
    divisor: Long,
): Long =
    if (divisor <= 0L) {
        value
    } else {
        (value + divisor - 1L) / divisor
    }

private fun Long.saturatingMultiply(multiplier: Long): Long {
    if (this == 0L || multiplier == 0L) return 0L
    if (this > Long.MAX_VALUE / multiplier) return Long.MAX_VALUE
    return this * multiplier
}

private fun Long.saturatingAdd(value: Long): Long {
    if (value <= 0L) return this
    if (this > Long.MAX_VALUE - value) return Long.MAX_VALUE
    return this + value
}
