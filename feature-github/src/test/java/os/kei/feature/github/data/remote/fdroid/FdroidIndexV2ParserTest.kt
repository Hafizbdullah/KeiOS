package os.kei.feature.github.data.remote.fdroid

import org.junit.Test
import os.kei.feature.github.model.FdroidIndexFormat
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FdroidIndexV2ParserTest {
    @Test
    fun `parseIndex reads repo metadata and package versions`() {
        val snapshot = FdroidIndexV2Parser.parseIndex(
            repoUrl = "https://f-droid.org/repo",
            rawJson = fdroidIndexFixture
        ).getOrThrow()

        assertEquals("https://f-droid.org/repo", snapshot.repoUrl)
        assertEquals(FdroidIndexFormat.V2, snapshot.format)
        assertEquals("F-Droid", snapshot.repoName)
        assertEquals("Free and Open Source Android apps", snapshot.repoDescription)
        assertEquals(1780000000000L, snapshot.timestampMillis)
        assertEquals(listOf("https://mirror.example/fdroid/repo"), snapshot.mirrors)
        assertEquals(1, snapshot.packageCount)

        val pkg = snapshot.packageSnapshot("org.fdroid.fdroid")

        assertNotNull(pkg)
        assertEquals("F-Droid", pkg.appName)
        assertEquals("App store", pkg.summary)
        assertEquals("GPL-3.0-or-later", pkg.license)
        assertEquals("https://gitlab.com/fdroid/fdroidclient", pkg.sourceCodeUrl)
        assertEquals(listOf("System"), pkg.categories)
        assertEquals(listOf("Tracking"), pkg.antiFeatures.map { it.id })
        assertEquals(1021051L, pkg.suggestedVersionCode)
        assertEquals(listOf(1021051L, 1020000L), pkg.versions.map { it.versionCode })

        val latest = pkg.versions.first()

        assertEquals("1.21.1", latest.versionName)
        assertEquals("org.fdroid.fdroid_1021051.apk", latest.apkName)
        assertEquals("/repo/org.fdroid.fdroid_1021051.apk", latest.apkPath)
        assertEquals("new-sha256", latest.apkSha256)
        assertEquals(123456L, latest.apkSizeBytes)
        assertEquals(23, latest.minSdk)
        assertEquals(35, latest.targetSdk)
        assertEquals(listOf("arm64-v8a", "armeabi-v7a"), latest.nativeAbis)
        assertEquals(listOf("signer-sha256"), latest.signerSha256)
        assertEquals(listOf("Beta"), latest.releaseChannels)
        assertEquals("Latest fixes", latest.whatsNew)
        assertEquals(listOf("KnownVuln"), latest.antiFeatures.map { it.id })
    }

    @Test
    fun `parseIndex supports compact string metadata`() {
        val snapshot = FdroidIndexV2Parser.parseIndex(
            repoUrl = "https://repo.example/fdroid/repo",
            rawJson = """
                {
                  "repo": {
                    "name": "Example Repo",
                    "timestamp": 1780000000001
                  },
                  "packages": {
                    "com.example.app": {
                      "metadata": {
                        "name": "Example",
                        "summary": "Summary",
                        "suggestedVersionCode": "20"
                      },
                      "versions": {
                        "com.example.app_20.apk": {
                          "manifest": {
                            "versionName": "2.0",
                            "versionCode": "20",
                            "usesSdk": {
                              "minSdkVersion": "23",
                              "targetSdkVersion": "35"
                            }
                          },
                          "file": {
                            "name": "com.example.app_20.apk",
                            "sha256": "hash",
                            "size": "2048"
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()
        ).getOrThrow()

        val pkg = snapshot.packageSnapshot("com.example.app")

        assertEquals("Example Repo", snapshot.repoName)
        assertEquals(20L, pkg?.suggestedVersionCode)
        assertEquals("2.0", pkg?.versions?.first()?.versionName)
        assertEquals(2048L, pkg?.versions?.first()?.apkSizeBytes)
    }

    private val fdroidIndexFixture: String =
        """
        {
          "repo": {
            "name": {
              "en-US": "F-Droid"
            },
            "description": {
              "en-US": "Free and Open Source Android apps"
            },
            "timestamp": 1780000000000,
            "mirrors": [
              {
                "url": "https://mirror.example/fdroid/repo"
              }
            ]
          },
          "packages": {
            "org.fdroid.fdroid": {
              "metadata": {
                "name": {
                  "en-US": "F-Droid"
                },
                "summary": {
                  "en-US": "App store"
                },
                "description": {
                  "en-US": "Client for F-Droid repositories"
                },
                "license": "GPL-3.0-or-later",
                "sourceCode": "https://gitlab.com/fdroid/fdroidclient",
                "changelog": "https://f-droid.org/packages/org.fdroid.fdroid/changelog",
                "categories": [
                  "System"
                ],
                "antiFeatures": [
                  "Tracking"
                ],
                "suggestedVersionCode": 1021051
              },
              "versions": {
                "org.fdroid.fdroid_1020000.apk": {
                  "manifest": {
                    "versionName": "1.20.0",
                    "versionCode": 1020000,
                    "usesSdk": {
                      "minSdkVersion": 21,
                      "targetSdkVersion": 34
                    }
                  },
                  "file": {
                    "name": "/repo/org.fdroid.fdroid_1020000.apk",
                    "sha256": "old-sha256",
                    "size": 120000
                  }
                },
                "org.fdroid.fdroid_1021051.apk": {
                  "manifest": {
                    "versionName": "1.21.1",
                    "versionCode": 1021051,
                    "usesSdk": {
                      "minSdkVersion": 23,
                      "targetSdkVersion": 35
                    },
                    "nativecode": [
                      "arm64-v8a",
                      "armeabi-v7a"
                    ],
                    "signer": {
                      "sha256": [
                        "signer-sha256"
                      ]
                    }
                  },
                  "file": {
                    "name": "/repo/org.fdroid.fdroid_1021051.apk",
                    "sha256": "new-sha256",
                    "size": 123456
                  },
                  "added": 1780000000000,
                  "whatsNew": {
                    "en-US": "Latest fixes"
                  },
                  "releaseChannels": [
                    "Beta"
                  ],
                  "antiFeatures": [
                    "KnownVuln"
                  ]
                }
              }
            }
          }
        }
        """.trimIndent()
}
