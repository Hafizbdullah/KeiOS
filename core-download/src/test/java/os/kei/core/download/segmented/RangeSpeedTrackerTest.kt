package os.kei.core.download.segmented

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RangeSpeedTrackerTest {
    @Test
    fun `tracker averages only connections with measured speed`() {
        val tracker = RangeSpeedTracker()
        val first = tracker.register()
        tracker.register()

        val snapshot = tracker.update(first, speedBytesPerMs = 10.0)

        assertEquals(10.0, snapshot.averageBytesPerMs)
        assertEquals(1, snapshot.measuredPeerCount)
    }

    @Test
    fun `tracker removes finished connection from peer average`() {
        val tracker = RangeSpeedTracker()
        val first = tracker.register()
        val second = tracker.register()
        tracker.update(first, speedBytesPerMs = 10.0)
        tracker.update(second, speedBytesPerMs = 30.0)

        tracker.unregister(second)
        val snapshot = tracker.update(first, speedBytesPerMs = 20.0)

        assertEquals(20.0, snapshot.averageBytesPerMs)
        assertEquals(1, snapshot.measuredPeerCount)
    }

    @Test
    fun `slow connection decision waits for enough peers age and bytes`() {
        assertFalse(
            shouldCloseSlowConnection(
                speedBytesPerMs = 1.0,
                averageBytesPerMs = 10.0,
                measuredPeerCount = 3,
                ageMs = 10_000,
                bytes = 2L * 1024L * 1024L,
            )
        )
        assertFalse(
            shouldCloseSlowConnection(
                speedBytesPerMs = 1.0,
                averageBytesPerMs = 10.0,
                measuredPeerCount = 4,
                ageMs = 4_999,
                bytes = 2L * 1024L * 1024L,
            )
        )
        assertTrue(
            shouldCloseSlowConnection(
                speedBytesPerMs = 4.0,
                averageBytesPerMs = 10.0,
                measuredPeerCount = 4,
                ageMs = 5_000,
                bytes = 2L * 1024L * 1024L,
            )
        )
    }
}
