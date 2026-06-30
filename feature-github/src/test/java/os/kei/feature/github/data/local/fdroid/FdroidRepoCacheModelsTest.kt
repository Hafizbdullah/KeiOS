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

    @Test
    fun `cache index entry parses legacy rows without access time`() {
        val entry = parseIndexEntry("abc123|https://f-droid.org/repo")

        assertEquals("abc123", entry?.id)
        assertEquals("https://f-droid.org/repo", entry?.repoUrl)
        assertEquals(0L, entry?.accessedAtMillis)
    }

    @Test
    fun `cache index entry preserves access time in serialized rows`() {
        val original = FdroidRepoIndexCacheEntry(
            id = "abc123",
            repoUrl = "https://apt.izzysoft.de/fdroid/repo",
            accessedAtMillis = 2_026_070_100L
        )

        val parsed = parseIndexEntry(original.serialize())

        assertEquals(original, parsed)
    }

    @Test
    fun `cache index retention keeps most recently accessed rows`() {
        val entries = setOf(
            indexEntry(id = "old", accessedAtMillis = 1L),
            indexEntry(id = "middle", accessedAtMillis = 2L),
            indexEntry(id = "new", accessedAtMillis = 3L),
        )

        val retained = retainMostRecentlyAccessedIndexEntries(entries, maxRecords = 2)

        assertEquals(setOf("middle", "new"), retained.map { entry -> entry.id }.toSet())
    }

    @Test
    fun `cache index retention accepts zero record limit`() {
        val entries = setOf(indexEntry(id = "old", accessedAtMillis = 1L))

        val retained = retainMostRecentlyAccessedIndexEntries(entries, maxRecords = 0)

        assertTrue(retained.isEmpty())
    }

    private fun indexEntry(
        id: String,
        accessedAtMillis: Long,
    ): FdroidRepoIndexCacheEntry =
        FdroidRepoIndexCacheEntry(
            id = id,
            repoUrl = "https://f-droid.org/repo",
            accessedAtMillis = accessedAtMillis,
        )

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
