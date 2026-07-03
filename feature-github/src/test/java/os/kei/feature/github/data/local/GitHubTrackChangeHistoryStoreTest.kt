package os.kei.feature.github.data.local

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Test
import os.kei.feature.github.model.GitHubTrackChangeField
import os.kei.feature.github.model.GitHubTrackChangeHistoryAction
import os.kei.feature.github.model.GitHubTrackChangeHistoryRecord
import os.kei.feature.github.model.GitHubTrackChangeHistorySource
import os.kei.feature.github.model.GitHubTrackedSourceMode

class GitHubTrackChangeHistoryStoreTest {
    @Test
    fun `track change history record round trips through json`() {
        val record = createRecord()

        val decoded = GitHubTrackChangeHistoryStore.decodeRecord(
            GitHubTrackChangeHistoryStore.encodeRecord(record).toString(),
        )

        assertEquals(record, decoded)
    }

    @Test
    fun `track change history id includes action source and time`() {
        val first = createRecord(changedAtMillis = 1_000L)
        val same = first.copy(note = "local detail")
        val updated = first.copy(action = GitHubTrackChangeHistoryAction.Deleted)
        val later = first.copy(changedAtMillis = 2_000L)

        val firstId =
            with(GitHubTrackChangeHistoryStore) {
                recordId(first.normalizedForStorage())
            }
        val sameId =
            with(GitHubTrackChangeHistoryStore) {
                recordId(same.normalizedForStorage())
            }
        val updatedId =
            with(GitHubTrackChangeHistoryStore) {
                recordId(updated.normalizedForStorage())
            }
        val laterId =
            with(GitHubTrackChangeHistoryStore) {
                recordId(later.normalizedForStorage())
            }

        assertEquals(firstId, sameId)
        assertNotEquals(firstId, updatedId)
        assertNotEquals(firstId, laterId)
    }

    @Test
    fun `track change history prune predicate uses changed time boundary`() {
        val oldRecord = createRecord(changedAtMillis = 1_000L)
        val boundaryRecord = createRecord(changedAtMillis = 1_500L)

        assertEquals(
            true,
            GitHubTrackChangeHistoryStore.shouldPruneBefore(oldRecord, cutoffMillis = 1_500L),
        )
        assertEquals(
            false,
            GitHubTrackChangeHistoryStore.shouldPruneBefore(boundaryRecord, cutoffMillis = 1_500L),
        )
        assertEquals(
            false,
            GitHubTrackChangeHistoryStore.shouldPruneBefore(oldRecord, cutoffMillis = 0L),
        )
    }

    private fun createRecord(
        changedAtMillis: Long = 1_778_000_300_000L,
    ): GitHubTrackChangeHistoryRecord =
        GitHubTrackChangeHistoryRecord(
            id = "change-42",
            trackId = "owner/repo|pkg.app",
            previousTrackId = "owner/old|pkg.app",
            action = GitHubTrackChangeHistoryAction.Updated,
            source = GitHubTrackChangeHistorySource.Page,
            changedAtMillis = changedAtMillis,
            owner = "owner",
            repo = "repo",
            repoUrl = "https://github.com/owner/repo",
            packageName = "pkg.app",
            appLabel = "Demo",
            sourceMode = GitHubTrackedSourceMode.GitHubRepository,
            changedFields =
                listOf(
                    GitHubTrackChangeField.Repository,
                    GitHubTrackChangeField.PackageName,
                ),
            note = "edited from sheet",
        )
}
