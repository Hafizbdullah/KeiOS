package os.kei.core.download.segmented

import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okio.Buffer
import okio.HashingSource
import okio.buffer
import okio.source
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Opt-in live benchmark for plain GET, shared HTTP/2, and isolated worker downloads.
 *
 * Property lookup order:
 * 1. JVM system properties
 * 2. Environment variables
 * 3. ~/.gradle/gradle.properties
 *
 * Recommended one-off run:
 *
 * ./gradlew :core-download:testDebugUnitTest \
 *   --tests "os.kei.core.download.segmented.SegmentedDownloadLiveBenchmarkTest" \
 *   -Dkeios.download.liveBenchmark=true \
 *   -Dkeios.download.liveRuns=1 \
 *   -Dkeios.download.partMiB=4 \
 *   -Dkeios.download.protocol=auto
 */
class SegmentedDownloadLiveBenchmarkTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `live benchmark compares single stream and segmented download`() = runBlocking {
        assumeTrue(
            "Set keios.download.liveBenchmark=true to enable large live downloads",
            isLiveBenchmarkEnabled(),
        )

        val url = readProperty("keios.download.liveUrl").orEmpty().ifBlank { PASEO_APK_URL }
        val expectedBytes = readProperty("keios.download.liveBytes")?.toLongOrNull() ?: PASEO_APK_BYTES
        val expectedSha256 = readProperty("keios.download.liveSha256").orEmpty().ifBlank { PASEO_APK_SHA256 }
        val runs = readProperty("keios.download.liveRuns")?.toIntOrNull()?.coerceIn(1, 5) ?: 3
        val maxConnections = readProperty("keios.download.maxConnections")?.toIntOrNull()?.coerceIn(1, 16) ?: 4
        val partSizeBytes =
            (readProperty("keios.download.partMiB")?.toLongOrNull()?.coerceIn(1L, 64L) ?: 4L) *
                1024L *
                1024L
        val protocolMode = LiveProtocolMode.from(readProperty("keios.download.protocol"))

        val rows = mutableListOf<DownloadBenchmarkRow>()
        repeat(runs) { index ->
            val runIndex = index + 1
            val operations: List<suspend () -> DownloadBenchmarkRow> = listOf(
                {
                    runSingleStreamBenchmark(
                        url = url,
                        outputFile = temp.newFile("plain-get-$runIndex.apk").apply { delete() },
                        expectedBytes = expectedBytes,
                        expectedSha256 = expectedSha256,
                        runIndex = runIndex,
                        protocolMode = protocolMode,
                    )
                },
                {
                    runSegmentedBenchmark(
                        url = url,
                        outputFile = temp.newFile("segmented-shared-$runIndex.apk").apply { delete() },
                        expectedBytes = expectedBytes,
                        expectedSha256 = expectedSha256,
                        runIndex = runIndex,
                        maxConnections = maxConnections,
                        partSizeBytes = partSizeBytes,
                        connectionStrategy = SegmentedDownloadConnectionStrategy.Shared,
                        protocolMode = protocolMode,
                    )
                },
                {
                    runSegmentedBenchmark(
                        url = url,
                        outputFile = temp.newFile("segmented-isolated-$runIndex.apk").apply { delete() },
                        expectedBytes = expectedBytes,
                        expectedSha256 = expectedSha256,
                        runIndex = runIndex,
                        maxConnections = maxConnections,
                        partSizeBytes = partSizeBytes,
                        connectionStrategy = SegmentedDownloadConnectionStrategy.IsolatedPerWorker,
                        protocolMode = protocolMode,
                    )
                },
            )
            val offset = index % operations.size
            val ordered = operations.drop(offset) + operations.take(offset)
            for (operation in ordered) {
                rows += operation()
            }
        }

        println(
            buildBenchmarkReport(
                rows = rows,
                url = url,
                expectedBytes = expectedBytes,
                maxConnections = maxConnections,
                partSizeBytes = partSizeBytes,
                protocolMode = protocolMode,
            ),
        )

        LIVE_MODE_ORDER.forEach { mode ->
            assertEquals(runs, rows.count { it.mode == mode })
        }
        assertTrue(rows.all { it.bytes == expectedBytes })
    }

    private suspend fun runSingleStreamBenchmark(
        url: String,
        outputFile: File,
        expectedBytes: Long,
        expectedSha256: String,
        runIndex: Int,
        protocolMode: LiveProtocolMode,
    ): DownloadBenchmarkRow {
        val tracker = BenchmarkConnectionTracker()
        val client = createBenchmarkClient(protocolMode, tracker)
        return try {
            withContext(Dispatchers.IO) {
                val startedNs = System.nanoTime()
                val recorder = ProgressRecorder(startedNs)
                recorder.start(expectedBytes)
                var finalHost = ""
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", ACCEPT_APK)
                    .build()
                client.newCall(request).execute().use { response ->
                    check(response.isSuccessful) { "HTTP ${response.code}" }
                    finalHost = response.request.url.host
                    response.body.byteStream().use { input ->
                        FileOutputStream(outputFile).use { output ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var downloaded = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                if (read == 0) continue
                                output.write(buffer, 0, read)
                                downloaded += read
                                recorder.record(downloaded)
                            }
                            output.flush()
                            output.fd.sync()
                        }
                    }
                    recorder.record(outputFile.length(), force = true)
                }
                val elapsedMs = elapsedMs(startedNs)
                val downloadedBytes = outputFile.length()
                verifyOutput(outputFile, expectedBytes, expectedSha256)
                DownloadBenchmarkRow(
                    runIndex = runIndex,
                    mode = "plain_get",
                    elapsedMs = elapsedMs,
                    bytes = downloadedBytes,
                    parallel = false,
                    activeConnections = 1,
                    physicalConnections = tracker.physicalConnectionCount,
                    requestCount = tracker.requestCount,
                    connectionStrategy = "Single",
                    protocol = tracker.protocolLabel(protocolMode.label),
                    rangeSupported = false,
                    retryCount = 0,
                    stealCount = 0,
                    handoffCount = 0,
                    fallbackReason = "",
                    finalHost = finalHost,
                    samples = recorder.samples,
                )
            }
        } finally {
            disposeBenchmarkClient(client)
        }
    }

    private suspend fun runSegmentedBenchmark(
        url: String,
        outputFile: File,
        expectedBytes: Long,
        expectedSha256: String,
        runIndex: Int,
        maxConnections: Int,
        partSizeBytes: Long,
        connectionStrategy: SegmentedDownloadConnectionStrategy,
        protocolMode: LiveProtocolMode,
    ): DownloadBenchmarkRow {
        val tracker = BenchmarkConnectionTracker()
        val client = createBenchmarkClient(protocolMode, tracker)
        return try {
            val downloader = SegmentedDownloadClient(client = client, dispatcher = Dispatchers.IO)
            val startedNs = System.nanoTime()
            val recorder = ProgressRecorder(startedNs)
            recorder.start(expectedBytes)
            var activeConnections = 0
            val result = downloader.downloadToFile(
                request = SegmentedDownloadRequest(
                    url = url,
                    outputFile = outputFile,
                    expectedSizeBytes = expectedBytes,
                    headers = mapOf(
                        "User-Agent" to USER_AGENT,
                        "Accept" to ACCEPT_APK,
                    ),
                    fileNameHint = outputFile.name,
                ),
                options = SegmentedDownloadOptions(
                    minParallelSizeBytes = 1L,
                    initialPartSizeBytes = partSizeBytes,
                    maxConnections = maxConnections,
                    maxRetriesPerPart = 3,
                    retryDelayMs = 1_000L,
                    progressIntervalMs = 250L,
                    requireHttpsForParallel = true,
                    bufferSizeBytes = BUFFER_SIZE,
                    connectionStrategy = connectionStrategy,
                ),
                onProgress = { progress ->
                    activeConnections = progress.activeConnections
                    recorder.record(progress.downloadedBytes)
                },
            )
            recorder.record(outputFile.length(), force = true)
            val elapsedMs = elapsedMs(startedNs)
            val downloadedBytes = outputFile.length()
            verifyOutput(outputFile, expectedBytes, expectedSha256)
            DownloadBenchmarkRow(
                runIndex = runIndex,
                mode =
                    when (connectionStrategy) {
                        SegmentedDownloadConnectionStrategy.Shared -> "segmented_shared"
                        SegmentedDownloadConnectionStrategy.IsolatedPerWorker -> "segmented_isolated"
                    },
                elapsedMs = elapsedMs,
                bytes = downloadedBytes,
                parallel = result.parallel,
                activeConnections = activeConnections,
                physicalConnections = tracker.physicalConnectionCount,
                requestCount = tracker.requestCount,
                connectionStrategy = connectionStrategy.name,
                protocol = tracker.protocolLabel(protocolMode.label),
                rangeSupported = result.rangeSupported,
                retryCount = result.retryCount,
                stealCount = result.stealCount,
                handoffCount = result.handoffCount,
                fallbackReason = result.fallbackReason.orEmpty(),
                finalHost = result.finalUrl.toHttpUrlOrNull()?.host.orEmpty(),
                samples = recorder.samples,
            )
        } finally {
            disposeBenchmarkClient(client)
        }
    }

    private fun createBenchmarkClient(
        protocolMode: LiveProtocolMode,
        tracker: BenchmarkConnectionTracker,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .writeTimeout(60L, TimeUnit.SECONDS)
            .callTimeout(10L, TimeUnit.MINUTES)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .eventListener(tracker)
        if (protocolMode == LiveProtocolMode.Http1) {
            builder.protocols(listOf(Protocol.HTTP_1_1))
        }
        return builder.build()
    }

    private fun disposeBenchmarkClient(client: OkHttpClient) {
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }

    private fun verifyOutput(
        outputFile: File,
        expectedBytes: Long,
        expectedSha256: String,
    ) {
        assertEquals(expectedBytes, outputFile.length())
        if (expectedSha256.isNotBlank()) {
            assertEquals(expectedSha256, sha256(outputFile))
        }
        runCatching { outputFile.delete() }
    }

    private fun buildBenchmarkReport(
        rows: List<DownloadBenchmarkRow>,
        url: String,
        expectedBytes: Long,
        maxConnections: Int,
        partSizeBytes: Long,
        protocolMode: LiveProtocolMode,
    ): String {
        val baselineRows = rows.filter { it.mode == "plain_get" }
        return buildString {
            appendLine("Segmented Download Live Benchmark")
            appendLine("URL: $url")
            appendLine("Bytes: $expectedBytes")
            appendLine("Max connections: $maxConnections")
            appendLine("Part size: ${partSizeBytes / 1024L / 1024L} MiB")
            appendLine("Protocol mode: ${protocolMode.label}")
            appendLine()
            appendLine(
                "run,mode,elapsed_ms,avg_mib_s,early_0_10_mib_s,mid_10_90_mib_s," +
                    "tail_90_100_mib_s,tail_ms,parallel,workers,physical_connections,requests," +
                    "strategy,protocol,range,retry,steal,handoff,fallback,host",
            )
            rows.forEach { row ->
                val speed = row.speedBreakdown()
                appendLine(
                    "${row.runIndex},${row.mode},${row.elapsedMs},${row.averageMiBs().format2()}," +
                        "${speed.earlyMiBs.format2()},${speed.midMiBs.format2()},${speed.tailMiBs.format2()}," +
                        "${speed.tailMs},${row.parallel},${row.activeConnections},${row.physicalConnections}," +
                        "${row.requestCount},${row.connectionStrategy},${row.protocol},${row.rangeSupported}," +
                        "${row.retryCount},${row.stealCount},${row.handoffCount}," +
                        "${row.fallbackReason.ifBlank { "-" }},${row.finalHost}",
                )
            }
            appendLine()
            appendLine("Median")
            val baselineElapsedMs = baselineRows.medianElapsedMs()
            LIVE_MODE_ORDER.forEach { mode ->
                val modeRows = rows.filter { it.mode == mode }
                val line = buildString {
                    append("$mode elapsed=${modeRows.medianElapsedMs().format2()}ms")
                    append(" avg=${modeRows.medianAverageMiBs().format2()} MiB/s")
                    append(" tail=${modeRows.medianTailMs().format2()}ms")
                    if (mode != "plain_get") {
                        append(" speedup=${speedup(baselineElapsedMs, modeRows.medianElapsedMs()).format2()}x")
                    }
                }
                appendLine(line)
            }
        }
    }

    private fun DownloadBenchmarkRow.averageMiBs(): Double =
        bytes.toDouble() / MIB / (elapsedMs.toDouble() / 1_000.0)

    private fun DownloadBenchmarkRow.speedBreakdown(): SpeedBreakdown {
        val total = samples.lastOrNull()?.bytes ?: bytes
        if (samples.size < 2 || total <= 0L) return SpeedBreakdown()
        val tenBytes = (total * 0.10).toLong()
        val ninetyBytes = (total * 0.90).toLong()
        val start = samples.first()
        val ten = samples.firstAtLeast(tenBytes) ?: samples.last()
        val ninety = samples.firstAtLeast(ninetyBytes) ?: samples.last()
        val end = samples.last()
        return SpeedBreakdown(
            earlyMiBs = segmentMiBs(start, ten),
            midMiBs = segmentMiBs(ten, ninety),
            tailMiBs = segmentMiBs(ninety, end),
            tailMs = max(0L, end.elapsedMs - ninety.elapsedMs),
        )
    }

    private fun List<ProgressSample>.firstAtLeast(bytes: Long): ProgressSample? =
        firstOrNull { it.bytes >= bytes }

    private fun segmentMiBs(
        start: ProgressSample,
        end: ProgressSample,
    ): Double {
        val elapsed = max(1L, end.elapsedMs - start.elapsedMs)
        val bytes = max(0L, end.bytes - start.bytes)
        return bytes.toDouble() / MIB / (elapsed.toDouble() / 1_000.0)
    }

    private fun List<DownloadBenchmarkRow>.medianElapsedMs(): Double =
        map { it.elapsedMs.toDouble() }.median()

    private fun List<DownloadBenchmarkRow>.medianAverageMiBs(): Double =
        map { it.averageMiBs() }.median()

    private fun List<DownloadBenchmarkRow>.medianTailMs(): Double =
        map { it.speedBreakdown().tailMs.toDouble() }.median()

    private fun speedup(
        singleElapsedMs: Double,
        segmentedElapsedMs: Double,
    ): Double {
        if (singleElapsedMs <= 0.0 || segmentedElapsedMs <= 0.0) return 0.0
        return singleElapsedMs / segmentedElapsedMs
    }

    private fun List<Double>.median(): Double {
        if (isEmpty()) return 0.0
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private fun isLiveBenchmarkEnabled(): Boolean =
        readProperty("keios.download.liveBenchmark")
            ?.let { value -> value.equals("true", ignoreCase = true) || value == "1" } == true

    private fun readProperty(key: String): String? {
        val sysValue = System.getProperty(key)?.trim()
        if (!sysValue.isNullOrBlank()) return sysValue
        val envKey = key
            .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
            .replace('.', '_')
            .uppercase()
        val envValue = System.getenv(envKey)?.trim()
        if (!envValue.isNullOrBlank()) return envValue

        val gradleProps = File(System.getProperty("user.home"), ".gradle/gradle.properties")
        if (!gradleProps.exists()) return null
        return gradleProps
            .useLines { lines ->
                lines
                    .map { line -> line.trim() }
                    .firstOrNull { line ->
                        line.isNotBlank() &&
                            !line.startsWith("#") &&
                            line.substringBefore('=').trim() == key
                    }
            }
            ?.substringAfter('=', "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun sha256(file: File): String {
        val hashingSource = HashingSource.sha256(file.source())
        hashingSource.buffer().use { source ->
            val sink = Buffer()
            while (source.read(sink, BUFFER_SIZE.toLong()) >= 0L) {
                sink.clear()
            }
        }
        return hashingSource.hash.hex()
    }

    private fun elapsedMs(startedNs: Long): Long =
        ((System.nanoTime() - startedNs) / 1_000_000L).coerceAtLeast(1L)

    private fun Double.format2(): String =
        String.format(Locale.US, "%.2f", this)

    private data class DownloadBenchmarkRow(
        val runIndex: Int,
        val mode: String,
        val elapsedMs: Long,
        val bytes: Long,
        val parallel: Boolean,
        val activeConnections: Int,
        val physicalConnections: Int,
        val requestCount: Int,
        val connectionStrategy: String,
        val protocol: String,
        val rangeSupported: Boolean,
        val retryCount: Int,
        val stealCount: Int,
        val handoffCount: Int,
        val fallbackReason: String,
        val finalHost: String,
        val samples: List<ProgressSample>,
    )

    private data class ProgressSample(
        val elapsedMs: Long,
        val bytes: Long,
    )

    private data class SpeedBreakdown(
        val earlyMiBs: Double = 0.0,
        val midMiBs: Double = 0.0,
        val tailMiBs: Double = 0.0,
        val tailMs: Long = 0L,
    )

    private class ProgressRecorder(
        private val startedNs: Long,
    ) {
        private val mutableSamples = mutableListOf<ProgressSample>()
        private var lastSampleMs = Long.MIN_VALUE

        var totalBytes: Long = -1L
            private set

        val samples: List<ProgressSample>
            get() = mutableSamples.toList()

        fun start(totalBytes: Long) {
            if (this.totalBytes > 0L) return
            this.totalBytes = totalBytes
            record(0L, force = true)
        }

        fun record(
            bytes: Long,
            force: Boolean = false,
        ) {
            val elapsedMs = (System.nanoTime() - startedNs) / 1_000_000L
            val complete = totalBytes > 0L && bytes >= totalBytes
            if (!force && !complete && lastSampleMs != Long.MIN_VALUE && elapsedMs - lastSampleMs < 250L) return
            val safeBytes = bytes.coerceAtLeast(0L)
            if (mutableSamples.lastOrNull()?.bytes == safeBytes && !force) return
            mutableSamples += ProgressSample(
                elapsedMs = elapsedMs,
                bytes = safeBytes,
            )
            lastSampleMs = elapsedMs
        }
    }

    private enum class LiveProtocolMode(val label: String) {
        Auto("auto"),
        Http1("http/1.1");

        companion object {
            fun from(value: String?): LiveProtocolMode =
                when (value?.trim()?.lowercase()) {
                    "h1", "http1", "http/1.1" -> Http1
                    else -> Auto
                }
        }
    }

    private companion object {
        private val LIVE_MODE_ORDER = listOf(
            "plain_get",
            "segmented_shared",
            "segmented_isolated",
        )
        private const val USER_AGENT = "KeiOS-App/1.0 (Android)"
        private const val ACCEPT_APK =
            "application/vnd.android.package-archive, application/octet-stream;q=0.9, */*;q=0.1"
        private const val BUFFER_SIZE = DEFAULT_SEGMENTED_DOWNLOAD_BUFFER_SIZE_BYTES
        private const val MIB = 1024.0 * 1024.0
        private const val PASEO_APK_URL =
            "https://github.com/getpaseo/paseo/releases/download/v0.1.104/paseo-v0.1.104-android.apk"
        private const val PASEO_APK_BYTES = 183_037_443L
        private const val PASEO_APK_SHA256 = "f98520a1d8c9df9fb54c11505fa5af32cd108b0b3581927a596f40d9fc5191d5"
    }
}
