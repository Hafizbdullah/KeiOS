package os.kei.core.download.segmented

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class SegmentedDownloadClient(
    private val client: OkHttpClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val probe = RangeProbe(client)
    private val downloadClient = client.newBuilder()
        .callTimeout(0L, TimeUnit.MILLISECONDS)
        .build()

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
            var resourceRestartCount = 0
            while (true) {
                runCatching { partFile.delete() }
                try {
                    val probeResult = probe.probe(request)
                    validateExpectedDownloadSize(
                        actualBytes = probeResult.totalBytes,
                        expectedBytes = request.expectedSizeBytes,
                    )
                    val parallelDecision = resolveParallelDecision(probeResult, options)
                    val result =
                        if (parallelDecision.parallel) {
                            downloadRangesWithProtocolFallback(
                                request = request,
                                options = options,
                                probeResult = probeResult,
                                outputFile = outputFile,
                                partFile = partFile,
                                parallel = true,
                                rangeFallbackReason = null,
                                onProgress = onProgress,
                            )
                        } else if (probeResult.rangeSupported) {
                            downloadRangesWithProtocolFallback(
                                request = request,
                                options = options,
                                probeResult = probeResult,
                                outputFile = outputFile,
                                partFile = partFile,
                                parallel = false,
                                rangeFallbackReason = parallelDecision.reason,
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
                    return@withContext result
                } catch (error: Throwable) {
                    runCatching { partFile.delete() }
                    if (error is CancellationException) throw error
                    if (
                        error is RangeResourceChangedException &&
                        resourceRestartCount < MAX_RESOURCE_SNAPSHOT_RESTARTS
                    ) {
                        resourceRestartCount += 1
                        continue
                    }
                    throw error
                }
            }
            error("unreachable download loop")
        }

    private suspend fun downloadRangesWithProtocolFallback(
        request: SegmentedDownloadRequest,
        options: SegmentedDownloadOptions,
        probeResult: RangeProbeResult,
        outputFile: File,
        partFile: File,
        parallel: Boolean,
        rangeFallbackReason: String?,
        onProgress: suspend (SegmentedDownloadProgress) -> Unit,
    ): SegmentedDownloadResult =
        try {
            downloadRanges(
                request = request,
                options = options,
                probeResult = probeResult,
                outputFile = outputFile,
                partFile = partFile,
                parallel = parallel,
                fallbackReason = rangeFallbackReason,
                onProgress = onProgress,
            )
        } catch (error: RangeProtocolException) {
            runCatching { partFile.delete() }
            downloadSingleStream(
                request = request,
                options = options,
                probeResult = probeResult,
                outputFile = outputFile,
                partFile = partFile,
                fallbackReason = "range-protocol-error",
                onProgress = onProgress,
            )
        }

    private suspend fun downloadRanges(
        request: SegmentedDownloadRequest,
        options: SegmentedDownloadOptions,
        probeResult: RangeProbeResult,
        outputFile: File,
        partFile: File,
        parallel: Boolean,
        fallbackReason: String?,
        onProgress: suspend (SegmentedDownloadProgress) -> Unit,
    ): SegmentedDownloadResult {
        val totalBytes = probeResult.totalBytes
        val effectiveConnections =
            if (parallel) {
                effectiveConnectionCount(totalBytes, options)
            } else {
                1
            }
        val effectiveInitialPartSizeBytes =
            effectiveInitialPartSizeBytes(
                options = options,
                effectiveConnections = effectiveConnections,
            )
        val schedulerTuning = schedulerTuningFor(options, effectiveConnections).let { tuning ->
            if (parallel) {
                tuning
            } else {
                tuning.copy(
                    tailPartsPerConnection = 1,
                    tailWindowInitialMultiplier = 0,
                    tailWindowMinDynamicMultiplier = 0,
                    startupActiveConnections = 1,
                    rateLimitMinActiveConnections = 1,
                )
            }
        }
        val scheduler = PartScheduler(
            totalBytes = totalBytes,
            initialPartSizeBytes = effectiveInitialPartSizeBytes,
            maxRetriesPerPart = options.maxRetriesPerPart,
            concurrency = effectiveConnections,
            tuning = schedulerTuning,
        )
        val progress = ProgressAggregator(
            totalBytes = totalBytes,
            activeConnections = effectiveConnections,
            parallel = parallel,
            intervalMs = options.progressIntervalMs,
            onProgress = onProgress,
        )
        val connectionStrategy = resolveConnectionStrategy(
            configured = options.connectionStrategy,
            protocol = probeResult.protocol,
        )
        val workerClientSet = createDownloadWorkerClientSet(
            baseClient = downloadClient,
            count = effectiveConnections,
            strategy = connectionStrategy,
        )
        val speedTracker = RangeSpeedTracker()
        progress.emit(force = true)
        try {
            RandomAccessFile(partFile, "rw").use { randomFile ->
                randomFile.setLength(totalBytes)
                randomFile.channel.use { channel ->
                    coroutineScope {
                        val writer = BoundedAsyncFileWriter(
                            scope = this,
                            capacity = options.writeQueueCapacity,
                            writeAt = { position, bytes ->
                                writeAt(channel, position, bytes)
                            },
                            onBytesWritten = progress::addBytes,
                        )
                        try {
                            val workers = List(effectiveConnections) { workerId ->
                                launch {
                                    runWorker(
                                        workerId = workerId,
                                        httpClient = workerClientSet.clients[workerId],
                                        speedTracker = speedTracker,
                                        request = request,
                                        dataUrl = probeResult.finalUrl,
                                        resourceValidator = probeResult.resourceValidator,
                                        options = options,
                                        scheduler = scheduler,
                                        writer = writer,
                                        totalBytes = totalBytes,
                                    )
                                }
                            }
                            workers.joinAll()
                            writer.closeAndJoin()
                        } finally {
                            withContext(NonCancellable) {
                                writer.cancelAndJoin()
                            }
                        }
                    }
                    channel.force(true)
                }
            }
        } finally {
            workerClientSet.close()
        }
        validateWrittenByteCount(progress.downloadedBytes, totalBytes)
        validateLength(partFile, totalBytes)
        validateExpectedDownloadSize(partFile.length(), request.expectedSizeBytes)
        verifyDownloadedSha256(partFile, request.expectedSha256)
        movePartToOutput(partFile, outputFile)
        progress.emit(force = true)
        val stats = scheduler.stats()
        return SegmentedDownloadResult(
            outputFile = outputFile,
            totalBytes = totalBytes,
            parallel = parallel,
            rangeSupported = true,
            finalUrl = probeResult.finalUrl,
            workerConnections = effectiveConnections,
            peakActiveConnections = stats.peakActiveConnections,
            retryCount = stats.retryCount,
            stealCount = stats.stealCount,
            handoffCount = stats.handoffCount,
            connectionStrategy = connectionStrategy,
            fallbackReason = fallbackReason,
        )
    }

    private suspend fun runWorker(
        workerId: Int,
        httpClient: OkHttpClient,
        speedTracker: RangeSpeedTracker,
        request: SegmentedDownloadRequest,
        dataUrl: String,
        resourceValidator: RangeResourceValidator?,
        options: SegmentedDownloadOptions,
        scheduler: PartScheduler,
        writer: BoundedAsyncFileWriter,
        totalBytes: Long,
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
                    dataUrl = dataUrl,
                    resourceValidator = resourceValidator,
                    options = options,
                    active = active,
                    writer = writer,
                    totalBytes = totalBytes,
                )
            }
            val elapsedMs = elapsedMsSince(startedNs)
            val completedBytes = active.completedBytes()
            scheduler.finish(workerId = workerId, active = active)
            if (outcome.isSuccess) {
                scheduler.recordSuccess(
                    workerId = workerId,
                    part = part,
                    bytes = completedBytes,
                    elapsedMs = elapsedMs,
                )
                continue
            }
            val error = outcome.exceptionOrNull()
            if (error is CancellationException) throw error
            if (error is RangeResourceChangedException) throw error
            if (error is RangeProtocolException) throw error
            if (active.isComplete()) {
                scheduler.recordSuccess(
                    workerId = workerId,
                    part = part,
                    bytes = completedBytes,
                    elapsedMs = elapsedMs,
                )
                continue
            }
            val nextStart = active.currentOffset()
            val failedEnd = active.currentEndInclusive()
            val failureKind = error.rangeFailureKindOrNull()
            val rateLimited = failureKind == RangeFailureKind.RateLimited
            val retryable = failureKind != null
            val retryDelayMs = failureKind?.let { kind ->
                rangeRetryDelayMs(
                    error = error,
                    failureKind = kind,
                    retryCounts = part.retryCounts,
                    baseDelayMs = options.retryDelayMs,
                )
            } ?: 0L
            if (rateLimited) {
                scheduler.recordRateLimit(part = part, delayMs = retryDelayMs)
            } else if (retryable) {
                scheduler.penalize(workerId)
            }
            val requeued =
                failureKind != null && scheduler.requeueFailed(
                    part = part.copy(endInclusive = failedEnd),
                    nextStart = nextStart,
                    failureKind = failureKind,
                    delayMs = retryDelayMs,
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
        dataUrl: String,
        resourceValidator: RangeResourceValidator?,
        options: SegmentedDownloadOptions,
        active: ActiveDownloadPart,
        writer: BoundedAsyncFileWriter,
        totalBytes: Long,
    ) {
        val requestStart = active.currentOffset()
        val requestEnd = active.currentEndInclusive()
        if (requestStart > requestEnd) return
        val rangeHeader = "bytes=$requestStart-$requestEnd"
        val rangeRequestBuilder = request.newRequestBuilder(targetUrl = dataUrl)
            .header("Range", rangeHeader)
        resourceValidator?.let { validator ->
            rangeRequestBuilder.header("If-Range", validator.ifRangeValue)
        }
        val rangeRequest = rangeRequestBuilder.get().build()
        val leaseTracker = RangeLeaseTracker(
            remainingBytes = requestEnd - requestStart + 1L,
            retryCount = active.part.retryCount,
        )
        val leaseExpired = AtomicBoolean(false)
        val rateProbeIdleExpired = AtomicBoolean(false)
        val rateProbeIdleTracker =
            if (active.part.rateProbe) RateProbeIdleTracker() else null
        val leaseJob =
            if (leaseTracker.millisUntilExpiration() != Long.MAX_VALUE) {
                CoroutineScope(currentCoroutineContext()).launch {
                    while (true) {
                        val remainingMs = leaseTracker.millisUntilExpiration()
                        if (remainingMs == Long.MAX_VALUE) return@launch
                        if (remainingMs > 0L) {
                            delay(min(remainingMs, RANGE_LEASE_POLL_MS))
                            continue
                        }
                        if (leaseExpired.compareAndSet(false, true)) {
                            active.cancelAttempt()
                        }
                        return@launch
                    }
                }
            } else {
                null
            }
        val rateProbeIdleJob = rateProbeIdleTracker?.let { tracker ->
            CoroutineScope(currentCoroutineContext()).launch {
                while (true) {
                    val remainingMs = tracker.millisUntilExpiration()
                    if (remainingMs > 0L) {
                        delay(min(remainingMs, RATE_PROBE_IDLE_POLL_MS))
                        continue
                    }
                    if (rateProbeIdleExpired.compareAndSet(false, true)) {
                        active.cancelAttempt()
                    }
                    return@launch
                }
            }
        }
        try {
            try {
                executeStreaming(httpClient, rangeRequest, active) { response ->
                    if (response.code != 206) {
                        if (response.code == 200 && resourceValidator != null) {
                            throw RangeResourceChangedException()
                        }
                        if (response.code == 200) {
                            throw RangeProtocolException("range request was ignored")
                        }
                        if (response.code == 416) {
                            throw RangeResourceChangedException()
                        }
                        throw SegmentedDownloadHttpException(
                            code = response.code,
                            retryable =
                                response.code == 408 ||
                                    response.code == 425 ||
                                    response.code == 429 ||
                                    response.code in 500..599,
                            retryAfterMs = parseRetryAfterMs(response.header("Retry-After")),
                        )
                    }
                    val contentRange = parseContentRange(response.header("Content-Range"))
                        ?: throw RangeProtocolException("missing Content-Range")
                    if (contentRange.totalBytes != totalBytes) {
                        throw RangeResourceChangedException()
                    }
                    if (contentRange.start != requestStart || contentRange.end != requestEnd) {
                        throw RangeProtocolException(
                            "unexpected Content-Range ${response.header("Content-Range").orEmpty()}",
                        )
                    }
                    if (resourceValidator?.responseChanged(response) == true) {
                        throw RangeResourceChangedException()
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
                                rateProbeIdleTracker?.recordProgress()
                                val written = min(read.toLong(), remaining).toInt()
                                if (written > 0) {
                                    writer.enqueue(
                                        position = offset,
                                        source = buffer,
                                        byteCount = written,
                                    )
                                    active.advanceTo(offset + written)
                                    val currentOffset = active.currentOffset()
                                    leaseTracker.recordProgress(
                                        remainingBytes = (end - currentOffset + 1L).coerceAtLeast(0L),
                                    )
                                    val nowNs = System.nanoTime()
                                    val checkElapsedMs = (nowNs - lastCheckNs) / 1_000_000L
                                    if (checkElapsedMs >= SLOW_CONNECTION_CHECK_INTERVAL_MS) {
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
                                                remainingBytes =
                                                    (end - currentOffset + 1L).coerceAtLeast(0L),
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
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                currentCoroutineContext().ensureActive()
                if (rateProbeIdleExpired.get()) {
                    throw RateProbeIdleTimeoutException(error)
                }
                if (leaseExpired.get()) {
                    throw RangeLeaseExpiredException(error)
                }
                throw error
            }
        } finally {
            withContext(NonCancellable) {
                rateProbeIdleJob?.cancelAndJoin()
                leaseJob?.cancelAndJoin()
            }
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
        var retryCount = 0
        var completedAttempt: SingleStreamAttemptResult? = null
        while (completedAttempt == null) {
            try {
                completedAttempt = downloadSingleStreamAttempt(
                        request = request,
                        options = options,
                        probeResult = probeResult,
                        partFile = partFile,
                        onProgress = onProgress,
                    )
            } catch (error: Throwable) {
                runCatching { partFile.delete() }
                if (error is CancellationException) throw error
                val failureKind = error.singleStreamFailureKindOrNull()
                    ?: throw error
                if (retryCount >= options.maxRetriesPerPart) {
                    throw SegmentedDownloadException(
                        message = "single stream retry budget exhausted",
                        cause = error,
                    )
                }
                val retryCounts =
                    if (failureKind == RangeFailureKind.RateLimited) {
                        RangeRetryCounts(rateLimited = retryCount)
                    } else {
                        RangeRetryCounts(transient = retryCount)
                    }
                val retryDelayMs = rangeRetryDelayMs(
                    error = error,
                    failureKind = failureKind,
                    retryCounts = retryCounts,
                    baseDelayMs = options.retryDelayMs,
                )
                retryCount += 1
                if (retryDelayMs > 0L) delay(retryDelayMs)
            }
        }
        val attemptResult = requireNotNull(completedAttempt)
        val downloaded = attemptResult.downloadedBytes
        val totalBytes = attemptResult.totalBytes
        val finalUrl = attemptResult.finalUrl
        if (totalBytes > 0L) validateLength(partFile, totalBytes)
        validateExpectedDownloadSize(partFile.length(), request.expectedSizeBytes)
        verifyDownloadedSha256(partFile, request.expectedSha256)
        movePartToOutput(partFile, outputFile)
        return SegmentedDownloadResult(
            outputFile = outputFile,
            totalBytes = totalBytes.takeIf { it > 0L } ?: downloaded,
            parallel = false,
            rangeSupported = probeResult.rangeSupported,
            finalUrl = finalUrl,
            workerConnections = 1,
            peakActiveConnections = 1,
            retryCount = retryCount,
            connectionStrategy = SegmentedDownloadConnectionStrategy.Shared,
            fallbackReason = fallbackReason,
        )
    }

    private suspend fun downloadSingleStreamAttempt(
        request: SegmentedDownloadRequest,
        options: SegmentedDownloadOptions,
        probeResult: RangeProbeResult,
        partFile: File,
        onProgress: suspend (SegmentedDownloadProgress) -> Unit,
    ): SingleStreamAttemptResult {
        val singleRequest = request.newRequestBuilder(targetUrl = probeResult.finalUrl).get().build()
        var downloaded = 0L
        var finalUrl = probeResult.finalUrl
        var totalBytes = probeResult.totalBytes
        executeStreaming(downloadClient, singleRequest) { response ->
            if (!response.isSuccessful) {
                throw SegmentedDownloadHttpException(
                    code = response.code,
                    retryable =
                        response.code == 408 ||
                            response.code == 425 ||
                            response.code == 429 ||
                            response.code in 500..599,
                    retryAfterMs = parseRetryAfterMs(response.header("Retry-After")),
                )
            }
            finalUrl = response.request.url.toString()
            totalBytes = response.body.contentLength().takeIf { it > 0L }
                ?: probeResult.totalBytes
            val progress = ProgressAggregator(
                totalBytes = totalBytes,
                activeConnections = 1,
                parallel = false,
                intervalMs = options.progressIntervalMs,
                onProgress = { progress ->
                    try {
                        onProgress(progress)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        throw SegmentedDownloadProgressException(error)
                    }
                },
            )
            progress.emit(force = true)
            response.body.byteStream().use { input ->
                val output = try {
                    FileOutputStream(partFile)
                } catch (error: IOException) {
                    throw SegmentedDownloadStorageException(error)
                }
                output.use {
                    val buffer = ByteArray(options.bufferSizeBytes)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        try {
                            output.write(buffer, 0, read)
                        } catch (error: IOException) {
                            throw SegmentedDownloadStorageException(error)
                        }
                        downloaded += read
                        progress.addBytes(read.toLong())
                    }
                    try {
                        output.flush()
                        output.fd.sync()
                    } catch (error: IOException) {
                        throw SegmentedDownloadStorageException(error)
                    }
                }
            }
            progress.emit(force = true)
        }
        return SingleStreamAttemptResult(
            downloadedBytes = downloaded,
            totalBytes = totalBytes,
            finalUrl = finalUrl,
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
        if (effectiveConnectionCount(probeResult.totalBytes, options) < 2) {
            return ParallelDecision(parallel = false, reason = "single-connection-budget")
        }
        if (options.requireHttpsForParallel && !probeResult.finalUrl.startsWith("https://", ignoreCase = true)) {
            return ParallelDecision(parallel = false, reason = "non-https")
        }
        return ParallelDecision(parallel = true, reason = null)
    }

    private fun writeAt(
        channel: FileChannel,
        position: Long,
        bytes: ByteArray,
    ) {
        var written = 0
        while (written < bytes.size) {
            written += channel.write(
                ByteBuffer.wrap(bytes, written, bytes.size - written),
                position + written,
            )
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

internal fun resolveConnectionStrategy(
    configured: SegmentedDownloadConnectionStrategy,
    protocol: Protocol,
): SegmentedDownloadConnectionStrategy =
    when (configured) {
        SegmentedDownloadConnectionStrategy.Adaptive ->
            when (protocol) {
                Protocol.HTTP_2, Protocol.H2_PRIOR_KNOWLEDGE ->
                    SegmentedDownloadConnectionStrategy.IsolatedPerWorker

                else -> SegmentedDownloadConnectionStrategy.Shared
            }

        SegmentedDownloadConnectionStrategy.Shared -> SegmentedDownloadConnectionStrategy.Shared
        SegmentedDownloadConnectionStrategy.IsolatedPerWorker ->
            SegmentedDownloadConnectionStrategy.IsolatedPerWorker
    }

internal fun validateWrittenByteCount(
    writtenBytes: Long,
    expectedBytes: Long,
) {
    if (writtenBytes != expectedBytes) {
        throw IOException(
            "download byte coverage mismatch expected=$expectedBytes written=$writtenBytes",
        )
    }
}

private data class ParallelDecision(
    val parallel: Boolean,
    val reason: String?,
)

private data class SingleStreamAttemptResult(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val finalUrl: String,
)

private class PartialPartDownloadException(
    val expectedBytes: Long,
    val writtenBytes: Long,
) : IOException("partial part expected=$expectedBytes written=$writtenBytes")

private class SlowConnectionDownloadException : IOException("slow connection")

private class RangeLeaseExpiredException(
    cause: Throwable,
) : IOException("range lease expired", cause)

internal class RateProbeIdleTimeoutException(
    cause: Throwable,
) : IOException("rate probe idle timeout", cause)

private class RangeResourceChangedException : IOException("remote resource changed during segmented download")

private class RangeProtocolException(
    message: String,
) : IOException(message)

private class SegmentedDownloadStorageException(
    cause: Throwable,
) : IOException("download storage write failed: ${cause.message.orEmpty()}", cause)

private class SegmentedDownloadProgressException(
    cause: Throwable,
) : IOException("download progress callback failed: ${cause.message.orEmpty()}", cause)

internal fun Throwable?.rangeFailureKindOrNull(): RangeFailureKind? =
    when (this) {
        is BoundedAsyncWriterException -> null
        is PartialPartDownloadException -> RangeFailureKind.PartialEof
        is RateProbeIdleTimeoutException -> RangeFailureKind.RateLimited
        is RangeLeaseExpiredException,
        is SlowConnectionDownloadException,
        is SocketTimeoutException -> RangeFailureKind.Timeout

        is SocketException -> RangeFailureKind.ConnectionReset
        is SegmentedDownloadHttpException ->
            when {
                code == 429 -> RangeFailureKind.RateLimited
                retryable -> RangeFailureKind.Transient
                else -> null
            }

        is IOException -> RangeFailureKind.Transient
        else -> null
    }

private fun Throwable?.singleStreamFailureKindOrNull(): RangeFailureKind? =
    when (this) {
        is SegmentedDownloadStorageException,
        is SegmentedDownloadProgressException -> null

        is SegmentedDownloadHttpException ->
            when {
                code == 429 -> RangeFailureKind.RateLimited
                retryable -> RangeFailureKind.Transient
                else -> null
            }

        is SocketTimeoutException,
        is SocketException,
        is IOException -> RangeFailureKind.Transient

        else -> null
    }

private const val MAX_RESOURCE_SNAPSHOT_RESTARTS = 1

internal fun effectiveConnectionCount(
    totalBytes: Long,
    options: SegmentedDownloadOptions,
): Int {
    val usefulBytesPerConnection =
        maxOf(options.initialPartSizeBytes, options.minBytesPerConnection)
    val usefulConnectionCount =
        (totalBytes / usefulBytesPerConnection +
            if (totalBytes % usefulBytesPerConnection == 0L) 0L else 1L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
            .coerceAtLeast(1)
    return min(options.maxConnections, usefulConnectionCount).coerceAtLeast(1)
}

internal fun effectiveInitialPartSizeBytes(
    options: SegmentedDownloadOptions,
    effectiveConnections: Int,
): Long =
    if (effectiveConnections <= LOW_CONNECTION_BUDGET_THRESHOLD) {
        maxOf(options.initialPartSizeBytes, options.minBytesPerConnection / 2L)
    } else {
        options.initialPartSizeBytes
    }

internal fun schedulerTuningFor(
    options: SegmentedDownloadOptions,
    effectiveConnections: Int,
): PartSchedulerTuning =
    if (
        options.speedProfile == SegmentedDownloadSpeedProfile.ForegroundBoost &&
        effectiveConnections <= LOW_CONNECTION_BUDGET_THRESHOLD
    ) {
        SegmentedDownloadSpeedProfile.Balanced.schedulerTuning()
    } else {
        options.speedProfile.schedulerTuning()
    }

private const val LOW_CONNECTION_BUDGET_THRESHOLD = 4

internal fun SegmentedDownloadSpeedProfile.schedulerTuning(): PartSchedulerTuning =
    when (this) {
        SegmentedDownloadSpeedProfile.Balanced ->
            PartSchedulerTuning(
                tailWindowInitialMultiplier = 16,
            )

        SegmentedDownloadSpeedProfile.ForegroundBoost ->
            PartSchedulerTuning(
                minDynamicPartSizeBytes = 256L * 1024L,
                minTailPartSizeBytes = 64L * 1024L,
                tailPartsPerConnection = 3,
                tailWindowInitialMultiplier = 32,
                partSizeTargetDurationMs = 16_000L,
                startupActiveConnections = 8,
                rateLimitedMinPartSizeBytes = 32L * 1024L * 1024L,
                idlePollMs = 20L,
            )
    }
