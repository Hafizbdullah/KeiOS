package os.kei.core.download.segmented

import java.io.IOException
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RangeRetryPolicyTest {
    @Test
    fun `file writer failure is fatal`() {
        val error = BoundedAsyncFileWriterException(IOException("disk full"))

        assertNull(error.rangeFailureKindOrNull())
    }

    @Test
    fun `retry after parses delta seconds`() {
        assertEquals(3_000L, parseRetryAfterMs("3", nowEpochMs = 1_000L))
    }

    @Test
    fun `retry after parses http date`() {
        assertEquals(
            5_000L,
            parseRetryAfterMs(
                value = "Thu, 01 Jan 1970 00:00:10 GMT",
                nowEpochMs = 5_000L,
            ),
        )
    }
}
