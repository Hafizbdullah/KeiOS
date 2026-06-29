package os.kei.feature.github.domain.fdroid

import kotlinx.coroutines.runBlocking
import org.junit.Test
import os.kei.feature.github.data.remote.fdroid.FdroidPackageSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidRepositorySnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidSearchApiApp
import os.kei.feature.github.data.remote.fdroid.FdroidSearchApiClient
import os.kei.feature.github.data.remote.fdroid.FdroidVersionSnapshot
import os.kei.feature.github.model.FdroidAppSearchRequest
import os.kei.feature.github.model.FdroidAppSearchSource
import os.kei.feature.github.model.FdroidIndexFormat
import os.kei.feature.github.model.FdroidRepositoryPresets
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FdroidAppSearchServiceTest {
    @Test
    fun `search uses official search api for fdroid main name lookup`() = runBlocking {
        val service = FdroidAppSearchService(
            searchApiClient = fakeSearchApiClient(
                apps = listOf(
                    FdroidSearchApiApp(
                        name = "AntennaPod",
                        summary = "Podcast player",
                        iconUrl = "https://example.test/icon.png",
                        packagePageUrl = "https://f-droid.org/en/packages/de.danoeh.antennapod",
                        packageName = "de.danoeh.antennapod"
                    )
                )
            ),
            repositoryProvider = emptyRepositoryProvider()
        )

        val result = service.search(
            FdroidAppSearchRequest(
                query = "antenna",
                repoUrls = listOf(FdroidRepositoryPresets.Main.repoUrl)
            )
        ).getOrThrow()

        assertEquals(1, result.candidates.size)
        val candidate = result.candidates.single()
        assertEquals("de.danoeh.antennapod", candidate.packageName)
        assertEquals("AntennaPod", candidate.appName)
        assertEquals(FdroidAppSearchSource.OfficialSearchApi, candidate.source)
        assertEquals(FdroidRepositoryPresets.MAIN_ID, candidate.repoPresetId)
    }

    @Test
    fun `package scan can find app on izzy when fdroid main misses`() = runBlocking {
        val service = FdroidAppSearchService(
            searchApiClient = fakeSearchApiClient(apps = emptyList()),
            repositoryProvider = repositoryProvider(
                FdroidRepositoryPresets.IzzyOnDroid.repoUrl to repository(
                    repoUrl = FdroidRepositoryPresets.IzzyOnDroid.repoUrl,
                    repoName = "IzzyOnDroid",
                    packages = listOf(
                        packageSnapshot(
                            packageName = "com.perol.asdpl.play.pixivez",
                            appName = "PixEz",
                            summary = "Pixiv client"
                        )
                    )
                )
            )
        )

        val result = service.search(
            FdroidAppSearchRequest(
                packageName = "com.perol.asdpl.play.pixivez",
                repoUrls = listOf(
                    FdroidRepositoryPresets.Main.repoUrl,
                    FdroidRepositoryPresets.IzzyOnDroid.repoUrl
                )
            )
        ).getOrThrow()

        assertEquals(1, result.candidates.size)
        val candidate = result.candidates.single()
        assertEquals(FdroidRepositoryPresets.IzzyOnDroid.repoUrl, candidate.repoUrl)
        assertEquals(FdroidRepositoryPresets.IZZY_ID, candidate.repoPresetId)
        assertEquals("PixEz", candidate.appName)
        assertEquals("com.perol.asdpl.play.pixivez", candidate.packageName)
        assertEquals("1.0", candidate.latestVersionName)
        assertTrue(result.failures.isEmpty())
    }

    @Test
    fun `fdroid main falls back to repository index when official search api returns empty`() =
        runBlocking {
            val service = FdroidAppSearchService(
                searchApiClient = fakeSearchApiClient(apps = emptyList()),
                repositoryProvider = repositoryProvider(
                    FdroidRepositoryPresets.Main.repoUrl to repository(
                        repoUrl = FdroidRepositoryPresets.Main.repoUrl,
                        repoName = "F-Droid",
                        packages = listOf(
                            packageSnapshot(
                                packageName = "de.danoeh.antennapod",
                                appName = "AntennaPod",
                                summary = "Podcast player"
                            )
                        )
                    )
                )
            )

            val result = service.search(
                FdroidAppSearchRequest(
                    query = "AntennaPod",
                    repoUrls = listOf(FdroidRepositoryPresets.Main.repoUrl)
                )
            ).getOrThrow()

            assertEquals(listOf("de.danoeh.antennapod"), result.candidates.map { it.packageName })
            assertEquals(FdroidAppSearchSource.RepositoryIndex, result.candidates.single().source)
        }

    @Test
    fun `installed app scan can fall back to app name when fdroid package differs`() = runBlocking {
        val service = FdroidAppSearchService(
            searchApiClient = fakeSearchApiClient(apps = emptyList()),
            repositoryProvider = repositoryProvider(
                FdroidRepositoryPresets.IzzyOnDroid.repoUrl to repository(
                    repoUrl = FdroidRepositoryPresets.IzzyOnDroid.repoUrl,
                    repoName = "IzzyOnDroid",
                    packages = listOf(
                        packageSnapshot(
                            packageName = "com.perol.pixez",
                            appName = "PixEz",
                            summary = "Pixiv client"
                        )
                    )
                )
            )
        )

        val result = service.search(
            FdroidAppSearchRequest(
                query = "PixEz",
                packageName = "com.perol.play.pixez",
                repoUrls = listOf(
                    FdroidRepositoryPresets.Main.repoUrl,
                    FdroidRepositoryPresets.IzzyOnDroid.repoUrl
                )
            )
        ).getOrThrow()

        assertEquals(listOf("com.perol.pixez"), result.candidates.map { it.packageName })
        val candidate = result.candidates.single()
        assertEquals(FdroidRepositoryPresets.IzzyOnDroid.repoUrl, candidate.repoUrl)
        assertEquals("PixEz", candidate.appName)
        assertTrue(candidate.score < 1000)
        assertTrue(result.failures.isEmpty())
    }

    @Test
    fun `name lookup filters third party repository index`() = runBlocking {
        val service = FdroidAppSearchService(
            searchApiClient = fakeSearchApiClient(apps = emptyList()),
            repositoryProvider = repositoryProvider(
                FdroidRepositoryPresets.IzzyOnDroid.repoUrl to repository(
                    repoUrl = FdroidRepositoryPresets.IzzyOnDroid.repoUrl,
                    repoName = "IzzyOnDroid",
                    packages = listOf(
                        packageSnapshot("dev.imranr.obtainium", "Obtainium", "App updater"),
                        packageSnapshot("com.perol.asdpl.play.pixivez", "PixEz", "Pixiv client")
                    )
                )
            )
        )

        val result = service.search(
            FdroidAppSearchRequest(
                query = "pixez",
                repoUrls = listOf(FdroidRepositoryPresets.IzzyOnDroid.repoUrl)
            )
        ).getOrThrow()

        assertEquals(listOf("com.perol.asdpl.play.pixivez"), result.candidates.map { it.packageName })
    }

    private fun fakeSearchApiClient(apps: List<FdroidSearchApiApp>): FdroidSearchApiClient {
        return object : FdroidSearchApiClient() {
            override suspend fun searchApps(
                query: String,
                limit: Int
            ): Result<List<FdroidSearchApiApp>> {
                return Result.success(apps.take(limit))
            }
        }
    }

    private fun emptyRepositoryProvider(): FdroidRepositorySnapshotProvider =
        FdroidRepositorySnapshotProvider { repoUrl, _ ->
            Result.failure(IllegalStateException("No fixture for $repoUrl"))
        }

    private fun repositoryProvider(
        vararg snapshots: Pair<String, FdroidRepositorySnapshot>
    ): FdroidRepositorySnapshotProvider {
        val byUrl = snapshots.associateBy(
            keySelector = { (url, _) -> url },
            valueTransform = { (_, snapshot) -> snapshot }
        )
        return FdroidRepositorySnapshotProvider { repoUrl, _ ->
            byUrl[repoUrl]?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("No fixture for $repoUrl"))
        }
    }

    private fun repository(
        repoUrl: String,
        repoName: String,
        packages: List<FdroidPackageSnapshot>
    ): FdroidRepositorySnapshot {
        return FdroidRepositorySnapshot(
            repoUrl = repoUrl,
            format = FdroidIndexFormat.V2,
            repoName = repoName,
            repoDescription = "",
            timestampMillis = 1780000000000,
            mirrors = emptyList(),
            packages = packages.associateBy { snapshot -> snapshot.packageName }
        )
    }

    private fun packageSnapshot(
        packageName: String,
        appName: String,
        summary: String
    ): FdroidPackageSnapshot {
        return FdroidPackageSnapshot(
            repoUrl = "",
            packageName = packageName,
            suggestedVersionCode = 100,
            appName = appName,
            summary = summary,
            categories = listOf("Internet"),
            versions = listOf(
                FdroidVersionSnapshot(
                    versionName = "1.0",
                    versionCode = 100,
                    apkName = "${packageName}_100.apk",
                    apkPath = "${packageName}_100.apk",
                    apkSha256 = "sha256",
                    apkSizeBytes = 1024,
                    addedAtMillis = 1780000000000,
                    minSdk = 23,
                    targetSdk = 35,
                    nativeAbis = emptyList(),
                    signerSha256 = emptyList(),
                    releaseChannels = emptyList(),
                    whatsNew = "",
                    antiFeatures = emptyList()
                )
            )
        )
    }
}
