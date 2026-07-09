package os.kei.core.download.segmented

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.math.ceil
import kotlin.math.min

class SegmentedDownloadClient(
    private val client: OkHttpClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val probe = RangeProbe(client)

    suspend fun downloadToFile(
        request: SegmentedDownloadRequest,
        options: SegmentedDownloadOptions = SegmentedDownloadOptions(),
        onProgress: suspend (SegmentedDownloadProgress) -> Unit = {},
    ): SegmentedDownloadResult =
        withContext(dispatcher) {
            require(request.url.isNotBlank()) { "url must not be blank" }
            val outputFile = request.outputFile
            outputFile.parentFile?.mkdirs()
            val partFile = outputFile.partFile()
            runCatching { partFile.delete() }
            try {
                val probeResult = probe.probe(request)
                val parallelDecision = resolveParallelDecision(probeResult, options)
                val result =
                    if (parallelDecision.parallel) {
                        downloadParallel(
                            request = request,
                            options = options,
                            probeResult = probeResult,
                            outputFile = outputFile,
                            partFile = partFile,
                            onProgress = onProgress,
                        )
                    } else {
                        downloadSingleStream(
                            request = request,
                            options = options,
                            probeResult = probeResult,
                            outputFile = outputFile,
                            partFile = partFile,
                            fallbackReason = parallelDecision.reason,
                            onProgress = onProgress,
                        )
                    }
                result
            } catch (error: Throwable) {
                runCatching { partFile.delete() }
                if (error is CancellationException) throw error
                throw error
            }
        }

    private suspend fun downloadParallel(
        request: SegmentedDownloadRequest,
        options: SegmentedDownloadOptions,
        probeResult: RangeProbeResult,
        outputFile: File,
        partFile: File,
        onProgress: suspend (SegmentedDownloadProgress) -> Unit,
    ): SegmentedDownloadResult {
        val totalBytes = probeResult.totalBytes
        val effectiveConnections = effectiveConnections(totalBytes, options)
        val scheduler = PartScheduler(
            totalBytes = totalBytes,
            partSizeBytes = options.initialPartSizeBytes,
            maxRetriesPerPart = options.maxRetriesPerPart,
        )
        val progress = ProgressAggregator(
            totalBytes = totalBytes,
            activeConnections = effectiveConnections,
            parallel = true,
            intervalMs = options.progressIntervalMs,
            onProgress = onProgress,
        )
        progress.emit(force = true)
        RandomAccessFile(partFile, "rw").use { randomFile ->
            randomFile.setLength(totalBytes)
            randomFile.channel.use { channel ->
                coroutineScope {
                    repeat(effectiveConnections) {
                        launch {
                            runWorker(
                                request = request,
                                options = options,
                                scheduler = scheduler,
                                channel = channel,
                                totalBytes = totalBytes,
                                progress = progress,
                            )
                        }
                    }
                }
                channel.force(true)
            }
        }
        validateLength(partFile, totalBytes)
        movePartToOutput(partFile, outputFile)
        progress.emit(force = true)
        return SegmentedDownloadResult(
            outputFile = outputFile,
            totalBytes = totalBytes,
            parallel = true,
            rangeSupported = true,
            finalUrl = probeResult.finalUrl,
            retryCount = scheduler.retryCount(),
        )
    }

    private suspend fun runWorker(
        request: SegmentedDownloadRequest,
        options: SegmentedDownloadOptions,
        scheduler: PartScheduler,
        channel: FileChannel,
        totalBytes: Long,
        progress: ProgressAggregator,
    ) {
        while (true) {
            currentCoroutineContext().ensureActive()
            val part = scheduler.nextPart() ?: return
            val outcome = runCatching {
                downloadPart(
                    request = request,
                    options = options,
                    part = part,
                    channel = channel,
                    totalBytes = totalBytes,
                    progress = progress,
                )
            }
            if (outcome.isSuccess) continue
            val error = outcome.exceptionOrNull()
            if (error is CancellationException) throw error
            val writtenBytes = (error as? PartialPartDownloadException)?.writtenBytes ?: 0L
            val retryable = error.isRetryableDownloadError()
            val requeued = retryable && scheduler.requeueFailed(part, part.start + writtenBytes)
            if (!requeued) {
                throw SegmentedDownloadException(
                    message = "part failed ${part.start}-${part.endInclusive}",
                    cause = error,
                )
            }
            if (error is SegmentedDownloadHttpException && error.code == 429 && options.retryDelayMs > 0L) {
                delay(options.retryDelayMs)
            }
        }
    }

    private suspend fun downloadPart(
        request: SegmentedDownloadRequest,
        options: SegmentedDownloadOptions,
        part: DownloadPart,
        channel: FileChannel,
        totalBytes: Long,
        progress: ProgressAggregator,
    ) {
        val rangeHeader = "bytes=${part.start}-${part.endInclusive}"
        val rangeRequest = request.newRequestBuilder()
            .header("Range", rangeHeader)
            .get()
            .build()
        var written = 0L
        executeStreaming(rangeRequest) { response ->
            if (response.code != 206) {
                throw SegmentedDownloadHttpException(
                    code = response.code,
                    retryable = response.code == 429 || response.code in 500..599,
                )
            }
            val contentRange = parseContentRange(response.header("Content-Range"))
                ?: throw IOException("missing Content-Range")
            if (
                contentRange.start != part.start ||
                contentRange.end != part.endInclusive ||
                contentRange.totalBytes != totalBytes
            ) {
                throw IOException("unexpected Content-Range ${response.header("Content-Range").orEmpty()}")
            }
            val buffer = ByteArray(options.bufferSizeBytes)
            response.body.byteStream().use { input ->
                while (written < part.length) {
                    currentCoroutineContext().ensureActive()
                    val remaining = part.length - written
                    val read = input.read(buffer, 0, min(buffer.size.toLong(), remaining).toInt())
                    if (read < 0) break
                    writeAt(channel, part.start + written, buffer, read)
                    written += read
                    progress.addBytes(read.toLong())
                }
            }
        }
        if (written != part.length) {
            throw PartialPartDownloadException(
                expectedBytes = part.length,
                writtenBytes = written,
            )
        }
    }

    private suspend fun downloadSingleStream(
        request: SegmentedDownloadRequest,
        options: SegmentedDownloadOptions,
        probeResult: RangeProbeResult,
        outputFile: File,
        partFile: File,
        fallbackReason: String?,
        onProgress: suspend (SegmentedDownloadProgress) -> Unit,
    ): SegmentedDownloadResult {
        val singleRequest = request.newRequestBuilder().get().build()
        var downloaded = 0L
        var finalUrl = probeResult.finalUrl
        var totalBytes = probeResult.totalBytes
        executeStreaming(singleRequest) { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            finalUrl = response.request.url.toString()
            totalBytes = response.body.contentLength().takeIf { it > 0L }
                ?: probeResult.totalBytes
            val progress = ProgressAggregator(
                totalBytes = totalBytes,
                activeConnections = 1,
                parallel = false,
                intervalMs = options.progressIntervalMs,
                onProgress = onProgress,
            )
            progress.emit(force = true)
            response.body.byteStream().use { input ->
                FileOutputStream(partFile).use { output ->
                    val buffer = ByteArray(options.bufferSizeBytes)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        progress.addBytes(read.toLong())
                    }
                    output.flush()
                }
            }
            progress.emit(force = true)
        }
        if (totalBytes > 0L) validateLength(partFile, totalBytes)
        movePartToOutput(partFile, outputFile)
        return SegmentedDownloadResult(
            outputFile = outputFile,
            totalBytes = totalBytes.takeIf { it > 0L } ?: downloaded,
            parallel = false,
            rangeSupported = probeResult.rangeSupported,
            finalUrl = finalUrl,
            fallbackReason = fallbackReason,
        )
    }

    private fun resolveParallelDecision(
        probeResult: RangeProbeResult,
        options: SegmentedDownloadOptions,
    ): ParallelDecision {
        if (!probeResult.rangeSupported) {
            return ParallelDecision(parallel = false, reason = probeResult.fallbackReason ?: "range-unavailable")
        }
        if (probeResult.totalBytes < options.minParallelSizeBytes) {
            return ParallelDecision(parallel = false, reason = "below-threshold")
        }
        if (options.requireHttpsForParallel && !probeResult.finalUrl.startsWith("https://", ignoreCase = true)) {
            return ParallelDecision(parallel = false, reason = "non-https")
        }
        return ParallelDecision(parallel = true, reason = null)
    }

    private fun effectiveConnections(
        totalBytes: Long,
        options: SegmentedDownloadOptions,
    ): Int {
        val partCount = ceil(totalBytes.toDouble() / options.initialPartSizeBytes.toDouble()).toInt().coerceAtLeast(1)
        return min(options.maxConnections, partCount).coerceAtLeast(1)
    }

    private fun writeAt(
        channel: FileChannel,
        position: Long,
        buffer: ByteArray,
        byteCount: Int,
    ) {
        var written = 0
        synchronized(channel) {
            while (written < byteCount) {
                written += channel.write(
                    ByteBuffer.wrap(buffer, written, byteCount - written),
                    position + written,
                )
            }
        }
    }

    private fun validateLength(file: File, expectedBytes: Long) {
        val actual = file.length()
        if (actual != expectedBytes) {
            throw IOException("download length mismatch expected=$expectedBytes actual=$actual")
        }
    }

    private fun movePartToOutput(partFile: File, outputFile: File) {
        runCatching {
            Files.move(
                partFile.toPath(),
                outputFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }.recoverCatching { error ->
            if (error is AtomicMoveNotSupportedException) {
                Files.move(
                    partFile.toPath(),
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } else {
                throw error
            }
        }.getOrElse { error ->
            throw IOException("rename failed ${partFile.absolutePath} -> ${outputFile.absolutePath}", error)
        }
    }

    private fun Throwable?.isRetryableDownloadError(): Boolean =
        when (this) {
            is PartialPartDownloadException -> true
            is SegmentedDownloadHttpException -> retryable
            is IOException -> true
            else -> false
        }

    private fun File.partFile(): File =
        File(parentFile ?: File("."), "$name.part")

    private suspend fun <T> executeStreaming(
        request: Request,
        block: suspend (Response) -> T,
    ): T {
        val call = client.newCall(request)
        val cancellationHandle = currentCoroutineContext()[Job]?.invokeOnCompletion { error ->
            if (error is CancellationException) {
                call.cancel()
            }
        }
        return try {
            call.execute().use { response ->
                block(response)
            }
        } finally {
            cancellationHandle?.dispose()
        }
    }
}

private data class ParallelDecision(
    val parallel: Boolean,
    val reason: String?,
)

private class PartialPartDownloadException(
    val expectedBytes: Long,
    val writtenBytes: Long,
) : IOException("partial part expected=$expectedBytes written=$writtenBytes")
