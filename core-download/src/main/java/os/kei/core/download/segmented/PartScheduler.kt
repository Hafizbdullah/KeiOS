package os.kei.core.download.segmented

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max
import kotlin.math.min

internal data class DownloadPart(
    val start: Long,
    val endInclusive: Long,
    val retryCount: Int = 0,
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
    val minStealPartSizeBytes: Long = 64L * 1024L,
    val minStealAgeMs: Long = 200L,
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
        require(minStealPartSizeBytes > 0L) { "minStealPartSizeBytes must be positive" }
        require(minStealAgeMs >= 0L) { "minStealAgeMs cannot be negative" }
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
    private var endInclusive: Long = part.endInclusive
    private var cancelAttempt: (() -> Unit)? = null

    fun currentOffset(): Long =
        synchronized(lock) { nextOffset }

    fun currentEndInclusive(): Long =
        synchronized(lock) { endInclusive }

    fun completedBytes(): Long =
        synchronized(lock) { (nextOffset - part.start).coerceAtLeast(0L) }

    fun isComplete(): Boolean =
        synchronized(lock) { nextOffset > endInclusive }

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

    fun writeAvailable(
        byteCount: Int,
        writer: (position: Long, byteCount: Int) -> Unit,
    ): Int =
        synchronized(lock) {
            if (nextOffset > endInclusive) return@synchronized 0
            val writable = min(byteCount.toLong(), endInclusive - nextOffset + 1L).toInt()
            if (writable <= 0) return@synchronized 0
            val position = nextOffset
            writer(position, writable)
            nextOffset += writable
            writable
        }

    internal fun remainingSnapshot(): ActivePartRemaining =
        synchronized(lock) {
            ActivePartRemaining(
                offset = nextOffset,
                endInclusive = endInclusive,
                remainingBytes = (endInclusive - nextOffset + 1L).coerceAtLeast(0L),
            )
        }

    internal fun splitForSteal(minStealPartSizeBytes: Long): StolenDownloadPart? {
        var shouldCancel = false
        val stolen =
            synchronized(lock) {
                val remaining = endInclusive - nextOffset + 1L
                if (remaining < minStealPartSizeBytes) return@synchronized null
                val oldEnd = endInclusive
                val handoff = remaining < minStealPartSizeBytes * 2L
                val stolenStart = if (handoff) nextOffset else nextOffset + remaining / 2L
                endInclusive = stolenStart - 1L
                shouldCancel = handoff
                DownloadPart(
                    start = stolenStart,
                    endInclusive = oldEnd,
                    retryCount = part.retryCount + 1,
                )
            }
                ?: return null
        if (shouldCancel) {
            cancelAttempt()
        }
        return StolenDownloadPart(
            part = stolen,
            handoff = shouldCancel,
        )
    }
}

internal data class ActivePartRemaining(
    val offset: Long,
    val endInclusive: Long,
    val remainingBytes: Long,
)

internal data class StolenDownloadPart(
    val part: DownloadPart,
    val handoff: Boolean,
)

internal data class PartSchedulerStats(
    val retryCount: Int,
    val stealCount: Int,
    val handoffCount: Int,
)

internal class PartScheduler(
    totalBytes: Long,
    private val initialPartSizeBytes: Long,
    private val maxRetriesPerPart: Int,
    private val concurrency: Int,
    private val tuning: PartSchedulerTuning = PartSchedulerTuning(),
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private val mutex = Mutex()
    private val queue = ArrayDeque<DownloadPart>()
    private val delayed = mutableListOf<DelayedPart>()
    private val activeParts = arrayOfNulls<ActiveDownloadPart>(concurrency)
    private val workerDone = IntArray(concurrency)
    private val workerSpeedBytesPerMs = DoubleArray(concurrency)
    private val workerPartSizeBytes = LongArray(concurrency) { initialPartSizeBytes }
    private val maxDynamicPartSizeBytes: Long
    private var front = 0L
    private var back = totalBytes - 1L
    private var sequence = 0
    private var retryCount = 0
    private var stealCount = 0
    private var handoffCount = 0

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

    suspend fun nextPart(workerId: Int = 0): DownloadPart? =
        mutex.withLock {
            moveReadyDelayedLocked()
            if (queue.isNotEmpty()) {
                return@withLock queue.removeLast()
            }
            if (front > back) {
                return@withLock splitLargestActiveLocked(workerId)
            }
            allocateNextPartLocked(workerId)
        }

    suspend fun activate(
        workerId: Int,
        part: DownloadPart,
    ): ActiveDownloadPart =
        mutex.withLock {
            val active = ActiveDownloadPart(
                workerId = workerId,
                part = part,
                startedMs = nowMs(),
            )
            if (workerId in activeParts.indices) {
                activeParts[workerId] = active
            }
            active
        }

    suspend fun finish(
        workerId: Int,
        active: ActiveDownloadPart,
    ) {
        mutex.withLock {
            if (workerId in activeParts.indices && activeParts[workerId] === active) {
                activeParts[workerId] = null
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
        delayMs: Long = 0L,
    ): Boolean =
        mutex.withLock {
            if (nextStart > part.endInclusive) return@withLock true
            if (part.retryCount >= maxRetriesPerPart) return@withLock false
            retryCount += 1
            val failedPart = part.copy(
                start = nextStart,
                retryCount = part.retryCount + 1,
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

    suspend fun record(
        workerId: Int,
        bytes: Long,
        elapsedMs: Long,
    ) {
        if (workerId !in workerPartSizeBytes.indices || bytes <= 0L || elapsedMs <= 0L) return
        mutex.withLock {
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
                stealCount = stealCount,
                handoffCount = handoffCount,
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
        val targetParts = concurrency.toLong() * tuning.tailPartsPerConnection.toLong()
        val partSize = ceilDiv(remaining, targetParts)
        return clampPartSize(
            size = partSize,
            remaining = remaining,
            maxPartSize = initialPartSizeBytes,
            minPartSize = tuning.minTailPartSizeBytes,
        )
    }

    private fun splitLargestActiveLocked(workerId: Int): DownloadPart? {
        val now = nowMs()
        val chosen = activeParts
            .withIndex()
            .asSequence()
            .filter { (index, active) -> index != workerId && active != null }
            .mapNotNull { (_, active) ->
                active ?: return@mapNotNull null
                val ageMs = now - active.startedMs
                val remaining = active.remainingSnapshot()
                if (ageMs < tuning.minStealAgeMs || remaining.remainingBytes < tuning.minStealPartSizeBytes) {
                    null
                } else {
                    active to remaining.remainingBytes
                }
            }
            .maxByOrNull { it.second }
            ?.first
            ?: return null

        val stolen = chosen.splitForSteal(tuning.minStealPartSizeBytes) ?: return null
        stealCount += 1
        if (stolen.handoff) {
            handoffCount += 1
        }
        return stolen.part
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
            concurrency.toLong()
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
