package os.kei.feature.github.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import os.kei.feature.github.model.GitHubCheckCacheEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubBackgroundRefreshCheckpointWriterTest {
    @Test
    fun `flushes when item threshold is reached`() = runTest {
        val persisted = mutableListOf<Map<String, GitHubCheckCacheEntry>>()
        val writer = GitHubBackgroundRefreshCheckpointWriter(
            persist = persisted::add,
            itemThreshold = 2,
            intervalMs = Long.MAX_VALUE,
        )

        writer.append("one", entry(1L))
        writer.append("two", entry(2L))

        assertEquals(listOf(setOf("one", "two")), persisted.map { it.keys })
    }

    @Test
    fun `final flush persists remainder and latest duplicate`() = runTest {
        val persisted = mutableListOf<Map<String, GitHubCheckCacheEntry>>()
        val writer = GitHubBackgroundRefreshCheckpointWriter(
            persist = persisted::add,
            itemThreshold = 10,
            intervalMs = Long.MAX_VALUE,
        )

        writer.append("one", entry(1L))
        writer.append("one", entry(3L))
        writer.flush()

        assertEquals(1, persisted.size)
        assertEquals(3L, persisted.single().getValue("one").checkedAtMillis)
    }

    @Test
    fun `flushes after interval`() = runTest {
        var nowMs = 100L
        val persisted = mutableListOf<Map<String, GitHubCheckCacheEntry>>()
        val writer = GitHubBackgroundRefreshCheckpointWriter(
            persist = persisted::add,
            itemThreshold = 10,
            intervalMs = 2_000L,
            nowMs = { nowMs },
        )

        writer.append("one", entry(1L))
        nowMs = 2_100L
        writer.append("two", entry(2L))

        assertEquals(1, persisted.size)
        assertEquals(setOf("one", "two"), persisted.single().keys)
    }

    @Test
    fun `failed persistence keeps pending entries for retry`() = runTest {
        var attempts = 0
        val persisted = mutableListOf<Map<String, GitHubCheckCacheEntry>>()
        val writer = GitHubBackgroundRefreshCheckpointWriter(
            persist = { entries ->
                attempts += 1
                if (attempts == 1) error("temporary")
                persisted += entries
            },
            itemThreshold = 1,
        )

        runCatching { writer.append("one", entry(1L)) }
        writer.flush()

        assertEquals(2, attempts)
        assertEquals(setOf("one"), persisted.single().keys)
    }

    @Test
    fun `concurrent appends persist every track`() = runTest {
        val persisted = mutableListOf<Map<String, GitHubCheckCacheEntry>>()
        val writer = GitHubBackgroundRefreshCheckpointWriter(
            persist = persisted::add,
            itemThreshold = 4,
            intervalMs = Long.MAX_VALUE,
        )

        (0 until 20).map { index ->
            async { writer.append("track-$index", entry(index.toLong())) }
        }.awaitAll()
        writer.flush()

        val merged = persisted.fold(emptyMap<String, GitHubCheckCacheEntry>()) { acc, entries -> acc + entries }
        assertEquals(20, merged.size)
        assertTrue((0 until 20).all { "track-$it" in merged })
    }

    private fun entry(checkedAtMillis: Long) =
        GitHubCheckCacheEntry(checkedAtMillis = checkedAtMillis)
}
