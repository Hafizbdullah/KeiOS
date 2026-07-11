package os.kei.feature.github.data.local

import os.kei.feature.github.model.GitHubCheckCacheEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GitHubTrackStoreDueChecksTest {
    @Test
    fun `due cache keeps content and fills missing tracks`() {
        val current =
            mapOf(
                "cached" to
                    GitHubCheckCacheEntry(
                        latestTag = "v2.0.0",
                        checkedAtMillis = 123_456L,
                    ),
                "removed" to GitHubCheckCacheEntry(latestTag = "old"),
            )

        val due = buildDueCheckCache(listOf("cached", "missing"), current)

        assertEquals(setOf("cached", "missing"), due.keys)
        assertEquals("v2.0.0", due.getValue("cached").latestTag)
        assertEquals(1L, due.getValue("cached").checkedAtMillis)
        assertEquals(1L, due.getValue("missing").checkedAtMillis)
        assertFalse("removed" in due)
    }
}
