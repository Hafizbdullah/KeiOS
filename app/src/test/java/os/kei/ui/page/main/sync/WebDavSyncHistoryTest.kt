package os.kei.ui.page.main.sync

import org.junit.Test
import kotlin.test.assertEquals

class WebDavSyncHistoryTest {
    @Test
    fun `append history keeps newest entries first and caps stored records`() {
        val existing =
            (0 until WebDavSyncHistoryMaxEntries).map { index ->
                webDavSyncHistoryEntry(id = "old-$index", finishedAtMs = index.toLong())
            }

        val updated =
            appendWebDavSyncHistory(
                current = existing,
                entry = webDavSyncHistoryEntry(id = "new", finishedAtMs = 10_000L),
            )

        assertEquals(WebDavSyncHistoryMaxEntries, updated.size)
        assertEquals("new", updated.first().id)
        assertEquals("old-1", updated.last().id)
    }

    @Test
    fun `append history replaces matching id instead of duplicating records`() {
        val existing =
            listOf(
                webDavSyncHistoryEntry(id = "same", reason = "launch", finishedAtMs = 1_000L),
                webDavSyncHistoryEntry(id = "other", reason = "manual", finishedAtMs = 900L),
            )

        val updated =
            appendWebDavSyncHistory(
                current = existing,
                entry = webDavSyncHistoryEntry(id = "same", reason = "background", finishedAtMs = 2_000L),
            )

        assertEquals(listOf("same", "other"), updated.map { it.id })
        assertEquals("background", updated.first().reason)
        assertEquals(2, updated.size)
    }
}

private fun webDavSyncHistoryEntry(
    id: String,
    reason: String = "manual",
    finishedAtMs: Long = 1_000L,
): WebDavSyncHistoryEntry =
    WebDavSyncHistoryEntry(
        id = id,
        source = WebDavSyncHistorySource.Manual,
        kind = WebDavSyncHistoryKind.Sync,
        reason = reason,
        status = WebDavAutoSyncStatus.Success,
        startedAtMs = finishedAtMs - 100L,
        finishedAtMs = finishedAtMs,
        targetCount = 1,
        succeededCount = 1,
        failedCount = 0,
        skippedCount = 0,
        items =
            listOf(
                WebDavSyncHistoryItem(
                    item = WebDavSyncItem.GitHubTracked,
                    status = WebDavItemStatus.UpToDate,
                ),
            ),
    )
