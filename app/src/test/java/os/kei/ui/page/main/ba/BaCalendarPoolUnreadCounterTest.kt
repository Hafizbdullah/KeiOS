package os.kei.ui.page.main.ba

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import os.kei.ui.page.main.ba.support.BaCalendarEntry

class BaCalendarPoolUnreadCounterTest {
    @Test
    fun `unread counts are scoped by server and kind`() {
        val events =
            listOf(
                unreadEvent(serverIndex = 0, kind = BaCalendarPoolUnreadKind.Calendar, changedAtMillis = 1_000L, changeCount = 2),
                unreadEvent(serverIndex = 0, kind = BaCalendarPoolUnreadKind.Pool, changedAtMillis = 1_200L, changeCount = 1),
                unreadEvent(serverIndex = 1, kind = BaCalendarPoolUnreadKind.Calendar, changedAtMillis = 1_400L, changeCount = 4),
                unreadEvent(serverIndex = 0, kind = BaCalendarPoolUnreadKind.Calendar, changedAtMillis = 2_000L, changeCount = 1),
            )
        val watermarks =
            BaCalendarPoolUnreadWatermarks(
                calendarReadAtByServer = mapOf(0 to 1_000L),
                poolReadAtByServer = emptyMap(),
            )

        val counts =
            BaCalendarPoolUnreadCounter.buildCounts(
                events = events,
                watermarks = watermarks,
                serverIndex = 0,
            )

        assertEquals(1, counts.calendarCount)
        assertEquals(1, counts.poolCount)
        assertEquals(2, counts.totalCount)
    }

    @Test
    fun `marking one bucket read preserves other buckets`() {
        val events =
            listOf(
                unreadEvent(serverIndex = 2, kind = BaCalendarPoolUnreadKind.Calendar, changedAtMillis = 1_000L, changeCount = 1),
                unreadEvent(serverIndex = 2, kind = BaCalendarPoolUnreadKind.Pool, changedAtMillis = 2_000L, changeCount = 3),
            )
        val nextWatermarks =
            BaCalendarPoolUnreadWatermarks()
                .markRead(
                    kind = BaCalendarPoolUnreadKind.Calendar,
                    serverIndex = 2,
                    latestSeenAtMillis =
                        BaCalendarPoolUnreadCounter.latestSeenAt(
                            events = events,
                            serverIndex = 2,
                            kind = BaCalendarPoolUnreadKind.Calendar,
                        ),
                )

        val counts =
            BaCalendarPoolUnreadCounter.buildCounts(
                events = events,
                watermarks = nextWatermarks,
                serverIndex = 2,
            )

        assertEquals(0, counts.calendarCount)
        assertEquals(3, counts.poolCount)
    }

    @Test
    fun `calendar data diff ignores unchanged ongoing entries`() {
        val previous = listOf(calendarEntry(id = 7, title = "Dress Hina", beginAtMs = 100L, endAtMs = 1_000L))
        val next = listOf(calendarEntry(id = 7, title = "Dress Hina", beginAtMs = 100L, endAtMs = 1_000L))

        val diff = BaCalendarPoolChangeDetector.calendarDataDiff(previous, next)

        assertEquals(0, diff.changedCount)
        assertTrue(diff.changedIds.isEmpty())
    }

    @Test
    fun `calendar phase detector counts start and end once`() {
        val entry = calendarEntry(id = 9, title = "Event", beginAtMs = 1_000L, endAtMs = 2_000L)
        val upcoming =
            BaCalendarPoolChangeDetector.calendarPhaseSnapshot(
                entries = listOf(entry),
                nowMs = 900L,
            )
        val runningIds =
            BaCalendarPoolChangeDetector.calendarStatusTransitionIds(
                previousPhases = upcoming,
                nextEntries = listOf(entry),
                nowMs = 1_500L,
            )
        val running =
            BaCalendarPoolChangeDetector.calendarPhaseSnapshot(
                entries = listOf(entry),
                nowMs = 1_500L,
            )
        val stillRunningIds =
            BaCalendarPoolChangeDetector.calendarStatusTransitionIds(
                previousPhases = running,
                nextEntries = listOf(entry),
                nowMs = 1_700L,
            )
        val endedIds =
            BaCalendarPoolChangeDetector.calendarStatusTransitionIds(
                previousPhases = running,
                nextEntries = listOf(entry),
                nowMs = 2_100L,
            )

        assertEquals(setOf(9), runningIds)
        assertEquals(emptySet(), stillRunningIds)
        assertEquals(setOf(9), endedIds)
    }

    private fun unreadEvent(
        serverIndex: Int,
        kind: BaCalendarPoolUnreadKind,
        changedAtMillis: Long,
        changeCount: Int,
    ): BaCalendarPoolUnreadEvent =
        BaCalendarPoolUnreadEvent(
            id = "$serverIndex-${kind.name}-$changedAtMillis",
            serverIndex = serverIndex,
            kind = kind,
            changedAtMillis = changedAtMillis,
            changeCount = changeCount,
            fingerprint = changedAtMillis,
        )

    private fun calendarEntry(
        id: Int,
        title: String,
        beginAtMs: Long,
        endAtMs: Long,
    ): BaCalendarEntry =
        BaCalendarEntry(
            id = id,
            title = title,
            kindId = 31,
            kindName = "Event",
            beginAtMs = beginAtMs,
            endAtMs = endAtMs,
            linkUrl = "https://example.com/$id",
            imageUrl = "",
            isRunning = false,
        )
}
