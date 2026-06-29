package os.kei.feature.github.data.remote.fdroid

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FdroidPackageApiClientTest {
    @Test
    fun `fetchPackage reads package API versions and suggested version`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "packageName": "org.fdroid.fdroid",
                          "suggestedVersionCode": "1021051",
                          "packages": [
                            {
                              "versionName": "1.21.1",
                              "versionCode": 1021051,
                              "apkName": "org.fdroid.fdroid_1021051.apk",
                              "hash": "sha256-new",
                              "size": 123456,
                              "minSdkVersion": 23,
                              "targetSdkVersion": 35,
                              "added": 1780000000000
                            },
                            {
                              "versionName": "1.20.0",
                              "versionCode": "1020000",
                              "apkName": "org.fdroid.fdroid_1020000.apk",
                              "hash": "sha256-old",
                              "size": "120000",
                              "minSdkVersion": "21",
                              "targetSdkVersion": "34"
                            }
                          ]
                        }
                        """.trimIndent()
                    )
            )

            val result = FdroidPackageApiClient()
                .fetchPackage(
                    repoBaseUrl = server.url("/repo").toString(),
                    packageName = "org.fdroid.fdroid"
                )
                .getOrThrow()

            assertEquals("/api/v1/packages/org.fdroid.fdroid", server.takeRequest().path)
            assertEquals(server.url("/repo").toString().trimEnd('/'), result.repoUrl)
            assertEquals("org.fdroid.fdroid", result.packageName)
            assertEquals(1021051L, result.suggestedVersionCode)
            assertEquals(listOf(1021051L, 1020000L), result.versions.map { it.versionCode })
            assertEquals("1.21.1", result.versions.first().versionName)
            assertEquals("sha256-new", result.versions.first().apkSha256)
            assertEquals(123456L, result.versions.first().apkSizeBytes)
            assertEquals(23, result.versions.first().minSdk)
            assertEquals(35, result.versions.first().targetSdk)
            assertEquals(1780000000000L, result.versions.first().addedAtMillis)
        }
    }

    @Test
    fun `fetchPackage falls back to repo scoped api path for third party hosts`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "packageName": "dev.imranr.obtainium",
                          "suggestedVersionCode": 200,
                          "packages": [
                            {
                              "versionName": "2.0.0",
                              "versionCode": 200,
                              "apkName": "dev.imranr.obtainium_200.apk"
                            }
                          ]
                        }
                        """.trimIndent()
                    )
            )

            val result = FdroidPackageApiClient()
                .fetchPackage(
                    repoBaseUrl = server.url("/fdroid/repo").toString(),
                    packageName = "dev.imranr.obtainium"
                )
                .getOrThrow()

            assertEquals("/fdroid/api/v1/packages/dev.imranr.obtainium", server.takeRequest().path)
            assertEquals(200L, result.selectedSuggestedVersion?.versionCode)
        }
    }

    @Test
    fun `fetchPackage returns failure when api response is not successful`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))

            val result = FdroidPackageApiClient()
                .fetchPackage(
                    repoBaseUrl = server.url("/repo").toString(),
                    packageName = "missing.package"
                )

            assertTrue(result.isFailure)
        }
    }
}
