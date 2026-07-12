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
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
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
            val value = request.getHeader("Range").orEmpty().removePrefix("bytes=")
            val parts = value.split("-", limit = 2)
            val start = parts[0].toInt()
            val endInclusive = parts[1].toInt().coerceAtMost(bytes.lastIndex)
            return MockResponse()
                .setResponseCode(206)
                .addHeader("Content-Range", "bytes $start-$endInclusive/${bytes.size}")
                .setBody(Buffer().write(bytes.copyOfRange(start, endInclusive + 1)))
        }
    }
