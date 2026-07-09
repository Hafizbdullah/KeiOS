package os.kei.core.download.segmented

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque

internal data class DownloadPart(
    val start: Long,
    val endInclusive: Long,
    val retryCount: Int = 0,
) {
    val length: Long
        get() = endInclusive - start + 1L
}

internal class PartScheduler(
    totalBytes: Long,
    partSizeBytes: Long,
    private val maxRetriesPerPart: Int,
) {
    private val mutex = Mutex()
    private val queue = ArrayDeque<DownloadPart>()
    private var retryCount = 0

    init {
        require(totalBytes > 0L) { "totalBytes must be positive" }
        require(partSizeBytes > 0L) { "partSizeBytes must be positive" }
        require(maxRetriesPerPart >= 0) { "maxRetriesPerPart cannot be negative" }
        buildAlternatingParts(totalBytes, partSizeBytes).forEach(queue::addLast)
    }

    suspend fun nextPart(): DownloadPart? =
        mutex.withLock {
            queue.pollFirst()
        }

    suspend fun requeueFailed(part: DownloadPart, nextStart: Long): Boolean =
        mutex.withLock {
            if (nextStart > part.endInclusive) return@withLock true
            if (part.retryCount >= maxRetriesPerPart) return@withLock false
            retryCount += 1
            queue.addFirst(
                DownloadPart(
                    start = nextStart,
                    endInclusive = part.endInclusive,
                    retryCount = part.retryCount + 1,
                )
            )
            true
        }

    suspend fun retryCount(): Int =
        mutex.withLock { retryCount }

    companion object {
        fun buildAlternatingParts(
            totalBytes: Long,
            partSizeBytes: Long,
        ): List<DownloadPart> {
            require(totalBytes > 0L) { "totalBytes must be positive" }
            require(partSizeBytes > 0L) { "partSizeBytes must be positive" }
            val parts = mutableListOf<DownloadPart>()
            var head = 0L
            var tail = totalBytes - 1L
            var fromHead = true
            while (head <= tail) {
                if (fromHead) {
                    val end = (head + partSizeBytes - 1L).coerceAtMost(tail)
                    parts += DownloadPart(start = head, endInclusive = end)
                    head = end + 1L
                } else {
                    val start = (tail - partSizeBytes + 1L).coerceAtLeast(head)
                    parts += DownloadPart(start = start, endInclusive = tail)
                    tail = start - 1L
                }
                fromHead = !fromHead
            }
            return parts
        }
    }
}
