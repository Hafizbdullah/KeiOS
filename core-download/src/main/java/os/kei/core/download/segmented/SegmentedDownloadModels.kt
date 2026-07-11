package os.kei.core.download.segmented

import java.io.File
import java.io.IOException

data class SegmentedDownloadRequest(
    val url: String,
    val outputFile: File,
    val headers: Map<String, String> = emptyMap(),
    val fileNameHint: String = "",
    val expectedSizeBytes: Long = -1L,
    val expectedSha256: String = "",
) {
    init {
        require(expectedSizeBytes >= -1L) { "expectedSizeBytes must be -1 or non-negative" }
    }
}

data class SegmentedDownloadOptions(
    val minParallelSizeBytes: Long = 8L * 1024L * 1024L,
    val initialPartSizeBytes: Long = 4L * 1024L * 1024L,
    val minBytesPerConnection: Long = initialPartSizeBytes,
    val maxConnections: Int = 4,
    val maxRetriesPerPart: Int = 3,
    val retryDelayMs: Long = 1_000L,
    val progressIntervalMs: Long = 250L,
    val requireHttpsForParallel: Boolean = true,
    val bufferSizeBytes: Int = DEFAULT_SEGMENTED_DOWNLOAD_BUFFER_SIZE_BYTES,
    val writeQueueCapacity: Int = DEFAULT_SEGMENTED_DOWNLOAD_WRITE_QUEUE_CAPACITY,
    val speedProfile: SegmentedDownloadSpeedProfile = SegmentedDownloadSpeedProfile.Balanced,
    val connectionStrategy: SegmentedDownloadConnectionStrategy =
        SegmentedDownloadConnectionStrategy.IsolatedPerWorker,
) {
    init {
        require(minParallelSizeBytes > 0L) { "minParallelSizeBytes must be positive" }
        require(initialPartSizeBytes > 0L) { "initialPartSizeBytes must be positive" }
        require(minBytesPerConnection > 0L) { "minBytesPerConnection must be positive" }
        require(maxConnections > 0) { "maxConnections must be positive" }
        require(maxRetriesPerPart >= 0) { "maxRetriesPerPart cannot be negative" }
        require(retryDelayMs >= 0L) { "retryDelayMs cannot be negative" }
        require(progressIntervalMs >= 0L) { "progressIntervalMs cannot be negative" }
        require(bufferSizeBytes > 0) { "bufferSizeBytes must be positive" }
        require(writeQueueCapacity > 0) { "writeQueueCapacity must be positive" }
    }
}

enum class SegmentedDownloadSpeedProfile {
    Balanced,
    ForegroundBoost,
}

enum class SegmentedDownloadConnectionStrategy {
    Adaptive,
    Shared,
    IsolatedPerWorker,
}

const val DEFAULT_SEGMENTED_DOWNLOAD_BUFFER_SIZE_BYTES: Int = 1024 * 1024
const val DEFAULT_SEGMENTED_DOWNLOAD_WRITE_QUEUE_CAPACITY: Int = 8

data class SegmentedDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val activeConnections: Int,
    val parallel: Boolean,
)

data class SegmentedDownloadResult(
    val outputFile: File,
    val totalBytes: Long,
    val parallel: Boolean,
    val rangeSupported: Boolean,
    val finalUrl: String,
    val workerConnections: Int = 1,
    val peakActiveConnections: Int = 1,
    val retryCount: Int = 0,
    val stealCount: Int = 0,
    val handoffCount: Int = 0,
    val connectionStrategy: SegmentedDownloadConnectionStrategy =
        SegmentedDownloadConnectionStrategy.IsolatedPerWorker,
    val fallbackReason: String? = null,
)

open class SegmentedDownloadException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

internal class SegmentedDownloadHttpException(
    val code: Int,
    val retryable: Boolean,
    val retryAfterMs: Long? = null,
) : IOException("HTTP $code")
