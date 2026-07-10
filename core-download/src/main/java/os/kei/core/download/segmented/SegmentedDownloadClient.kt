package os.kei.core.download.segmented

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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
import kotlin.math.max
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
            initialPartSizeBytes = options.initialPartSizeBytes,
            maxRetriesPerPart = effectiveRequeueBudget(options.maxRetriesPerPart),
            concurrency = effectiveConnections,
            tuning = options.speedProfile.schedulerTuning(),
        )
        val progress = ProgressAggregator(
            totalBytes = totalBytes,
            activeConnections = effectiveConnections,
            parallel = true,
            intervalMs = options.progressIntervalMs,
            onProgress = onProgress,
        )
        val workerClients = List(effectiveConnections) { client }
        val speedTracker = RangeSpeedTracker()
        progress.emit(force = true)
        RandomAccessFile(partFile, "rw").use { randomFile ->
            randomFile.setLength(totalBytes)
            randomFile.channel.use { channel ->
                coroutineScope {
                    repeat(effectiveConnections) { workerId ->
                        launch {
                            runWorker(
                                workerId = workerId,
                                httpClient = workerClients[workerId % workerClients.size],
                                speedTracker = speedTracker,
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
        verifyDownloadedSha256(partFile, request.expectedSha256)
        movePartToOutput(partFile, outputFile)
        progress.emit(force = true)
        val stats = scheduler.stats()
        return SegmentedDownloadResult(
            outputFile = outputFile,
            totalBytes = totalBytes,
            parallel = true,
            rangeSupported = true,
            finalUrl = probeResult.finalUrl,
            retryCount = stats.retryCount,
            stealCount = stats.stealCount,
            handoffCount = stats.handoffCount,
        )
    }

    private suspend fun runWorker(
        workerId: Int,
        httpClient: OkHttpClient,
        speedTracker: RangeSpeedTracker,
        request: SegmentedDownloadRequest,
        options: SegmentedDownloadOptions,
        scheduler: PartScheduler,
        channel: FileChannel,
        totalBytes: Long,
        progress: ProgressAggregator,
    ) {
        while (true) {
            currentCoroutineContext().ensureActive()
            val active = scheduler.nextPart(workerId)
            if (active == null) {
                if (scheduler.hasInFlight()) {
                    delay(scheduler.idlePollMs)
                    continue
                }
                return
            }
            val part = active.part
            val startedNs = System.nanoTime()
            val outcome = runCatching {
                downloadPart(
                    httpClient = httpClient,
                    speedTracker = speedTracker,
                    request = request,
                    options = options,
                    active = active,
                    channel = channel,
                    totalBytes = totalBytes,
                    progress = progress,
                )
            }
            val elapsedMs = elapsedMsSince(startedNs)
            val completedBytes = active.completedBytes()
            scheduler.finish(workerId = workerId, active = active)
            if (outcome.isSuccess) {
                scheduler.record(workerId = workerId, bytes = completedBytes, elapsedMs = elapsedMs)
                continue
            }
            val error = outcome.exceptionOrNull()
            if (error is CancellationException) throw error
            if (active.isComplete()) {
                scheduler.record(workerId = workerId, bytes = completedBytes, elapsedMs = elapsedMs)
                continue
            }
            val nextStart = active.currentOffset()
            val failedEnd = active.currentEndInclusive()
            val retryable = error.isRetryableDownloadError()
            val rateLimited = error is SegmentedDownloadHttpException && error.code == 429
            if (retryable && !rateLimited) {
                scheduler.penalize(workerId)
            }
            val requeued = retryable && scheduler.requeueFailed(
                part = part.copy(endInclusive = failedEnd),
                nextStart = nextStart,
                delayMs = if (rateLimited) options.retryDelayMs else 0L,
            )
            if (!requeued) {
                throw SegmentedDownloadException(
                    message = "part failed ${part.start}-${part.endInclusive}",
                    cause = error,
                )
            }
        }
    }

    private suspend fun downloadPart(
        httpClient: OkHttpClient,
        speedTracker: RangeSpeedTracker,
        request: SegmentedDownloadRequest,
        options: SegmentedDownloadOptions,
        active: ActiveDownloadPart,
        channel: FileChannel,
        totalBytes: Long,
        progress: ProgressAggregator,
    ) {
        val requestStart = active.currentOffset()
        val requestEnd = active.currentEndInclusive()
        if (requestStart > requestEnd) return
        val rangeHeader = "bytes=$requestStart-$requestEnd"
        val rangeRequest = request.newRequestBuilder()
            .header("Accept-Encoding", "identity")
            .header("Range", rangeHeader)
            .get()
            .build()
        val leaseMs = rangeLeaseMs(
            part = active.part,
            initialPartSizeBytes = options.initialPartSizeBytes,
        )
        val leaseJob =
            if (leaseMs > 0L) {
                CoroutineScope(currentCoroutineContext()).launch {
                    delay(leaseMs)
                    active.cancelAttempt()
                }
            } else {
                null
        }
        try {
            executeStreaming(httpClient, rangeRequest, active) { response ->
                if (response.code != 206) {
                    throw SegmentedDownloadHttpException(
                        code = response.code,
                        retryable = response.code == 429 || response.code in 500..599,
                    )
                }
                val contentRange = parseContentRange(response.header("Content-Range"))
                    ?: throw IOException("missing Content-Range")
                if (
                    contentRange.start != requestStart ||
                    contentRange.end != requestEnd ||
                    contentRange.totalBytes != totalBytes
                ) {
                    throw IOException("unexpected Content-Range ${response.header("Content-Range").orEmpty()}")
                }
                val buffer = ByteArray(options.bufferSizeBytes)
                val speedId = speedTracker.register()
                val startedNs = System.nanoTime()
                var lastCheckNs = startedNs
                var lastCheckOffset = requestStart
                var slowStrikes = 0
                response.body.byteStream().use { input ->
                    try {
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val offset = active.currentOffset()
                            val end = active.currentEndInclusive()
                            if (offset > end) return@executeStreaming
                            val remaining = end - offset + 1L
                            val read = input.read(buffer, 0, min(buffer.size.toLong(), remaining).toInt())
                            if (read < 0) break
                            if (read == 0) continue
                            val written = active.writeAvailable(read) { position, byteCount ->
                                writeAt(channel, position, buffer, byteCount)
                            }
                            if (written > 0) {
                                progress.addBytes(written.toLong())
                                val nowNs = System.nanoTime()
                                val checkElapsedMs = (nowNs - lastCheckNs) / 1_000_000L
                                if (checkElapsedMs >= SLOW_CONNECTION_CHECK_INTERVAL_MS) {
                                    val currentOffset = active.currentOffset()
                                    val speedBytesPerMs =
                                        (currentOffset - lastCheckOffset).toDouble() / checkElapsedMs.toDouble()
                                    val snapshot = speedTracker.update(speedId, speedBytesPerMs)
                                    if (
                                        shouldCloseSlowConnection(
                                            speedBytesPerMs = speedBytesPerMs,
                                            averageBytesPerMs = snapshot.averageBytesPerMs,
                                            measuredPeerCount = snapshot.measuredPeerCount,
                                            ageMs = (nowNs - startedNs) / 1_000_000L,
                                            bytes = currentOffset - requestStart,
                                        )
                                    ) {
                                        slowStrikes += 1
                                    } else {
                                        slowStrikes = 0
                                    }
                                    lastCheckNs = nowNs
                                    lastCheckOffset = currentOffset
                                    if (slowStrikes >= SLOW_CONNECTION_STRIKES) {
                                        throw SlowConnectionDownloadException()
                                    }
                                }
                            }
                            if (written < read) return@executeStreaming
                        }
                    } finally {
                        speedTracker.unregister(speedId)
                    }
                }
            }
        } finally {
            leaseJob?.cancelAndJoin()
        }
        if (active.currentOffset() <= active.currentEndInclusive()) {
            throw PartialPartDownloadException(
                expectedBytes = requestEnd - requestStart + 1L,
                writtenBytes = active.currentOffset() - requestStart,
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
        executeStreaming(client, singleRequest) { response ->
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
        verifyDownloadedSha256(partFile, request.expectedSha256)
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
        val usefulPartBytes = min(options.initialPartSizeBytes, MIN_DYNAMIC_PARALLEL_PART_BYTES)
            .coerceAtLeast(1L)
        val partCount = ((totalBytes + usefulPartBytes - 1L) / usefulPartBytes)
            .toInt()
            .coerceAtLeast(1)
        return min(options.maxConnections, partCount).coerceAtLeast(1)
    }

    private fun effectiveRequeueBudget(maxRetriesPerPart: Int): Int =
        if (maxRetriesPerPart <= 0) {
            0
        } else {
            max(maxRetriesPerPart * PIKO_REQUEUE_BUDGET_MULTIPLIER, PIKO_MIN_REQUEUE_BUDGET)
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

    private fun elapsedMsSince(startedNs: Long): Long =
        ((System.nanoTime() - startedNs) / 1_000_000L).coerceAtLeast(1L)

    private suspend fun <T> executeStreaming(
        httpClient: OkHttpClient,
        request: Request,
        active: ActiveDownloadPart? = null,
        block: suspend (Response) -> T,
    ): T {
        val call = httpClient.newCall(request)
        active?.setCancelAttempt { call.cancel() }
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
            active?.clearCancelAttempt()
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

private class SlowConnectionDownloadException : IOException("slow connection")

private const val MIN_DYNAMIC_PARALLEL_PART_BYTES = 512L * 1024L
private const val PIKO_REQUEUE_BUDGET_MULTIPLIER = 4
private const val PIKO_MIN_REQUEUE_BUDGET = 8

internal fun SegmentedDownloadSpeedProfile.schedulerTuning(): PartSchedulerTuning =
    when (this) {
        SegmentedDownloadSpeedProfile.Balanced -> PartSchedulerTuning()

        SegmentedDownloadSpeedProfile.ForegroundBoost ->
            PartSchedulerTuning(
                minDynamicPartSizeBytes = 256L * 1024L,
                minTailPartSizeBytes = 64L * 1024L,
                partSizeTargetDurationMs = 10_000L,
                idlePollMs = 30L,
            )
    }
