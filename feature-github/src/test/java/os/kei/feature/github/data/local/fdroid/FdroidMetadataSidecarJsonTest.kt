package os.kei.feature.github.data.local.fdroid

import org.junit.Test
import os.kei.feature.github.data.remote.fdroid.FdroidAntiFeatureSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidPackageSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidRepositorySnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidVersionSnapshot
import os.kei.feature.github.model.FdroidIndexFormat
import os.kei.feature.github.model.FdroidTrustPolicy
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FdroidMetadataSidecarJsonTest {
    @Test
    fun `metadata sidecar round trip preserves repo package version trust and anti-features`() {
        val sidecar = buildFdroidMetadataSidecar(
            trackId = "fdroid_repository|https://f-droid.org/repo|org.fdroid.fdroid",
            sourceConfigSignature = "fdroid_repository-v1|fixture",
            fetchedAtMillis = FETCHED_AT,
            repositorySnapshot = repositorySnapshot(),
            packageSnapshot = packageSnapshot(),
            selectedVersion = version()
        )

        val restored = parseFdroidMetadataSidecar(sidecar.toCacheJson())
            ?: error("sidecar should restore")

        assertEquals(sidecar.trackId, restored.trackId)
        assertEquals("F-Droid", restored.repo.repoName)
        assertEquals("org.fdroid.fdroid", restored.packageInfo.packageName)
        assertEquals("1.2.0", restored.selectedVersion?.versionName)
        assertEquals("sha256-102", restored.trust.apkSha256)
        assertEquals(listOf("signer-102"), restored.trust.signerSha256)
        assertEquals(FdroidTrustPolicy.TrackOnlyWarn, restored.trust.trustPolicy)
        assertEquals(listOf("Tracking", "KnownVuln"), restored.antiFeatures.map { it.id })
    }

    @Test
    fun `metadata sidecar freshness follows signature and ttl`() {
        val sidecar = buildFdroidMetadataSidecar(
            trackId = "track",
            sourceConfigSignature = "sig",
            fetchedAtMillis = FETCHED_AT,
            repositorySnapshot = repositorySnapshot(),
            packageSnapshot = packageSnapshot(),
            selectedVersion = version()
        )

        assertTrue(
            sidecar.isFreshFor(
                activeSourceConfigSignature = "sig",
                nowMillis = FETCHED_AT + 1_000L,
                ttlMillis = 10_000L
            )
        )
        assertFalse(
            sidecar.isFreshFor(
                activeSourceConfigSignature = "other",
                nowMillis = FETCHED_AT + 1_000L,
                ttlMillis = 10_000L
            )
        )
        assertFalse(
            sidecar.isFreshFor(
                activeSourceConfigSignature = "sig",
                nowMillis = FETCHED_AT + 20_000L,
                ttlMillis = 10_000L
            )
        )
    }

    private fun repositorySnapshot(): FdroidRepositorySnapshot {
        return FdroidRepositorySnapshot(
            repoUrl = "https://f-droid.org/repo",
            format = FdroidIndexFormat.V2,
            repoName = "F-Droid",
            repoDescription = "Official repo",
            timestampMillis = FETCHED_AT,
            mirrors = listOf("https://mirror.example/fdroid/repo"),
            packages = mapOf("org.fdroid.fdroid" to packageSnapshot())
        )
    }

    private fun packageSnapshot(): FdroidPackageSnapshot {
        return FdroidPackageSnapshot(
            repoUrl = "https://f-droid.org/repo",
            packageName = "org.fdroid.fdroid",
            suggestedVersionCode = 102,
            versions = listOf(version()),
            appName = "F-Droid",
            summary = "App store",
            description = "Repository client",
            license = "GPL-3.0-or-later",
            sourceCodeUrl = "https://gitlab.com/fdroid/fdroidclient",
            webSiteUrl = "https://f-droid.org",
            issueTrackerUrl = "https://gitlab.com/fdroid/fdroidclient/-/issues",
            changelogUrl = "https://f-droid.org/packages/org.fdroid.fdroid/changelog",
            categories = listOf("System"),
            antiFeatures = listOf(FdroidAntiFeatureSnapshot("Tracking"))
        )
    }

    private fun version(): FdroidVersionSnapshot {
        return FdroidVersionSnapshot(
            versionName = "1.2.0",
            versionCode = 102,
            apkName = "org.fdroid.fdroid_102.apk",
            apkPath = "/repo/org.fdroid.fdroid_102.apk",
            apkSha256 = "sha256-102",
            apkSizeBytes = 102_000L,
            addedAtMillis = FETCHED_AT,
            minSdk = 23,
            targetSdk = 35,
            nativeAbis = listOf("arm64-v8a"),
            signerSha256 = listOf("signer-102"),
            releaseChannels = emptyList(),
            whatsNew = "Latest fixes",
            antiFeatures = listOf(FdroidAntiFeatureSnapshot("KnownVuln"))
        )
    }

    private companion object {
        const val FETCHED_AT = 1_780_000_000_000L
    }
}
