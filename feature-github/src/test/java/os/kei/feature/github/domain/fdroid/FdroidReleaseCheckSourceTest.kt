package os.kei.feature.github.domain.fdroid

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import os.kei.feature.github.data.local.fdroid.FdroidMetadataSidecar
import os.kei.feature.github.data.remote.fdroid.FdroidPackageSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidRepositorySnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidVersionSnapshot
import os.kei.feature.github.model.FdroidIndexFormat
import os.kei.feature.github.model.FdroidTrackedAppConfig
import os.kei.feature.github.model.FdroidTrustPolicy
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
        var savedSidecar: FdroidMetadataSidecar? = null
        val packageSnapshot = FdroidPackageSnapshot(
            repoUrl = "https://f-droid.org/repo",
            packageName = "org.fdroid.fdroid",
            suggestedVersionCode = 102,
            appName = "F-Droid",
            versions = listOf(
                version(versionCode = 102, versionName = "1.2.0"),
                version(versionCode = 100, versionName = "1.0.0")
            )
        )
        val source = FdroidReleaseCheckSource(
            snapshotProvider = staticLookupSnapshotProvider(
                packageSnapshot = packageSnapshot,
                repositorySnapshot = repositorySnapshot(packageSnapshot)
            ),
            metadataWriter = { sidecar -> savedSidecar = sidecar },
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
        assertEquals("1.2.0", savedSidecar?.selectedVersion?.versionName)
        assertEquals("sha256-102", savedSidecar?.trust?.apkSha256)
        assertEquals("F-Droid", savedSidecar?.repo?.repoName)
        assertEquals(1, savedSidecar?.repo?.packageCount)
    }

    @Test
    fun `evaluate reports failure when package is missing`() = runBlocking {
        val source = FdroidReleaseCheckSource(
            snapshotProvider = staticSnapshotProvider(
                Result.failure(IllegalStateException("package missing"))
            ),
            metadataWriter = { },
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

    @Test
    fun `evaluate fails when apk hash policy cannot verify selected version`() = runBlocking {
        val packageSnapshot = FdroidPackageSnapshot(
            repoUrl = "https://f-droid.org/repo",
            packageName = "org.fdroid.fdroid",
            suggestedVersionCode = 102,
            appName = "F-Droid",
            versions = listOf(
                version(versionCode = 102, versionName = "1.2.0", apkSha256 = "")
            )
        )
        val source = FdroidReleaseCheckSource(
            snapshotProvider = staticLookupSnapshotProvider(
                packageSnapshot = packageSnapshot,
                repositorySnapshot = repositorySnapshot(packageSnapshot)
            ),
            metadataWriter = { },
            ioDispatcher = Dispatchers.Unconfined,
            deviceSdkProvider = { 37 }
        )

        val result = source.evaluate(
            item = fdroidItem(
                FdroidTrackedAppConfig(trustPolicy = FdroidTrustPolicy.RequireApkHash)
            ),
            lookupConfig = GitHubLookupConfig(),
            localVersion = "1.0.0",
            localVersionCode = 100,
            forceRefresh = false
        )

        assertEquals(GitHubTrackedReleaseStatus.Failed, result.status)
        assertTrue(result.message.contains("APK hash"))
    }

    @Test
    fun `evaluate fails when signer index policy cannot verify selected version`() = runBlocking {
        val packageSnapshot = FdroidPackageSnapshot(
            repoUrl = "https://f-droid.org/repo",
            packageName = "org.fdroid.fdroid",
            suggestedVersionCode = 102,
            appName = "F-Droid",
            versions = listOf(
                version(versionCode = 102, versionName = "1.2.0", signerSha256 = emptyList())
            )
        )
        val source = FdroidReleaseCheckSource(
            snapshotProvider = staticLookupSnapshotProvider(
                packageSnapshot = packageSnapshot,
                repositorySnapshot = repositorySnapshot(packageSnapshot)
            ),
            metadataWriter = { },
            ioDispatcher = Dispatchers.Unconfined,
            deviceSdkProvider = { 37 }
        )

        val result = source.evaluate(
            item = fdroidItem(
                FdroidTrackedAppConfig(trustPolicy = FdroidTrustPolicy.RequireOfficialSignerIndex)
            ),
            lookupConfig = GitHubLookupConfig(),
            localVersion = "1.0.0",
            localVersionCode = 100,
            forceRefresh = false
        )

        assertEquals(GitHubTrackedReleaseStatus.Failed, result.status)
        assertTrue(result.message.contains("signer index"))
    }

    @Test
    fun `evaluate fails when repository fingerprint policy has no configured fingerprint`() = runBlocking {
        val packageSnapshot = FdroidPackageSnapshot(
            repoUrl = "https://f-droid.org/repo",
            packageName = "org.fdroid.fdroid",
            suggestedVersionCode = 102,
            appName = "F-Droid",
            versions = listOf(
                version(versionCode = 102, versionName = "1.2.0")
            )
        )
        val source = FdroidReleaseCheckSource(
            snapshotProvider = staticLookupSnapshotProvider(
                packageSnapshot = packageSnapshot,
                repositorySnapshot = repositorySnapshot(packageSnapshot)
            ),
            metadataWriter = { },
            ioDispatcher = Dispatchers.Unconfined,
            deviceSdkProvider = { 37 }
        )

        val result = source.evaluate(
            item = fdroidItem(
                FdroidTrackedAppConfig(
                    trustPolicy = FdroidTrustPolicy.RequireRepoFingerprint,
                    repoFingerprint = ""
                )
            ),
            lookupConfig = GitHubLookupConfig(),
            localVersion = "1.0.0",
            localVersionCode = 100,
            forceRefresh = false
        )

        assertEquals(GitHubTrackedReleaseStatus.Failed, result.status)
        assertTrue(result.message.contains("repository fingerprint"))
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

    private fun staticLookupSnapshotProvider(
        packageSnapshot: FdroidPackageSnapshot,
        repositorySnapshot: FdroidRepositorySnapshot
    ): FdroidPackageSnapshotProvider {
        return object : FdroidPackageSnapshotProvider, FdroidPackageLookupSnapshotProvider {
            override suspend fun loadPackageSnapshot(
                item: GitHubTrackedApp,
                forceRefresh: Boolean
            ): Result<FdroidPackageSnapshot> {
                return Result.success(packageSnapshot)
            }

            override suspend fun loadPackageLookupSnapshot(
                item: GitHubTrackedApp,
                forceRefresh: Boolean
            ): Result<FdroidPackageLookupSnapshot> {
                return Result.success(
                    FdroidPackageLookupSnapshot(
                        packageSnapshot = packageSnapshot,
                        repositorySnapshot = repositorySnapshot
                    )
                )
            }
        }
    }

    private fun fdroidItem(
        fdroidConfig: FdroidTrackedAppConfig = FdroidTrackedAppConfig()
    ): GitHubTrackedApp {
        return GitHubTrackedApp(
            repoUrl = "https://f-droid.org/repo",
            owner = "f-droid.org",
            repo = "repo",
            packageName = "org.fdroid.fdroid",
            appLabel = "F-Droid",
            sourceMode = GitHubTrackedSourceMode.FdroidRepository,
            fdroidConfig = fdroidConfig
        )
    }

    private fun version(
        versionCode: Long,
        versionName: String,
        apkSha256: String = "sha256-$versionCode",
        signerSha256: List<String> = emptyList()
    ): FdroidVersionSnapshot {
        return FdroidVersionSnapshot(
            versionName = versionName,
            versionCode = versionCode,
            apkName = "org.fdroid.fdroid_$versionCode.apk",
            apkPath = "/repo/org.fdroid.fdroid_$versionCode.apk",
            apkSha256 = apkSha256,
            apkSizeBytes = versionCode,
            addedAtMillis = null,
            minSdk = 23,
            targetSdk = 35,
            nativeAbis = emptyList(),
            signerSha256 = signerSha256,
            releaseChannels = emptyList(),
            whatsNew = "notes $versionCode",
            antiFeatures = emptyList()
        )
    }

    private fun repositorySnapshot(packageSnapshot: FdroidPackageSnapshot): FdroidRepositorySnapshot {
        return FdroidRepositorySnapshot(
            repoUrl = "https://f-droid.org/repo",
            format = FdroidIndexFormat.V2,
            repoName = "F-Droid",
            repoDescription = "Official repository",
            timestampMillis = 1_777_392_000_000L,
            mirrors = emptyList(),
            packages = mapOf(packageSnapshot.packageName to packageSnapshot)
        )
    }
}
