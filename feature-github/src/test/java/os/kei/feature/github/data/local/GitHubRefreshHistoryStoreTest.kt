package os.kei.feature.github.data.local

import org.junit.Test
import os.kei.feature.github.domain.GitHubRefreshScope
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.github.model.GitHubRefreshHistoryFailureSummary
import os.kei.feature.github.model.GitHubRefreshHistoryOutcome
import os.kei.feature.github.model.GitHubRefreshHistoryRecord
import os.kei.feature.github.model.GitHubRefreshHistorySlowItem
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GitHubRefreshHistoryStoreTest {
    @Test
    fun `refresh history record round trips through json`() {
        val record = createRecord()

        val decoded = GitHubRefreshHistoryStore.decodeRecord(
            GitHubRefreshHistoryStore.encodeRecord(record).toString()
        )

        assertEquals(record, decoded)
    }

    @Test
    fun `refresh history id is stable for the same session`() {
        val first = createRecord(sessionId = 42L, finishedAtMillis = 2_000L)
        val updated =
            first.copy(
                failedCount = 2,
                finishedAtMillis = 2_100L,
                note = "timeout",
            )
        val anotherSession = first.copy(sessionId = 43L)

        val firstId =
            with(GitHubRefreshHistoryStore) {
                recordId(first.normalizedForStorage())
            }
        val updatedId =
            with(GitHubRefreshHistoryStore) {
                recordId(updated.normalizedForStorage())
            }
        val anotherId =
            with(GitHubRefreshHistoryStore) {
                recordId(anotherSession.normalizedForStorage())
            }

        assertEquals(firstId, updatedId)
        assertNotEquals(firstId, anotherId)
    }

    @Test
    fun `refresh history collapses legacy duplicate records for one session`() {
        val first =
            createRecord(sessionId = 42L, finishedAtMillis = 2_000L)
                .copy(id = "legacy-first")
        val latest =
            first.copy(
                id = "legacy-latest",
                finishedAtMillis = 2_100L,
                note = "stopped: timeout",
            )
        val anotherSession =
            createRecord(sessionId = 43L, finishedAtMillis = 2_050L)
                .copy(id = "legacy-other")

        val records =
            GitHubRefreshHistoryStore.collapseDuplicateSessions(
                listOf(first, latest, anotherSession),
            )

        assertEquals(2, records.size)
        assertEquals(latest, records.single { it.sessionId == 42L })
        assertEquals(anotherSession, records.single { it.sessionId == 43L })
    }

    @Test
    fun `refresh history compaction keeps the newest stored entry for each session`() {
        val first =
            createRecord(sessionId = 42L, finishedAtMillis = 2_000L)
                .copy(id = "legacy-first")
        val latest =
            first.copy(
                id = "legacy-latest",
                finishedAtMillis = 2_100L,
                note = "stopped: timeout",
            )
        val anotherSession =
            createRecord(sessionId = 43L, finishedAtMillis = 2_050L)
                .copy(id = "legacy-other")

        val retainedIds =
            GitHubRefreshHistoryStore.collapsedEntryIds(
                listOf(
                    "legacy-first" to first,
                    "legacy-latest" to latest,
                    "legacy-other" to anotherSession,
                ),
            )

        assertEquals(setOf("legacy-latest", "legacy-other"), retainedIds)
    }

    @Test
    fun `refresh history prune predicate uses finished time boundary`() {
        val oldRecord = createRecord(finishedAtMillis = 1_000L)
        val boundaryRecord = createRecord(finishedAtMillis = 1_500L)

        assertEquals(
            true,
            GitHubRefreshHistoryStore.shouldPruneBefore(oldRecord, cutoffMillis = 1_500L),
        )
        assertEquals(
            false,
            GitHubRefreshHistoryStore.shouldPruneBefore(boundaryRecord, cutoffMillis = 1_500L),
        )
        assertEquals(
            false,
            GitHubRefreshHistoryStore.shouldPruneBefore(oldRecord, cutoffMillis = 0L),
        )
    }

    private fun createRecord(
        sessionId: Long = 42L,
        finishedAtMillis: Long = 1_778_000_300_000L,
    ): GitHubRefreshHistoryRecord =
        GitHubRefreshHistoryRecord(
            id = "refresh-42",
            sessionId = sessionId,
            scope = GitHubRefreshScope.AllTracked,
            source = GitHubRefreshSource.Page,
            outcome = GitHubRefreshHistoryOutcome.Completed,
            totalTrackedCount = 77,
            targetCount = 77,
            targetTrackIds = listOf("track-a", "track-b", "track-c"),
            completedCount = 77,
            updatableCount = 2,
            preReleaseUpdateCount = 1,
            failedCount = 1,
            startedAtMillis = 1_778_000_000_000L,
            finishedAtMillis = finishedAtMillis,
            elapsedMs = 300_000L,
            p50ItemMs = 500L,
            p95ItemMs = 2_000L,
            maxItemMs = 5_000L,
            maxConcurrency = 10,
            directApkConcurrency = 2,
            fdroidConcurrency = 2,
            repositoryItemCount = 55,
            directApkItemCount = 12,
            fdroidItemCount = 8,
            otherItemCount = 2,
            schedulerJobId = 42_101,
            schedulerEnqueuedAtMillis = 1_777_999_990_000L,
            schedulerStartedAtMillis = 1_778_000_000_000L,
            schedulerStopReason = "timeout",
            schedulerRescheduled = true,
            slowItems =
                listOf(
                    GitHubRefreshHistorySlowItem(
                        trackId = "fdroid|https://f-droid.org/repo|org.fdroid.fdroid",
                        owner = "f-droid",
                        repo = "fdroid",
                        packageName = "org.fdroid.fdroid",
                        appLabel = "F-Droid",
                        sourceMode = "fdroid_repository",
                        elapsedMs = 12_000L,
                        status = "UpToDate",
                        message = "remote index parsed",
                        strategyId = "fdroid",
                        localVersionElapsedMs = 250L,
                        snapshotElapsedMs = 9_000L,
                        profileElapsedMs = 0L,
                        profileFromCache = true,
                        preciseApkElapsedMs = 500L,
                        preciseApkRequested = true,
                        comparisonElapsedMs = 125L,
                        unclassifiedElapsedMs = 2_500L,
                        fallbackStrategyId = "fallback",
                    ),
                ),
            failureSummaries =
                listOf(
                    GitHubRefreshHistoryFailureSummary(
                        trackId = "open-ani/animeko|me.him188.ani",
                        owner = "open-ani",
                        repo = "animeko",
                        packageName = "me.him188.ani",
                        appLabel = "Animeko",
                        sourceMode = "github",
                        message = "rate limited",
                        elapsedMs = 5_000L,
                    ),
                ),
            note = "manual refresh",
        )
}
