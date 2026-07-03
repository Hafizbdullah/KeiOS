package os.kei.feature.github.domain

import kotlin.test.assertEquals
import org.junit.Test

class GitHubHistoryUnreadServiceTest {
    @Test
    fun `unread counts compare records against independent watermarks`() {
        val eventTimes =
            GitHubHistoryUnreadEventTimes(
                refreshTimes = listOf(1_000L, 2_000L, 3_000L),
                actionTimes = listOf(1_500L, 2_500L),
                trackingTimes = listOf(900L, 1_100L),
                appTimes = listOf(4_000L),
            )
        val watermarks =
            GitHubHistoryUnreadWatermarks(
                refreshReadAtMillis = 2_000L,
                actionsReadAtMillis = 0L,
                trackingReadAtMillis = 1_000L,
                appsReadAtMillis = 4_000L,
            )

        val counts =
            GitHubHistoryUnreadCounter.buildCounts(
                eventTimes = eventTimes,
                watermarks = watermarks,
            )

        assertEquals(1, counts.refreshCount)
        assertEquals(2, counts.actionsCount)
        assertEquals(1, counts.trackingCount)
        assertEquals(0, counts.appsCount)
        assertEquals(4, counts.totalCount)
    }

    @Test
    fun `marking one bucket read preserves unread records from other buckets`() {
        val eventTimes =
            GitHubHistoryUnreadEventTimes(
                refreshTimes = listOf(1_000L, 2_000L),
                actionTimes = listOf(1_000L, 2_000L, 3_000L),
                trackingTimes = listOf(1_000L),
                appTimes = listOf(1_000L),
            )
        val previousWatermarks =
            GitHubHistoryUnreadWatermarks(
                refreshReadAtMillis = 0L,
                actionsReadAtMillis = 1_000L,
                trackingReadAtMillis = 0L,
                appsReadAtMillis = 0L,
            )
        val nextWatermarks =
            previousWatermarks.copy(
                actionsReadAtMillis = eventTimes.latestAt(GitHubHistoryUnreadBucket.Actions),
            )

        val counts =
            GitHubHistoryUnreadCounter.buildCounts(
                eventTimes = eventTimes,
                watermarks = nextWatermarks,
            )

        assertEquals(2, counts.refreshCount)
        assertEquals(0, counts.actionsCount)
        assertEquals(1, counts.trackingCount)
        assertEquals(1, counts.appsCount)
        assertEquals(4, counts.totalCount)
    }
}
