package os.kei.core.download.segmented

import org.junit.Test
import kotlin.test.assertEquals

class RangeLeaseTest {
    @Test
    fun `fresh large part has no lease`() {
        assertEquals(
            0L,
            rangeLeaseMs(
                part = DownloadPart(start = 0, endInclusive = 2L * 1024L * 1024L),
                initialPartSizeBytes = 4L * 1024L * 1024L,
            ),
        )
    }

    @Test
    fun `small part gets shorter lease`() {
        assertEquals(
            4_000L,
            rangeLeaseMs(
                part = DownloadPart(start = 0, endInclusive = 128L * 1024L),
                initialPartSizeBytes = 4L * 1024L * 1024L,
            ),
        )
    }

    @Test
    fun `stolen or retried part lease shrinks with retry count`() {
        assertEquals(
            4_000L,
            rangeLeaseMs(
                part = DownloadPart(start = 0, endInclusive = 2L * 1024L * 1024L, retryCount = 1),
                initialPartSizeBytes = 4L * 1024L * 1024L,
            ),
        )
        assertEquals(
            2_000L,
            rangeLeaseMs(
                part = DownloadPart(start = 0, endInclusive = 2L * 1024L * 1024L, retryCount = 4),
                initialPartSizeBytes = 4L * 1024L * 1024L,
            ),
        )
    }
}
