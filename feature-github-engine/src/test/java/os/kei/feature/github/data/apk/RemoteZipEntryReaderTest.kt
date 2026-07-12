package os.kei.feature.github.data.apk

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import os.kei.core.download.range.RemoteByteRangeResourceChangedException
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RemoteZipEntryReaderTest {
    @Test
    fun `reader uses bounded byte ranges to list and read zip entries`() = runBlocking {
        val manifest = "manifest-content".encodeToByteArray()
        val zipBytes = zipBytes("AndroidManifest.xml" to manifest, "assets/info.txt" to byteArrayOf(1, 2, 3))
        MockWebServer().use { server ->
            server.dispatcher = zipRangeDispatcher(zipBytes)
            val reader = RemoteZipEntryReader(OkHttpClient())
            val url = server.url("/app.apk").toString()

            val names = reader.listEntryNames(url).getOrThrow()
            val content = reader.readEntry(url, "AndroidManifest.xml").getOrThrow()

            assertEquals(listOf("AndroidManifest.xml", "assets/info.txt"), names)
            assertContentEquals(manifest, content)
            assertTrue(server.requestCount >= 4)
            repeat(server.requestCount) {
                assertTrue(server.takeRequest().getHeader("Range")?.startsWith("bytes=") == true)
            }
        }
    }

    @Test
    fun `reader rejects a zip that changes after probing`() = runBlocking {
        val zipBytes = zipBytes("AndroidManifest.xml" to "manifest-content".encodeToByteArray())
        MockWebServer().use { server ->
            var requestIndex = 0
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        requestIndex += 1
                        val response = zipRangeResponse(request, zipBytes)
                        return response.addHeader("ETag", if (requestIndex == 1) "\"v1\"" else "\"v2\"")
                    }
                }
            val reader = RemoteZipEntryReader(OkHttpClient())

            val failure = reader.listEntryNames(server.url("/changing.apk").toString()).exceptionOrNull()

            assertIs<RemoteByteRangeResourceChangedException>(failure)
            assertEquals(null, server.takeRequest().getHeader("If-Range"))
            assertEquals("\"v1\"", server.takeRequest().getHeader("If-Range"))
        }
    }
}

private fun zipBytes(vararg entries: Pair<String, ByteArray>): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        entries.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return output.toByteArray()
}

private fun zipRangeDispatcher(bytes: ByteArray): Dispatcher =
    object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return zipRangeResponse(request, bytes)
        }
    }

private fun zipRangeResponse(request: RecordedRequest, bytes: ByteArray): MockResponse {
    val value = request.getHeader("Range").orEmpty().removePrefix("bytes=")
    val parts = value.split("-", limit = 2)
    val start = parts[0].toInt()
    val endInclusive = parts[1].toInt().coerceAtMost(bytes.lastIndex)
    return MockResponse()
        .setResponseCode(206)
        .addHeader("Content-Range", "bytes $start-$endInclusive/${bytes.size}")
        .setBody(Buffer().write(bytes.copyOfRange(start, endInclusive + 1)))
}
