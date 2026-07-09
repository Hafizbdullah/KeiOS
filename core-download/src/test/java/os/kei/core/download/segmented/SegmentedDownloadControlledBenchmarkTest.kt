package os.kei.core.download.segmented

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class SegmentedDownloadControlledBenchmarkTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `controlled examples compare segmented download speed`() = runBlocking {
        assumeTrue(
            "Set keios.download.controlledBenchmark=true to enable controlled download benchmarks",
            isControlledBenchmarkEnabled(),
        )

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .callTimeout(3, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()

        val capped = runPerConnectionCapScenario(client)
        val handoff = runSlowHeadHandoffScenario(client)

        println(
            buildString {
                appendLine("Segmented Download Controlled Benchmark")
                appendLine("scenario,mode,bytes,elapsed_ms,avg_mib_s,parallel,connections,retry,steal,handoff,speedup")
                appendRows(capped)
                appendRows(handoff)
            },
        )

        assertTrue(capped.speedup >= 2.0, "per-connection cap speedup=${capped.speedup.format2()}")
        assertTrue(handoff.speedup >= 2.0, "slow-head handoff speedup=${handoff.speedup.format2()}")
    }

    private suspend fun runPerConnectionCapScenario(client: OkHttpClient): ScenarioRows {
        val bytes = ByteArray(24 * MIB) { (it * 31).toByte() }
        return MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    rangeOrFullResponse(bytes, request.getHeader("Range"))
                        .throttleBody(128L * 1024L, 50, TimeUnit.MILLISECONDS)
            }
            runScenario(
                name = "per_connection_cap",
                client = client,
                server = server,
                bytes = bytes,
                partSizeBytes = 3L * MIB,
                baselineConnections = 1,
                segmentedConnections = 4,
            )
        }
    }

    private suspend fun runSlowHeadHandoffScenario(client: OkHttpClient): ScenarioRows {
        val bytes = ByteArray(30 * MIB) { (it * 17).toByte() }
        val initialPartBytes = 3L * MIB
        return MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val range = request.getHeader("Range")
                    val response = rangeOrFullResponse(bytes, range)
                    return if (range == "bytes=0-${initialPartBytes - 1L}") {
                        response.throttleBody(16L * 1024L, 40, TimeUnit.MILLISECONDS)
                    } else {
                        response.throttleBody(256L * 1024L, 40, TimeUnit.MILLISECONDS)
                    }
                }
            }
            runScenario(
                name = "slow_head_handoff",
                client = client,
                server = server,
                bytes = bytes,
                partSizeBytes = initialPartBytes,
                baselineConnections = 1,
                segmentedConnections = 4,
            )
        }
    }

    private suspend fun runScenario(
        name: String,
        client: OkHttpClient,
        server: MockWebServer,
        bytes: ByteArray,
        partSizeBytes: Long,
        baselineConnections: Int,
        segmentedConnections: Int,
    ): ScenarioRows {
        val baseline = runSegmented(
            client = client,
            server = server,
            bytes = bytes,
            outputFile = temp.newFile("$name-baseline.bin").apply { delete() },
            partSizeBytes = partSizeBytes,
            maxConnections = baselineConnections,
        )
        val segmented = runSegmented(
            client = client,
            server = server,
            bytes = bytes,
            outputFile = temp.newFile("$name-segmented.bin").apply { delete() },
            partSizeBytes = partSizeBytes,
            maxConnections = segmentedConnections,
        )
        return ScenarioRows(
            name = name,
            baseline = baseline.copy(mode = "baseline_${baselineConnections}x"),
            segmented = segmented.copy(mode = "segmented_${segmentedConnections}x"),
        )
    }

    private suspend fun runSegmented(
        client: OkHttpClient,
        server: MockWebServer,
        bytes: ByteArray,
        outputFile: File,
        partSizeBytes: Long,
        maxConnections: Int,
    ): BenchmarkRow {
        val downloader = SegmentedDownloadClient(client = client, dispatcher = Dispatchers.IO)
        var activeConnections = 0
        val startedNs = System.nanoTime()
        val result = downloader.downloadToFile(
            request = SegmentedDownloadRequest(
                url = server.url("/artifact.bin").toString(),
                outputFile = outputFile,
            ),
            options = SegmentedDownloadOptions(
                minParallelSizeBytes = 1L,
                initialPartSizeBytes = partSizeBytes,
                maxConnections = maxConnections,
                maxRetriesPerPart = 3,
                retryDelayMs = 1,
                progressIntervalMs = 0,
                requireHttpsForParallel = false,
                bufferSizeBytes = BUFFER_SIZE,
            ),
            onProgress = { progress ->
                activeConnections = progress.activeConnections
            },
        )
        assertContentEquals(bytes, outputFile.readBytes())
        return BenchmarkRow(
            mode = "segmented",
            bytes = outputFile.length(),
            elapsedMs = elapsedMs(startedNs),
            parallel = result.parallel,
            connections = max(activeConnections, 1),
            retryCount = result.retryCount,
            stealCount = result.stealCount,
            handoffCount = result.handoffCount,
        )
    }

    private fun rangeOrFullResponse(
        bytes: ByteArray,
        rangeHeader: String?,
    ): MockResponse {
        if (rangeHeader.isNullOrBlank()) {
            return MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Length", bytes.size)
                .addHeader("Accept-Ranges", "bytes")
                .setBody(Buffer().write(bytes))
        }
        val parts = rangeHeader.removePrefix("bytes=").split("-", limit = 2)
        val start = parts[0].toInt()
        val endInclusive = parts[1].toInt().coerceAtMost(bytes.lastIndex)
        return MockResponse()
            .setResponseCode(206)
            .addHeader("Content-Range", "bytes $start-$endInclusive/${bytes.size}")
            .addHeader("Content-Length", endInclusive - start + 1)
            .setBody(Buffer().write(bytes.copyOfRange(start, endInclusive + 1)))
    }

    private fun StringBuilder.appendRows(rows: ScenarioRows) {
        appendRow(rows.name, rows.baseline, rows.speedup)
        appendRow(rows.name, rows.segmented, rows.speedup)
    }

    private fun StringBuilder.appendRow(
        scenario: String,
        row: BenchmarkRow,
        speedup: Double,
    ) {
        appendLine(
            listOf(
                scenario,
                row.mode,
                row.bytes,
                row.elapsedMs,
                row.averageMiBs().format2(),
                row.parallel,
                row.connections,
                row.retryCount,
                row.stealCount,
                row.handoffCount,
                speedup.format2(),
            ).joinToString(separator = ","),
        )
    }

    private fun isControlledBenchmarkEnabled(): Boolean =
        System.getProperty("keios.download.controlledBenchmark")
            ?.let { value -> value.equals("true", ignoreCase = true) || value == "1" } == true

    private fun elapsedMs(startedNs: Long): Long =
        ((System.nanoTime() - startedNs) / 1_000_000L).coerceAtLeast(1L)

    private fun BenchmarkRow.averageMiBs(): Double =
        bytes.toDouble() / MIB.toDouble() / (elapsedMs.toDouble() / 1_000.0)

    private fun Double.format2(): String =
        "%.2f".format(this)

    private data class ScenarioRows(
        val name: String,
        val baseline: BenchmarkRow,
        val segmented: BenchmarkRow,
    ) {
        val speedup: Double =
            baseline.elapsedMs.toDouble() / segmented.elapsedMs.toDouble()
    }

    private data class BenchmarkRow(
        val mode: String,
        val bytes: Long,
        val elapsedMs: Long,
        val parallel: Boolean,
        val connections: Int,
        val retryCount: Int = 0,
        val stealCount: Int = 0,
        val handoffCount: Int = 0,
    )

    private companion object {
        private const val MIB = 1024 * 1024
        private const val BUFFER_SIZE = 64 * 1024
    }
}
