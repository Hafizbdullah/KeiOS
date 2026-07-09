package os.kei.core.download.segmented

import java.io.File
import java.io.IOException

data class SegmentedDownloadRequest(
    val url: String,
    val outputFile: File,
    val headers: Map<String, String> = emptyMap(),
    val fileNameHint: String = "",
)

data class SegmentedDownloadOptions(
    val minParallelSizeBytes: Long = 8L * 1024L * 1024L,
    val initialPartSizeBytes: Long = 4L * 1024L * 1024L,
    val maxConnections: Int = 4,
    val maxRetriesPerPart: Int = 3,
    val retryDelayMs: Long = 1_000L,
    val progressIntervalMs: Long = 250L,
    val requireHttpsForParallel: Boolean = true,
    val bufferSizeBytes: Int = DEFAULT_BUFFER_SIZE,
) {
    init {
        require(minParallelSizeBytes > 0L) { "minParallelSizeBytes must be positive" }
        require(initialPartSizeBytes > 0L) { "initialPartSizeBytes must be positive" }
        require(maxConnections > 0) { "maxConnections must be positive" }
        require(maxRetriesPerPart >= 0) { "maxRetriesPerPart cannot be negative" }
        require(retryDelayMs >= 0L) { "retryDelayMs cannot be negative" }
        require(progressIntervalMs >= 0L) { "progressIntervalMs cannot be negative" }
        require(bufferSizeBytes > 0) { "bufferSizeBytes must be positive" }
    }
}

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
    val retryCount: Int = 0,
    val fallbackReason: String? = null,
)

class SegmentedDownloadException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

internal class SegmentedDownloadHttpException(
    val code: Int,
    val retryable: Boolean,
) : IOException("HTTP $code")
