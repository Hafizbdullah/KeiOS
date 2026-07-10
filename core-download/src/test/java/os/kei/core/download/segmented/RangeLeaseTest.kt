package os.kei.core.download.segmented

import org.junit.Test
import kotlin.test.assertEquals

class RangeLeaseTest {
    @Test
    fun `fresh large part has no lease`() {
        assertEquals(
            0L,
            rangeLeaseMs(
                remainingBytes = 2L * 1024L * 1024L + 1L,
                retryCount = 0,
                minExpectedBytesPerSecond = 64L * 1024L,
            ),
        )
    }

    @Test
    fun `lease derives from remaining bytes and minimum expected speed`() {
        assertEquals(
            8_000L,
            rangeLeaseMs(
                remainingBytes = 512L * 1024L,
                retryCount = 0,
                minExpectedBytesPerSecond = 64L * 1024L,
            ),
        )
    }

    @Test
    fun `retried small part keeps a bounded minimum lease`() {
        assertEquals(
            4_000L,
            rangeLeaseMs(
                remainingBytes = 128L * 1024L,
                retryCount = 1,
                minExpectedBytesPerSecond = 64L * 1024L,
            ),
        )
    }

    @Test
    fun `new progress resets continuous no progress expiry`() {
        var now = 0L
        val tracker = RangeLeaseTracker(
            remainingBytes = 128L * 1024L,
            retryCount = 0,
            minExpectedBytesPerSecond = 64L * 1024L,
            nowMs = { now },
        )

        now = 3_999L
        assertEquals(false, tracker.isExpired())

        tracker.recordProgress(remainingBytes = 64L * 1024L)
        now = 7_998L
        assertEquals(false, tracker.isExpired())
        now = 7_999L
        assertEquals(true, tracker.isExpired())
    }
}
