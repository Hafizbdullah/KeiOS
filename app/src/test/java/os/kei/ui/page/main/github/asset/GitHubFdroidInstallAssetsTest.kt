package os.kei.ui.page.main.github.asset

import os.kei.feature.github.data.local.fdroid.FdroidMetadataSidecar
import os.kei.feature.github.data.local.fdroid.FdroidPackageMetadataSummary
import os.kei.feature.github.data.local.fdroid.FdroidRepoMetadataSummary
import os.kei.feature.github.data.local.fdroid.FdroidTrustSummary
import os.kei.feature.github.data.local.fdroid.FdroidVersionMetadataSummary
import os.kei.feature.github.model.FdroidIndexFormat
import os.kei.feature.github.model.FdroidTrustPolicy
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedSourceMode
import os.kei.feature.github.model.fdroidRepositoryCheckSourceSignature
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GitHubFdroidInstallAssetsTest {
    @Test
    fun `resolve fdroid apk path against repository url`() {
        assertEquals(
            "https://f-droid.org/repo/org.fdroid.fdroid_102.apk",
            resolveFdroidApkDownloadUrl(
                repoUrl = "https://f-droid.org/repo",
                apkPath = "/repo/org.fdroid.fdroid_102.apk",
            ),
        )
        assertEquals(
            "https://apt.izzysoft.de/fdroid/repo/demo.apk",
            resolveFdroidApkDownloadUrl(
                repoUrl = "https://apt.izzysoft.de/fdroid/repo",
                apkPath = "demo.apk",
            ),
        )
        assertEquals(
            "https://cdn.example/demo.apk",
            resolveFdroidApkDownloadUrl(
                repoUrl = "https://repo.example/fdroid/repo",
                apkPath = "https://cdn.example/demo.apk",
            ),
        )
    }

    @Test
    fun `fdroid sidecar maps selected apk to release asset bundle`() {
        val item = fdroidItem()
        val sidecar = sidecar(item)

        val data = item.fdroidAssetPanelData(sidecar)

        assertNotNull(data)
        assertEquals("1.2.3", data.targetRawTag)
        assertEquals(GITHUB_FDROID_ASSET_FETCH_SOURCE, data.bundle.fetchSource)
        assertEquals(item.fdroidRepositoryCheckSourceSignature(), data.bundle.sourceConfigSignature)
        assertEquals("F-Droid", data.bundle.releaseName)
        assertEquals("https://f-droid.org/packages/org.fdroid.fdroid/", data.bundle.htmlUrl)
        val asset = data.bundle.assets.single()
        assertEquals("org.fdroid.fdroid_102.apk", asset.name)
        assertEquals("https://f-droid.org/repo/org.fdroid.fdroid_102.apk", asset.downloadUrl)
        assertEquals("sha256:abc123", asset.digest)
        assertEquals(listOf("signer-1"), asset.signerSha256)
    }

    @Test
    fun `stale fdroid sidecar is ignored`() {
        val item = fdroidItem()
        val sidecar = sidecar(item).copy(sourceConfigSignature = "old")

        assertNull(item.fdroidAssetPanelData(sidecar))
    }

    private fun fdroidItem(): GitHubTrackedApp {
        return GitHubTrackedApp(
            repoUrl = "https://f-droid.org/packages/org.fdroid.fdroid/",
            owner = "f-droid.org",
            repo = "repo",
            packageName = "org.fdroid.fdroid",
            appLabel = "F-Droid",
            sourceMode = GitHubTrackedSourceMode.FdroidRepository,
        )
    }

    private fun sidecar(item: GitHubTrackedApp): FdroidMetadataSidecar {
        return FdroidMetadataSidecar(
            trackId = item.id,
            sourceConfigSignature = item.fdroidRepositoryCheckSourceSignature(),
            fetchedAtMillis = 1_777_000_000_000L,
            repo =
                FdroidRepoMetadataSummary(
                    repoUrl = "https://f-droid.org/repo",
                    repoName = "F-Droid",
                    repoDescription = "",
                    format = FdroidIndexFormat.PackageApi,
                    timestampMillis = null,
                    packageCount = 0,
                    mirrors = emptyList(),
                ),
            packageInfo =
                FdroidPackageMetadataSummary(
                    packageName = "org.fdroid.fdroid",
                    appName = "F-Droid",
                    summary = "",
                    description = "",
                    license = "GPL-3.0",
                    sourceCodeUrl = "",
                    webSiteUrl = "",
                    issueTrackerUrl = "",
                    changelogUrl = "",
                    categories = emptyList(),
                ),
            selectedVersion =
                FdroidVersionMetadataSummary(
                    versionName = "1.2.3",
                    versionCode = 102L,
                    apkName = "org.fdroid.fdroid_102.apk",
                    apkPath = "/repo/org.fdroid.fdroid_102.apk",
                    apkSha256 = "abc123",
                    apkSizeBytes = 12_345L,
                    addedAtMillis = 1_776_000_000_000L,
                    minSdk = 23,
                    targetSdk = 35,
                    nativeAbis = emptyList(),
                    signerSha256 = listOf("signer-1"),
                    releaseChannels = emptyList(),
                    whatsNew = "Release notes",
                    antiFeatures = emptyList(),
                ),
            candidateVersions = emptyList(),
            trust =
                FdroidTrustSummary(
                    trustPolicy = FdroidTrustPolicy.TrackOnlyWarn,
                    repoFingerprint = "",
                    apkSha256 = "abc123",
                    signerSha256 = listOf("signer-1"),
                    hashAvailable = true,
                    signerAvailable = true,
                ),
            antiFeatures = emptyList(),
        )
    }
}
