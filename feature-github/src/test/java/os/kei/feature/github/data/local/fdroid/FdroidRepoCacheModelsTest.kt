package os.kei.feature.github.data.local.fdroid

import org.junit.Test
import os.kei.feature.github.data.remote.fdroid.FdroidPackageSnapshot
import os.kei.feature.github.data.remote.fdroid.FdroidRepositorySnapshot
import os.kei.feature.github.model.FdroidIndexFormat
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FdroidRepoCacheModelsTest {
    @Test
    fun `cache record reports freshness from fetched time and max age`() {
        val record = FdroidRepoCacheRecord(
            repoUrl = "https://f-droid.org/repo",
            fetchedAtMillis = 1_000L,
            etag = "etag",
            lastModified = "last-modified",
            snapshot = repositorySnapshot()
        )

        assertTrue(record.isFresh(nowMillis = 1_500L, maxAgeMillis = 1_000L))
        assertFalse(record.isFresh(nowMillis = 2_001L, maxAgeMillis = 1_000L))
        assertFalse(record.isFresh(nowMillis = 1_500L, maxAgeMillis = 0L))
    }

    @Test
    fun `cache key normalizes repository url`() {
        assertEquals(
            "https://f-droid.org/repo",
            FdroidRepoCacheKey.from(" https://f-droid.org/repo/ ").repoUrl
        )
    }

    private fun repositorySnapshot(): FdroidRepositorySnapshot {
        return FdroidRepositorySnapshot(
            repoUrl = "https://f-droid.org/repo",
            format = FdroidIndexFormat.V2,
            repoName = "F-Droid",
            repoDescription = "",
            timestampMillis = null,
            mirrors = emptyList(),
            packages = mapOf(
                "org.fdroid.fdroid" to FdroidPackageSnapshot(
                    repoUrl = "https://f-droid.org/repo",
                    packageName = "org.fdroid.fdroid",
                    suggestedVersionCode = null,
                    versions = emptyList()
                )
            )
        )
    }
}
