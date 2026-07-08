package os.kei.ui.page.main.github.history

import org.junit.Test
import kotlin.test.assertEquals
import os.kei.feature.github.domain.GitHubRefreshScope
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.github.model.GitHubRefreshHistoryFailureSummary
import os.kei.feature.github.model.GitHubRefreshHistoryOutcome
import os.kei.feature.github.model.GitHubRefreshHistoryRecord

class GitHubRefreshHistoryRetryTargetsTest {
    @Test
    fun `partial failure retries only failed targets`() {
        val record =
            createRecord(
                outcome = GitHubRefreshHistoryOutcome.Completed,
                targetTrackIds = listOf("ok-a", "failed-a", "failed-b"),
                failedCount = 2,
                failureTrackIds = listOf("failed-a", "failed-b", "failed-a"),
            )

        assertEquals(
            listOf("failed-a", "failed-b"),
            record.refreshHistoryRetryTargetIds(),
        )
    }

    @Test
    fun `cancelled record retries original batch targets`() {
        val record =
            createRecord(
                outcome = GitHubRefreshHistoryOutcome.Cancelled,
                targetTrackIds = listOf("target-a", "target-b", "target-a"),
                completedCount = 1,
            )

        assertEquals(
            listOf("target-a", "target-b"),
            record.refreshHistoryRetryTargetIds(),
        )
    }

    @Test
    fun `cancelled record prefers original batch targets when failure summaries exist`() {
        val record =
            createRecord(
                outcome = GitHubRefreshHistoryOutcome.Cancelled,
                targetTrackIds = listOf("target-a", "target-b", "target-c"),
                completedCount = 1,
                failedCount = 1,
                failureTrackIds = listOf("target-b"),
            )

        assertEquals(
            listOf("target-a", "target-b", "target-c"),
            record.refreshHistoryRetryTargetIds(),
        )
    }

    @Test
    fun `successful record has no retry targets`() {
        val record =
            createRecord(
                outcome = GitHubRefreshHistoryOutcome.Completed,
                targetTrackIds = listOf("target-a"),
                failedCount = 0,
            )

        assertEquals(emptyList(), record.refreshHistoryRetryTargetIds())
    }

    private fun createRecord(
        outcome: GitHubRefreshHistoryOutcome,
        targetTrackIds: List<String> = emptyList(),
        completedCount: Int = 3,
        failedCount: Int = 0,
        failureTrackIds: List<String> = emptyList(),
    ): GitHubRefreshHistoryRecord =
        GitHubRefreshHistoryRecord(
            id = "refresh-test",
            sessionId = 42L,
            scope = GitHubRefreshScope.RequestedTracked,
            source = GitHubRefreshSource.Page,
            outcome = outcome,
            totalTrackedCount = 4,
            targetCount = targetTrackIds.size.coerceAtLeast(failureTrackIds.size),
            targetTrackIds = targetTrackIds,
            completedCount = completedCount,
            updatableCount = 0,
            preReleaseUpdateCount = 0,
            failedCount = failedCount,
            startedAtMillis = 1_000L,
            finishedAtMillis = 2_000L,
            elapsedMs = 1_000L,
            failureSummaries =
                failureTrackIds.map { trackId ->
                    GitHubRefreshHistoryFailureSummary(
                        trackId = trackId,
                        owner = "owner",
                        repo = "repo",
                        packageName = "pkg",
                        appLabel = "App",
                        sourceMode = "github",
                        message = "timeout",
                    )
                },
        )
}
