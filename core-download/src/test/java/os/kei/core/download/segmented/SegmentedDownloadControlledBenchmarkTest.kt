package os.kei.core.download.segmented

import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.math.max
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
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

        val capped = runPerConnectionCapScenario()
        val slowHead = runSlowHeadScenario()
        val http2 = runHttp2ConnectionStrategyScenario()

        println(
            buildString {
                appendLine("Segmented Download Controlled Benchmark")
                appendLine(
                    "scenario,mode,bytes,elapsed_ms,avg_mib_s,parallel,workers," +
                        "physical_connections,requests,strategy,protocol,retry,steal,handoff,speedup",
                )
                appendRows(capped)
                appendRows(slowHead)
                appendRows(http2)
            },
        )

        assertTrue(capped.speedup("segmented_balanced") >= 2.0)
        assertTrue(capped.speedup("segmented_foreground_boost") >= 2.0)
        assertTrue(slowHead.speedup("segmented_balanced") >= 2.0)
        assertTrue(slowHead.speedup("segmented_foreground_boost") >= 2.0)

        val sharedHttp2 = http2.row("segmented_shared")
        val isolatedHttp2 = http2.row("segmented_isolated")
        assertEquals(1, sharedHttp2.physicalConnections)
        assertTrue(isolatedHttp2.physicalConnections > sharedHttp2.physicalConnections)
        assertEquals("h2_prior_knowledge", sharedHttp2.protocol)
        assertEquals("h2_prior_knowledge", isolatedHttp2.protocol)
    }

    private suspend fun runPerConnectionCapScenario(): ScenarioRows {
        val bytes = ByteArray(24 * MIB) { (it * 31).toByte() }
        return MockWebServer().use { server ->
            server.protocols = BenchmarkTransport.Http1.protocols
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    rangeOrFullResponse(bytes, request.getHeader("Range"))
                        .throttleBody(128L * 1024L, 50L, TimeUnit.MILLISECONDS)
            }
            runSpeedProfileScenario(
                name = "per_connection_cap_http1",
                server = server,
                bytes = bytes,
                transport = BenchmarkTransport.Http1,
            )
        }
    }

    private suspend fun runSlowHeadScenario(): ScenarioRows {
        val bytes = ByteArray(30 * MIB) { (it * 17).toByte() }
        return MockWebServer().use { server ->
            server.protocols = BenchmarkTransport.Http1.protocols
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val range = request.getHeader("Range")
                    val response = rangeOrFullResponse(bytes, range)
                    return if (range == null || range.startsWith("bytes=0-")) {
                        response.throttleBody(16L * 1024L, 40L, TimeUnit.MILLISECONDS)
                    } else {
                        response.throttleBody(256L * 1024L, 40L, TimeUnit.MILLISECONDS)
                    }
                }
            }
            runSpeedProfileScenario(
                name = "slow_head_http1",
                server = server,
                bytes = bytes,
                transport = BenchmarkTransport.Http1,
            )
        }
    }

    private suspend fun runHttp2ConnectionStrategyScenario(): ScenarioRows {
        val bytes = ByteArray(24 * MIB) { (it * 13).toByte() }
        return MockWebServer().use { server ->
            server.protocols = BenchmarkTransport.Http2PriorKnowledge.protocols
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    rangeOrFullResponse(bytes, request.getHeader("Range"))
                        .throttleBody(128L * 1024L, 50L, TimeUnit.MILLISECONDS)
            }
            val baseline = runPlainGet(
                server = server,
                bytes = bytes,
                outputFile = temp.newFile("http2-baseline.bin").apply { delete() },
                transport = BenchmarkTransport.Http2PriorKnowledge,
            )
            val shared = runSegmented(
                server = server,
                bytes = bytes,
                outputFile = temp.newFile("http2-shared.bin").apply { delete() },
                partSizeBytes = 4L * MIB,
                maxConnections = 4,
                speedProfile = SegmentedDownloadSpeedProfile.Balanced,
                connectionStrategy = SegmentedDownloadConnectionStrategy.Shared,
                transport = BenchmarkTransport.Http2PriorKnowledge,
            ).copy(mode = "segmented_shared")
            val isolated = runSegmented(
                server = server,
                bytes = bytes,
                outputFile = temp.newFile("http2-isolated.bin").apply { delete() },
                partSizeBytes = 4L * MIB,
                maxConnections = 4,
                speedProfile = SegmentedDownloadSpeedProfile.Balanced,
                connectionStrategy = SegmentedDownloadConnectionStrategy.IsolatedPerWorker,
                transport = BenchmarkTransport.Http2PriorKnowledge,
            ).copy(mode = "segmented_isolated")
            ScenarioRows(
                name = "per_stream_cap_h2",
                rows = listOf(baseline, shared, isolated),
            )
        }
    }

    private suspend fun runSpeedProfileScenario(
        name: String,
        server: MockWebServer,
        bytes: ByteArray,
        transport: BenchmarkTransport,
    ): ScenarioRows {
        val baseline = runPlainGet(
            server = server,
            bytes = bytes,
            outputFile = temp.newFile("$name-baseline.bin").apply { delete() },
            transport = transport,
        )
        val balanced = runSegmented(
            server = server,
            bytes = bytes,
            outputFile = temp.newFile("$name-balanced.bin").apply { delete() },
            partSizeBytes = 8L * MIB,
            maxConnections = 4,
            speedProfile = SegmentedDownloadSpeedProfile.Balanced,
            connectionStrategy = SegmentedDownloadConnectionStrategy.IsolatedPerWorker,
            transport = transport,
        ).copy(mode = "segmented_balanced")
        val foregroundBoost = runSegmented(
            server = server,
            bytes = bytes,
            outputFile = temp.newFile("$name-foreground-boost.bin").apply { delete() },
            partSizeBytes = 4L * MIB,
            maxConnections = 8,
            speedProfile = SegmentedDownloadSpeedProfile.ForegroundBoost,
            connectionStrategy = SegmentedDownloadConnectionStrategy.IsolatedPerWorker,
            transport = transport,
        ).copy(mode = "segmented_foreground_boost")
        return ScenarioRows(
            name = name,
            rows = listOf(baseline, balanced, foregroundBoost),
        )
    }

    private suspend fun runPlainGet(
        server: MockWebServer,
        bytes: ByteArray,
        outputFile: File,
        transport: BenchmarkTransport,
    ): BenchmarkRow =
        withBenchmarkClient(transport) { client, tracker ->
            withContext(Dispatchers.IO) {
                val requestCountBefore = server.requestCount
                val request = Request.Builder()
                    .url(server.url("/artifact.bin"))
                    .get()
                    .build()
                val startedNs = System.nanoTime()
                client.newCall(request).execute().use { response ->
                    check(response.isSuccessful) { "HTTP ${response.code}" }
                    response.body.byteStream().use { input ->
                        FileOutputStream(outputFile).use { output ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                if (read == 0) continue
                                output.write(buffer, 0, read)
                            }
                            output.flush()
                            output.fd.sync()
                        }
                    }
                }
                val elapsedMs = elapsedMs(startedNs)
                assertContentEquals(bytes, outputFile.readBytes())
                BenchmarkRow(
                    mode = "plain_get",
                    bytes = outputFile.length(),
                    elapsedMs = elapsedMs,
                    parallel = false,
                    workerConnections = 1,
                    physicalConnections = tracker.physicalConnectionCount,
                    requestCount = server.requestCount - requestCountBefore,
                    connectionStrategy = "Single",
                    protocol = tracker.protocolLabel(transport.label),
                )
            }
        }

    private suspend fun runSegmented(
        server: MockWebServer,
        bytes: ByteArray,
        outputFile: File,
        partSizeBytes: Long,
        maxConnections: Int,
        speedProfile: SegmentedDownloadSpeedProfile,
        connectionStrategy: SegmentedDownloadConnectionStrategy,
        transport: BenchmarkTransport,
    ): BenchmarkRow =
        withBenchmarkClient(transport) { client, tracker ->
            val downloader = SegmentedDownloadClient(client = client, dispatcher = Dispatchers.IO)
            val requestCountBefore = server.requestCount
            var activeConnections = 0
            val startedNs = System.nanoTime()
            val result = downloader.downloadToFile(
                request = SegmentedDownloadRequest(
                    url = server.url("/artifact.bin").toString(),
                    outputFile = outputFile,
                    expectedSizeBytes = bytes.size.toLong(),
                ),
                options = SegmentedDownloadOptions(
                    minParallelSizeBytes = 1L,
                    initialPartSizeBytes = partSizeBytes,
                    maxConnections = maxConnections,
                    maxRetriesPerPart = 3,
                    retryDelayMs = 1L,
                    progressIntervalMs = 0L,
                    requireHttpsForParallel = false,
                    bufferSizeBytes = BUFFER_SIZE,
                    speedProfile = speedProfile,
                    connectionStrategy = connectionStrategy,
                ),
                onProgress = { progress ->
                    activeConnections = progress.activeConnections
                },
            )
            val elapsedMs = elapsedMs(startedNs)
            assertContentEquals(bytes, outputFile.readBytes())
            BenchmarkRow(
                mode = "segmented",
                bytes = outputFile.length(),
                elapsedMs = elapsedMs,
                parallel = result.parallel,
                workerConnections = max(activeConnections, 1),
                physicalConnections = tracker.physicalConnectionCount,
                requestCount = server.requestCount - requestCountBefore,
                connectionStrategy = connectionStrategy.name,
                protocol = tracker.protocolLabel(transport.label),
                retryCount = result.retryCount,
                stealCount = result.stealCount,
                handoffCount = result.handoffCount,
            )
        }

    private suspend fun <T> withBenchmarkClient(
        transport: BenchmarkTransport,
        block: suspend (OkHttpClient, BenchmarkConnectionTracker) -> T,
    ): T {
        val tracker = BenchmarkConnectionTracker()
        val client = OkHttpClient.Builder()
            .connectTimeout(10L, TimeUnit.SECONDS)
            .readTimeout(2L, TimeUnit.MINUTES)
            .writeTimeout(2L, TimeUnit.MINUTES)
            .callTimeout(3L, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .protocols(transport.protocols)
            .eventListener(tracker)
            .build()
        return try {
            block(client, tracker)
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
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
        val baselineElapsedMs = rows.row("plain_get").elapsedMs
        rows.rows.forEach { row ->
            appendLine(
                listOf(
                    rows.name,
                    row.mode,
                    row.bytes,
                    row.elapsedMs,
                    row.averageMiBs().format2(),
                    row.parallel,
                    row.workerConnections,
                    row.physicalConnections,
                    row.requestCount,
                    row.connectionStrategy,
                    row.protocol,
                    row.retryCount,
                    row.stealCount,
                    row.handoffCount,
                    (baselineElapsedMs.toDouble() / row.elapsedMs.toDouble()).format2(),
                ).joinToString(separator = ","),
            )
        }
    }

    private fun isControlledBenchmarkEnabled(): Boolean =
        System.getProperty("keios.download.controlledBenchmark")
            ?.let { value -> value.equals("true", ignoreCase = true) || value == "1" } == true

    private fun elapsedMs(startedNs: Long): Long =
        ((System.nanoTime() - startedNs) / 1_000_000L).coerceAtLeast(1L)

    private fun BenchmarkRow.averageMiBs(): Double =
        bytes.toDouble() / MIB.toDouble() / (elapsedMs.toDouble() / 1_000.0)

    private fun Double.format2(): String =
        String.format(Locale.US, "%.2f", this)

    private data class ScenarioRows(
        val name: String,
        val rows: List<BenchmarkRow>,
    ) {
        fun row(mode: String): BenchmarkRow = rows.single { it.mode == mode }

        fun speedup(mode: String): Double =
            row("plain_get").elapsedMs.toDouble() / row(mode).elapsedMs.toDouble()
    }

    private data class BenchmarkRow(
        val mode: String,
        val bytes: Long,
        val elapsedMs: Long,
        val parallel: Boolean,
        val workerConnections: Int,
        val physicalConnections: Int,
        val requestCount: Int,
        val connectionStrategy: String,
        val protocol: String,
        val retryCount: Int = 0,
        val stealCount: Int = 0,
        val handoffCount: Int = 0,
    )

    private enum class BenchmarkTransport(
        val label: String,
        val protocols: List<Protocol>,
    ) {
        Http1("http/1.1", listOf(Protocol.HTTP_1_1)),
        Http2PriorKnowledge("h2_prior_knowledge", listOf(Protocol.H2_PRIOR_KNOWLEDGE)),
    }

    private companion object {
        private const val MIB = 1024 * 1024
        private const val BUFFER_SIZE = 64 * 1024
    }
}
