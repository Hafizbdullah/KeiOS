package os.kei.core.download.segmented

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.HashingSource
import okio.buffer
import okio.source
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Opt-in live benchmark for the segmented downloader.
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
 *   -Dkeios.download.liveRuns=3 \
 *   -Dkeios.download.partMiB=4
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
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.MINUTES)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()

        val rows = mutableListOf<DownloadBenchmarkRow>()
        repeat(runs) { index ->
            val runIndex = index + 1
            val single = suspend {
                runSingleStreamBenchmark(
                    client = client,
                    url = url,
                    outputFile = temp.newFile("single-$runIndex.apk").apply { delete() },
                    expectedBytes = expectedBytes,
                    expectedSha256 = expectedSha256,
                    runIndex = runIndex,
                )
            }
            val segmented = suspend {
                runSegmentedBenchmark(
                    client = client,
                    url = url,
                    outputFile = temp.newFile("segmented-$runIndex.apk").apply { delete() },
                    expectedBytes = expectedBytes,
                    expectedSha256 = expectedSha256,
                    runIndex = runIndex,
                    maxConnections = maxConnections,
                    partSizeBytes = partSizeBytes,
                )
            }
            if (runIndex % 2 == 0) {
                rows += segmented()
                rows += single()
            } else {
                rows += single()
                rows += segmented()
            }
        }

        println(buildBenchmarkReport(rows, url, expectedBytes, maxConnections, partSizeBytes))

        assertEquals(runs, rows.count { it.mode == "single" })
        assertEquals(runs, rows.count { it.mode == "segmented" })
        assertTrue(rows.all { it.bytes == expectedBytes })
    }

    private suspend fun runSingleStreamBenchmark(
        client: OkHttpClient,
        url: String,
        outputFile: File,
        expectedBytes: Long,
        expectedSha256: String,
        runIndex: Int,
    ): DownloadBenchmarkRow =
        withContext(Dispatchers.IO) {
            val recorder = ProgressRecorder()
            var finalHost = ""
            val startedNs = System.nanoTime()
            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", USER_AGENT)
                .header("Accept", ACCEPT_APK)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                finalHost = response.request.url.host
                val totalBytes = response.body.contentLength().takeIf { it > 0L } ?: expectedBytes
                recorder.start(totalBytes)
                response.body.byteStream().use { input ->
                    outputFile.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            recorder.record(downloaded)
                        }
                        output.flush()
                    }
                }
                recorder.record(outputFile.length(), force = true)
            }
            val elapsedMs = elapsedMs(startedNs)
            val downloadedBytes = outputFile.length()
            verifyOutput(outputFile, expectedBytes, expectedSha256)
            DownloadBenchmarkRow(
                runIndex = runIndex,
                mode = "single",
                elapsedMs = elapsedMs,
                bytes = downloadedBytes,
                parallel = false,
                activeConnections = 1,
                rangeSupported = false,
                retryCount = 0,
                stealCount = 0,
                handoffCount = 0,
                fallbackReason = "",
                finalHost = finalHost,
                samples = recorder.samples,
            )
        }

    private suspend fun runSegmentedBenchmark(
        client: OkHttpClient,
        url: String,
        outputFile: File,
        expectedBytes: Long,
        expectedSha256: String,
        runIndex: Int,
        maxConnections: Int,
        partSizeBytes: Long,
    ): DownloadBenchmarkRow {
        val downloader = SegmentedDownloadClient(client = client, dispatcher = Dispatchers.IO)
        val recorder = ProgressRecorder()
        val startedNs = System.nanoTime()
        var activeConnections = 0
        val result = downloader.downloadToFile(
            request = SegmentedDownloadRequest(
                url = url,
                outputFile = outputFile,
                expectedSha256 = expectedSha256,
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
            ),
            onProgress = { progress ->
                activeConnections = progress.activeConnections
                if (recorder.totalBytes <= 0L) {
                    recorder.start(progress.totalBytes.takeIf { it > 0L } ?: expectedBytes)
                }
                recorder.record(progress.downloadedBytes)
            },
        )
        recorder.record(outputFile.length(), force = true)
        val elapsedMs = elapsedMs(startedNs)
        val downloadedBytes = outputFile.length()
        verifyOutput(outputFile, expectedBytes, expectedSha256)
        return DownloadBenchmarkRow(
            runIndex = runIndex,
            mode = "segmented",
            elapsedMs = elapsedMs,
            bytes = downloadedBytes,
            parallel = result.parallel,
            activeConnections = activeConnections,
            rangeSupported = result.rangeSupported,
            retryCount = result.retryCount,
            stealCount = result.stealCount,
            handoffCount = result.handoffCount,
            fallbackReason = result.fallbackReason.orEmpty(),
            finalHost = result.finalUrl.toHttpUrlOrNull()?.host.orEmpty(),
            samples = recorder.samples,
        )
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
    ): String {
        val singles = rows.filter { it.mode == "single" }
        val segmented = rows.filter { it.mode == "segmented" }
        return buildString {
            appendLine("Segmented Download Live Benchmark")
            appendLine("URL: $url")
            appendLine("Bytes: $expectedBytes")
            appendLine("Max connections: $maxConnections")
            appendLine("Part size: ${partSizeBytes / 1024L / 1024L} MiB")
            appendLine()
            appendLine("run,mode,elapsed_ms,avg_mib_s,early_0_10_mib_s,mid_10_90_mib_s,tail_90_100_mib_s,tail_ms,parallel,connections,range,retry,steal,handoff,fallback,host")
            rows.forEach { row ->
                val speed = row.speedBreakdown()
                appendLine(
                    "${row.runIndex},${row.mode},${row.elapsedMs},${row.averageMiBs().format2()}," +
                        "${speed.earlyMiBs.format2()},${speed.midMiBs.format2()},${speed.tailMiBs.format2()}," +
                        "${speed.tailMs},${row.parallel},${row.activeConnections},${row.rangeSupported}," +
                        "${row.retryCount},${row.stealCount},${row.handoffCount}," +
                        "${row.fallbackReason.ifBlank { "-" }},${row.finalHost}"
                )
            }
            appendLine()
            appendLine("Median")
            val singleElapsed = singles.medianElapsedMs()
            val segmentedElapsed = segmented.medianElapsedMs()
            appendLine("single elapsed=${singleElapsed.format2()}ms avg=${singles.medianAverageMiBs().format2()} MiB/s tail=${singles.medianTailMs().format2()}ms")
            appendLine("segmented elapsed=${segmentedElapsed.format2()}ms avg=${segmented.medianAverageMiBs().format2()} MiB/s tail=${segmented.medianTailMs().format2()}ms")
            appendLine("speedup=${speedup(singleElapsed, segmentedElapsed).format2()}x")
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

    private fun isLiveBenchmarkEnabled(): Boolean {
        return readProperty("keios.download.liveBenchmark")
            ?.let { value -> value.equals("true", ignoreCase = true) || value == "1" } == true
    }

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
        (System.nanoTime() - startedNs) / 1_000_000L

    private fun Double.format2(): String =
        "%.2f".format(this)

    private data class DownloadBenchmarkRow(
        val runIndex: Int,
        val mode: String,
        val elapsedMs: Long,
        val bytes: Long,
        val parallel: Boolean,
        val activeConnections: Int,
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

    private class ProgressRecorder {
        private val startedNs = System.nanoTime()
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

    private companion object {
        private const val USER_AGENT = "KeiOS-App/1.0 (Android)"
        private const val ACCEPT_APK = "application/vnd.android.package-archive, application/octet-stream;q=0.9, */*;q=0.1"
        private const val BUFFER_SIZE = DEFAULT_SEGMENTED_DOWNLOAD_BUFFER_SIZE_BYTES
        private const val MIB = 1024.0 * 1024.0
        private const val PASEO_APK_URL =
            "https://github.com/getpaseo/paseo/releases/download/v0.1.104/paseo-v0.1.104-android.apk"
        private const val PASEO_APK_BYTES = 183_037_443L
        private const val PASEO_APK_SHA256 = "f98520a1d8c9df9fb54c11505fa5af32cd108b0b3581927a596f40d9fc5191d5"
    }
}
