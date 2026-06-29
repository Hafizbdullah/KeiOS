package os.kei.feature.github.domain.fdroid

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import os.kei.feature.github.data.remote.fdroid.FdroidPackageSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidVersionSnapshot
import os.kei.feature.github.model.GITHUB_FDROID_STRATEGY_ID
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedReleaseStatus
import os.kei.feature.github.model.GitHubTrackedSourceMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FdroidReleaseCheckSourceTest {
    @Test
    fun `evaluate maps selected fdroid candidate to update check`() = runBlocking {
        val source = FdroidReleaseCheckSource(
            snapshotProvider = staticSnapshotProvider(
                FdroidPackageSnapshot(
                    repoUrl = "https://f-droid.org/repo",
                    packageName = "org.fdroid.fdroid",
                    suggestedVersionCode = 102,
                    appName = "F-Droid",
                    versions = listOf(
                        version(versionCode = 102, versionName = "1.2.0"),
                        version(versionCode = 100, versionName = "1.0.0")
                    )
                )
            ),
            ioDispatcher = Dispatchers.Unconfined,
            deviceSdkProvider = { 37 }
        )

        val result = source.evaluate(
            item = fdroidItem(),
            lookupConfig = GitHubLookupConfig(),
            localVersion = "1.0.0",
            localVersionCode = 100,
            forceRefresh = false
        )

        assertEquals(GITHUB_FDROID_STRATEGY_ID, result.strategyId)
        assertEquals(GitHubTrackedReleaseStatus.UpdateAvailable, result.status)
        assertEquals(true, result.hasUpdate)
        assertEquals("1.2.0 (102)", result.stableRelease?.displayVersion)
        assertEquals("102", result.preciseStableApkVersion?.versionCode)
        assertEquals("org.fdroid.fdroid_102.apk", result.preciseStableApkVersion?.assetName)
        assertEquals("fdroid_repository-v1", result.sourceConfigSignature.substringBefore('|'))
    }

    @Test
    fun `evaluate reports failure when package is missing`() = runBlocking {
        val source = FdroidReleaseCheckSource(
            snapshotProvider = staticSnapshotProvider(
                Result.failure(IllegalStateException("package missing"))
            ),
            ioDispatcher = Dispatchers.Unconfined,
            deviceSdkProvider = { 37 }
        )

        val result = source.evaluate(
            item = fdroidItem(),
            lookupConfig = GitHubLookupConfig(),
            localVersion = "",
            localVersionCode = -1L,
            forceRefresh = false
        )

        assertEquals(GitHubTrackedReleaseStatus.Failed, result.status)
        assertTrue(result.message.contains("package missing"))
    }

    private fun staticSnapshotProvider(
        snapshot: FdroidPackageSnapshot
    ): FdroidPackageSnapshotProvider {
        return staticSnapshotProvider(Result.success(snapshot))
    }

    private fun staticSnapshotProvider(
        result: Result<FdroidPackageSnapshot>
    ): FdroidPackageSnapshotProvider {
        return FdroidPackageSnapshotProvider { _, _ -> result }
    }

    private fun fdroidItem(): GitHubTrackedApp {
        return GitHubTrackedApp(
            repoUrl = "https://f-droid.org/repo",
            owner = "f-droid.org",
            repo = "repo",
            packageName = "org.fdroid.fdroid",
            appLabel = "F-Droid",
            sourceMode = GitHubTrackedSourceMode.FdroidRepository
        )
    }

    private fun version(
        versionCode: Long,
        versionName: String
    ): FdroidVersionSnapshot {
        return FdroidVersionSnapshot(
            versionName = versionName,
            versionCode = versionCode,
            apkName = "org.fdroid.fdroid_$versionCode.apk",
            apkPath = "/repo/org.fdroid.fdroid_$versionCode.apk",
            apkSha256 = "sha256-$versionCode",
            apkSizeBytes = versionCode,
            addedAtMillis = null,
            minSdk = 23,
            targetSdk = 35,
            nativeAbis = emptyList(),
            signerSha256 = emptyList(),
            releaseChannels = emptyList(),
            whatsNew = "notes $versionCode",
            antiFeatures = emptyList()
        )
    }
}
