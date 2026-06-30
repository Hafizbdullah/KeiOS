package os.kei.feature.github.data.remote.fdroid

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FdroidRepositoryIndexClientTest {
    @Test
    fun `searchIndexV2 reads candidates through repository index stream`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(indexFixture)
            )

            val snapshot = FdroidRepositoryIndexClient()
                .searchIndexV2(
                    repoBaseUrl = server.url("/fdroid/repo").toString(),
                    query = "PixEz",
                    packageName = "",
                    limit = 12
                )
                .getOrThrow()

            assertEquals("/fdroid/repo/index-v2.json", server.takeRequest().path)
            assertEquals(listOf("com.perol.pixez"), snapshot.packages.keys.toList())
            assertEquals("PixEz", snapshot.packageSnapshot("com.perol.pixez")?.appName)
            assertNull(snapshot.packageSnapshot("dev.imranr.obtainium"))
        }
    }

    @Test
    fun `fetchIndexV2Packages materializes only requested packages`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(indexFixture)
            )

            val snapshot = FdroidRepositoryIndexClient()
                .fetchIndexV2Packages(
                    repoBaseUrl = server.url("/fdroid/repo").toString(),
                    packageNames = setOf("dev.imranr.obtainium")
                )
                .getOrThrow()

            assertEquals("/fdroid/repo/index-v2.json", server.takeRequest().path)
            assertEquals(listOf("dev.imranr.obtainium"), snapshot.packages.keys.toList())
            assertEquals(3, snapshot.packageCount)
        }
    }

    @Test
    fun `searchIndexV2 returns failure for http errors`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))

            val result = FdroidRepositoryIndexClient()
                .searchIndexV2(
                    repoBaseUrl = server.url("/fdroid/repo").toString(),
                    query = "PixEz",
                    packageName = "",
                    limit = 12
                )

            assertTrue(result.isFailure)
        }
    }

    private val indexFixture: String =
        """
        {
          "repo": {
            "name": {
              "en-US": "IzzyOnDroid"
            },
            "timestamp": 1780000000000
          },
          "packages": {
            "com.perol.pixez": {
              "metadata": {
                "name": {
                  "en-US": "PixEz"
                },
                "summary": {
                  "en-US": "Pixiv client"
                },
                "suggestedVersionCode": 10010040
              },
              "versions": {
                "com.perol.pixez_10010040.apk": {
                  "manifest": {
                    "versionName": "0.9.104 wsv",
                    "versionCode": 10010040
                  },
                  "file": {
                    "name": "/repo/com.perol.pixez_10010040.apk",
                    "sha256": "pixez-sha256"
                  }
                }
              }
            },
            "dev.imranr.obtainium": {
              "metadata": {
                "name": {
                  "en-US": "Obtainium"
                },
                "summary": {
                  "en-US": "App updater"
                },
                "suggestedVersionCode": 200
              },
              "versions": {
                "dev.imranr.obtainium_200.apk": {
                  "manifest": {
                    "versionName": "2.0",
                    "versionCode": 200
                  },
                  "file": {
                    "name": "/repo/dev.imranr.obtainium_200.apk",
                    "sha256": "obtainium-sha256"
                  }
                }
              }
            },
            "org.fdroid.fdroid": {
              "metadata": {
                "name": {
                  "en-US": "F-Droid"
                },
                "summary": {
                  "en-US": "App store"
                },
                "suggestedVersionCode": 1021051
              },
              "versions": {
                "org.fdroid.fdroid_1021051.apk": {
                  "manifest": {
                    "versionName": "1.21.1",
                    "versionCode": 1021051
                  },
                  "file": {
                    "name": "/repo/org.fdroid.fdroid_1021051.apk",
                    "sha256": "fdroid-sha256"
                  }
                }
              }
            }
          }
        }
        """.trimIndent()
}
