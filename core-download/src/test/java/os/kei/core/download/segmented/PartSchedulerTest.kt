package os.kei.core.download.segmented

import org.junit.Test
import kotlin.test.assertEquals

class PartSchedulerTest {
    @Test
    fun `scheduler alternates head and tail ranges deterministically`() {
        val parts = PartScheduler.buildAlternatingParts(
            totalBytes = 20,
            partSizeBytes = 5,
        )

        assertEquals(
            listOf(
                DownloadPart(start = 0, endInclusive = 4),
                DownloadPart(start = 15, endInclusive = 19),
                DownloadPart(start = 5, endInclusive = 9),
                DownloadPart(start = 10, endInclusive = 14),
            ),
            parts,
        )
    }

    @Test
    fun `scheduler clamps the tail range when bytes are uneven`() {
        val parts = PartScheduler.buildAlternatingParts(
            totalBytes = 13,
            partSizeBytes = 5,
        )

        assertEquals(
            listOf(
                DownloadPart(start = 0, endInclusive = 4),
                DownloadPart(start = 8, endInclusive = 12),
                DownloadPart(start = 5, endInclusive = 7),
            ),
            parts,
        )
    }
}
