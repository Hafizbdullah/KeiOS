package os.kei.feature.github.data.remote.fdroid

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import os.kei.core.io.BoundedContentTextReadTooLargeException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FdroidSearchApiClientTest {
    @Test
    fun `searchApps rejects oversized chunked response`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setChunkedBody("x".repeat(3 * 1024 * 1024), 16 * 1024),
            )

            val error = FdroidSearchApiClient(
                searchEndpoint = server.url("/api/search_apps").toString(),
            ).searchApps("demo").exceptionOrNull()

            assertTrue(error is BoundedContentTextReadTooLargeException)
        }
    }

    @Test
    fun `searchApps reads official search candidates and package names`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "apps": [
                            {
                              "name": "AntennaPod - Podcast Player",
                              "summary": "Podcast manager",
                              "icon": "https://example.test/icon.png",
                              "url": "https://f-droid.org/en/packages/de.danoeh.antennapod"
                            }
                          ]
                        }
                        """.trimIndent()
                    )
            )

            val apps = FdroidSearchApiClient(
                searchEndpoint = server.url("/api/search_apps").toString()
            ).searchApps("AntennaPod").getOrThrow()

            assertEquals("/api/search_apps?q=AntennaPod", server.takeRequest().path)
            assertEquals(1, apps.size)
            assertEquals("de.danoeh.antennapod", apps.single().packageName)
            assertEquals("AntennaPod - Podcast Player", apps.single().name)
        }
    }

    @Test
    fun `searchApps returns failure for http errors`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503).setBody("{}"))

            val result = FdroidSearchApiClient(
                searchEndpoint = server.url("/api/search_apps").toString()
            ).searchApps("demo")

            assertTrue(result.isFailure)
        }
    }
}
