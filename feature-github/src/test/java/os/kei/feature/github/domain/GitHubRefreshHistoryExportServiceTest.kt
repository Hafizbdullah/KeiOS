package os.kei.feature.github.domain

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import os.kei.core.json.optArray
import os.kei.core.json.optInt
import os.kei.core.json.optLong
import os.kei.core.json.optObject
import os.kei.core.json.optString
import os.kei.core.json.parseJsonObjectOrNull
import os.kei.feature.github.model.GitHubRefreshHistoryFailureSummary
import os.kei.feature.github.model.GitHubRefreshHistoryOutcome
import os.kei.feature.github.model.GitHubRefreshHistoryRecord

class GitHubRefreshHistoryExportServiceTest {
    @Test
    fun `export json includes metadata filters summary and records`() {
        val records =
            listOf(
                createRecord(
                    id = "newer",
                    sessionId = 2L,
                    finishedAtMillis = 2_000L,
                    updatableCount = 2,
                ),
                createRecord(
                    id = "older",
                    sessionId = 1L,
                    finishedAtMillis = 1_000L,
                    failedCount = 1,
                ),
            )

        val raw =
            GitHubRefreshHistoryExportService.buildExportJson(
                allRecords = records,
                query =
                    GitHubRefreshHistoryQuery(
                        outcome = GitHubRefreshHistoryOutcomeFilter.Updatable,
                        limit = 10,
                    ),
                exportedAtMillis = 3_000L,
            )
        val root = raw.parseJsonObjectOrNull()

        requireNotNull(root)
        assertEquals("keios.github.refresh-history", root.optString("format"))
        assertEquals(1, root.optInt("schemaVersion"))
        assertEquals("local_only", root.optString("syncScope"))
        assertEquals(3_000L, root.optLong("exportedAtMillis"))
        assertEquals("updatable", root.optObject("filters")?.optString("outcome"))
        assertEquals(2, root.optObject("summary")?.optInt("storedCount"))
        assertEquals(1, root.optObject("summary")?.optInt("matchedCount"))
        assertEquals("newer", root.optArray("records")?.optObject(0)?.optString("id"))
    }

    @Test
    fun `failed filter keeps failed outcome and partial failures`() {
        val records =
            listOf(
                createRecord(
                    id = "completed",
                    sessionId = 1L,
                    outcome = GitHubRefreshHistoryOutcome.Completed,
                ),
                createRecord(
                    id = "partial",
                    sessionId = 2L,
                    outcome = GitHubRefreshHistoryOutcome.Completed,
                    failedCount = 1,
                ),
                createRecord(
                    id = "failed",
                    sessionId = 3L,
                    outcome = GitHubRefreshHistoryOutcome.Failed,
                ),
            )

        val filtered =
            GitHubRefreshHistoryExportService.filterRecords(
                records = records,
                query = GitHubRefreshHistoryQuery(outcome = GitHubRefreshHistoryOutcomeFilter.Failed),
            )

        assertEquals(listOf("failed", "partial"), filtered.map { it.id })
    }

    @Test
    fun `summary calculates totals from matched records`() {
        val records =
            listOf(
                createRecord(
                    id = "one",
                    sessionId = 1L,
                    targetCount = 4,
                    completedCount = 4,
                    updatableCount = 1,
                    preReleaseUpdateCount = 2,
                    elapsedMs = 100L,
                ),
                createRecord(
                    id = "two",
                    sessionId = 2L,
                    targetCount = 6,
                    completedCount = 6,
                    failedCount = 2,
                    elapsedMs = 300L,
                ),
            )

        val summary =
            GitHubRefreshHistoryExportService.summarize(
                allRecords = records,
                records = records,
            )

        assertEquals(2, summary.storedCount)
        assertEquals(2, summary.matchedCount)
        assertEquals(10, summary.totalTargetCount)
        assertEquals(10, summary.totalCompletedCount)
        assertEquals(2, summary.totalFailedItemCount)
        assertEquals(1, summary.totalStableUpdateCount)
        assertEquals(2, summary.totalPreReleaseUpdateCount)
        assertEquals(200L, summary.averageElapsedMs)
        assertTrue(summary.latestFinishedAtMillis > 0L)
    }

    private fun createRecord(
        id: String,
        sessionId: Long,
        outcome: GitHubRefreshHistoryOutcome = GitHubRefreshHistoryOutcome.Completed,
        targetCount: Int = 3,
        completedCount: Int = 3,
        updatableCount: Int = 0,
        preReleaseUpdateCount: Int = 0,
        failedCount: Int = 0,
        elapsedMs: Long = 200L,
        finishedAtMillis: Long = 1_778_000_000_000L + sessionId,
    ): GitHubRefreshHistoryRecord =
        GitHubRefreshHistoryRecord(
            id = id,
            sessionId = sessionId,
            scope = GitHubRefreshScope.AllTracked,
            source = GitHubRefreshSource.Page,
            outcome = outcome,
            totalTrackedCount = targetCount,
            targetCount = targetCount,
            completedCount = completedCount,
            updatableCount = updatableCount,
            preReleaseUpdateCount = preReleaseUpdateCount,
            failedCount = failedCount,
            startedAtMillis = finishedAtMillis - elapsedMs,
            finishedAtMillis = finishedAtMillis,
            elapsedMs = elapsedMs,
            p50ItemMs = 10L,
            p95ItemMs = 20L,
            maxItemMs = 30L,
            failureSummaries =
                if (failedCount > 0) {
                    listOf(
                        GitHubRefreshHistoryFailureSummary(
                            trackId = "track-$sessionId",
                            owner = "owner",
                            repo = "repo",
                            packageName = "dev.example",
                            appLabel = "Example",
                            sourceMode = "github_repository",
                            message = "timeout",
                            elapsedMs = 30L,
                        ),
                    )
                } else {
                    emptyList()
                },
            note = "",
        )
}
