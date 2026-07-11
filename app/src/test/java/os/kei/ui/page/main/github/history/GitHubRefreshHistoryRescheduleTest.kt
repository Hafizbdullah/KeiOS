package os.kei.ui.page.main.github.history

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import os.kei.feature.github.domain.GitHubRefreshScope
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.github.model.GitHubRefreshHistoryOutcome
import os.kei.feature.github.model.GitHubRefreshHistoryRecord

class GitHubRefreshHistoryRescheduleTest {
    @Test
    fun `scheduler reschedule is presented separately from user cancellation`() {
        assertTrue(record(schedulerRescheduled = true).isBackgroundSchedulerReschedule())
    }

    @Test
    fun `legacy stopped note is recognized as scheduler reschedule`() {
        assertTrue(record(note = "github tick stopped").isBackgroundSchedulerReschedule())
        assertTrue(record(note = "github tick stopped: quota").isBackgroundSchedulerReschedule())
    }

    @Test
    fun `foreground cancellation remains interrupted`() {
        assertFalse(
            record(
                source = GitHubRefreshSource.Page,
                schedulerRescheduled = true,
            ).isBackgroundSchedulerReschedule(),
        )
    }

    private fun record(
        source: GitHubRefreshSource = GitHubRefreshSource.BackgroundTick,
        schedulerRescheduled: Boolean = false,
        note: String = "",
    ): GitHubRefreshHistoryRecord =
        GitHubRefreshHistoryRecord(
            id = "test",
            sessionId = 1L,
            scope = GitHubRefreshScope.DueTracked,
            source = source,
            outcome = GitHubRefreshHistoryOutcome.Cancelled,
            totalTrackedCount = 77,
            targetCount = 73,
            completedCount = 27,
            updatableCount = 13,
            preReleaseUpdateCount = 3,
            failedCount = 0,
            startedAtMillis = 1_000L,
            finishedAtMillis = 9_000L,
            elapsedMs = 8_000L,
            schedulerRescheduled = schedulerRescheduled,
            note = note,
        )
}
