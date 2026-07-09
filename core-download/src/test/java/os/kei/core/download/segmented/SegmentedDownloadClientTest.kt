package os.kei.core.download.segmented

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SegmentedDownloadClientTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `segmented download writes all bytes in correct order`() = runBlocking {
        val bytes = ByteArray(32) { it.toByte() }
        MockWebServer().use { server ->
            server.dispatcher = byteRangeDispatcher(bytes)
            val outputFile = temp.newFile("artifact.bin").apply { delete() }
            val progress = mutableListOf<SegmentedDownloadProgress>()

            val result = client().downloadToFile(
                request = request(server, outputFile),
                options = testOptions(partSizeBytes = 8, maxConnections = 4),
                onProgress = { progress += it },
            )

            assertEquals(true, result.parallel)
            assertEquals(true, result.rangeSupported)
            assertContentEquals(bytes, outputFile.readBytes())
            assertEquals(32, progress.last().downloadedBytes)
            assertEquals(4, progress.first { it.parallel }.activeConnections)
        }
    }

    @Test
    fun `range unavailable uses single stream fallback`() = runBlocking {
        val bytes = ByteArray(16) { (it + 1).toByte() }
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Length", bytes.size)
                        .setBody(Buffer().write(bytes))
                }
            }
            val outputFile = temp.newFile("fallback.bin").apply { delete() }

            val result = client().downloadToFile(
                request = request(server, outputFile),
                options = testOptions(partSizeBytes = 8, maxConnections = 4),
            )

            assertEquals(false, result.parallel)
            assertEquals(false, result.rangeSupported)
            assertEquals("range-ignored", result.fallbackReason)
            assertContentEquals(bytes, outputFile.readBytes())
        }
    }

    @Test
    fun `worker retries after partial EOF and resumes from offset`() = runBlocking {
        val bytes = ByteArray(12) { (it + 7).toByte() }
        val truncatedOnce = AtomicBoolean(false)
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val range = request.getHeader("Range").orEmpty()
                    if (range == "bytes=0-0") return rangeResponse(bytes, 0, 0)
                    if (range == "bytes=0-3" && truncatedOnce.compareAndSet(false, true)) {
                        return MockResponse()
                            .setResponseCode(206)
                            .addHeader("Content-Range", "bytes 0-3/${bytes.size}")
                            .setBody(Buffer().write(bytes.copyOfRange(0, 2)))
                    }
                    return rangeResponse(bytes, range)
                }
            }
            val outputFile = temp.newFile("partial.bin").apply { delete() }

            val result = client().downloadToFile(
                request = request(server, outputFile),
                options = testOptions(partSizeBytes = 4, maxConnections = 2),
            )

            assertEquals(true, result.parallel)
            assertEquals(1, result.retryCount)
            assertContentEquals(bytes, outputFile.readBytes())
            val ranges = server.takeAllRequests().map { it.getHeader("Range").orEmpty() }
            assertTrue("bytes=2-3" in ranges)
        }
    }

    @Test
    fun `worker retries after 429`() = runBlocking {
        val bytes = ByteArray(8) { (it + 11).toByte() }
        val throttledOnce = AtomicBoolean(false)
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val range = request.getHeader("Range").orEmpty()
                    if (range == "bytes=0-0") return rangeResponse(bytes, 0, 0)
                    if (range == "bytes=0-3" && throttledOnce.compareAndSet(false, true)) {
                        return MockResponse().setResponseCode(429)
                    }
                    return rangeResponse(bytes, range)
                }
            }
            val outputFile = temp.newFile("retry.bin").apply { delete() }

            val result = client().downloadToFile(
                request = request(server, outputFile),
                options = testOptions(partSizeBytes = 4, maxConnections = 2),
            )

            assertEquals(1, result.retryCount)
            assertContentEquals(bytes, outputFile.readBytes())
        }
    }

    @Test
    fun `parallel download replaces existing output and removes part file`() = runBlocking {
        val bytes = ByteArray(16) { (it + 3).toByte() }
        MockWebServer().use { server ->
            server.dispatcher = byteRangeDispatcher(bytes)
            val outputFile = temp.newFile("replace.bin").apply { writeText("old") }
            val partFile = File(outputFile.parentFile, "${outputFile.name}.part")

            client().downloadToFile(
                request = request(server, outputFile),
                options = testOptions(partSizeBytes = 8, maxConnections = 2),
            )

            assertContentEquals(bytes, outputFile.readBytes())
            assertFalse(partFile.exists())
        }
    }

    @Test
    fun `active connections clamp to part count`() = runBlocking {
        val bytes = ByteArray(12) { (it + 5).toByte() }
        MockWebServer().use { server ->
            server.dispatcher = byteRangeDispatcher(bytes)
            val outputFile = temp.newFile("connections.bin").apply { delete() }
            val progress = mutableListOf<SegmentedDownloadProgress>()

            client().downloadToFile(
                request = request(server, outputFile),
                options = testOptions(partSizeBytes = 8, maxConnections = 4),
                onProgress = { progress += it },
            )

            assertEquals(2, progress.first { it.parallel }.activeConnections)
            assertContentEquals(bytes, outputFile.readBytes())
        }
    }

    @Test
    fun `range response body is bounded to requested part`() = runBlocking {
        val bytes = ByteArray(12) { (it + 17).toByte() }
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val range = request.getHeader("Range").orEmpty()
                    if (range == "bytes=0-0") return rangeResponse(bytes, 0, 0)
                    if (range == "bytes=0-3") {
                        return MockResponse()
                            .setResponseCode(206)
                            .addHeader("Content-Range", "bytes 0-3/${bytes.size}")
                            .setBody(Buffer().write(bytes))
                    }
                    return rangeResponse(bytes, range)
                }
            }
            val outputFile = temp.newFile("bounded.bin").apply { delete() }

            client().downloadToFile(
                request = request(server, outputFile),
                options = testOptions(partSizeBytes = 4, maxConnections = 2),
            )

            assertContentEquals(bytes, outputFile.readBytes())
        }
    }

    @Test
    fun `cancellation removes temp part file`() = runBlocking {
        val bytes = ByteArray(128) { it.toByte() }
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val range = request.getHeader("Range").orEmpty()
                    if (range == "bytes=0-0") return rangeResponse(bytes, 0, 0)
                    return rangeResponse(bytes, range)
                        .throttleBody(1, 250, TimeUnit.MILLISECONDS)
                }
            }
            val outputFile = temp.newFile("cancel.bin").apply { delete() }
            val partFile = File(outputFile.parentFile, "${outputFile.name}.part")
            val started = AtomicInteger(0)
            val job = launch {
                client().downloadToFile(
                    request = request(server, outputFile),
                    options = testOptions(partSizeBytes = 64, maxConnections = 2),
                    onProgress = {
                        if (it.downloadedBytes > 0L) started.incrementAndGet()
                    },
                )
            }

            repeat(20) {
                if (partFile.exists() || started.get() > 0) return@repeat
                delay(50)
            }
            job.cancelAndJoin()

            assertFalse(partFile.exists())
            assertFalse(outputFile.exists())
        }
    }

    private fun client(): SegmentedDownloadClient =
        SegmentedDownloadClient(
            client = OkHttpClient(),
            dispatcher = Dispatchers.IO,
        )

    private fun request(
        server: MockWebServer,
        outputFile: File,
    ): SegmentedDownloadRequest =
        SegmentedDownloadRequest(
            url = server.url("/download.bin").toString(),
            outputFile = outputFile,
            headers = mapOf("User-Agent" to "KeiOS-Test"),
        )

    private fun testOptions(
        partSizeBytes: Long,
        maxConnections: Int,
    ): SegmentedDownloadOptions =
        SegmentedDownloadOptions(
            minParallelSizeBytes = 1,
            initialPartSizeBytes = partSizeBytes,
            maxConnections = maxConnections,
            maxRetriesPerPart = 2,
            retryDelayMs = 1,
            progressIntervalMs = 0,
            requireHttpsForParallel = false,
            bufferSizeBytes = 4,
        )

    private fun byteRangeDispatcher(bytes: ByteArray): Dispatcher =
        object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return rangeResponse(bytes, request.getHeader("Range").orEmpty())
            }
        }

    private fun rangeResponse(
        bytes: ByteArray,
        rangeHeader: String,
    ): MockResponse {
        if (rangeHeader.isBlank()) {
            return MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Length", bytes.size)
                .setBody(Buffer().write(bytes))
        }
        val parts = rangeHeader.removePrefix("bytes=").split("-", limit = 2)
        return rangeResponse(
            bytes = bytes,
            start = parts[0].toInt(),
            endInclusive = parts[1].toInt(),
        )
    }

    private fun rangeResponse(
        bytes: ByteArray,
        start: Int,
        endInclusive: Int,
    ): MockResponse {
        val safeEnd = endInclusive.coerceAtMost(bytes.lastIndex)
        return MockResponse()
            .setResponseCode(206)
            .addHeader("Content-Range", "bytes $start-$safeEnd/${bytes.size}")
            .setBody(Buffer().write(bytes.copyOfRange(start, safeEnd + 1)))
    }

    private fun MockWebServer.takeAllRequests(): List<RecordedRequest> =
        buildList {
            repeat(requestCount) {
                add(takeRequest())
            }
        }
}
