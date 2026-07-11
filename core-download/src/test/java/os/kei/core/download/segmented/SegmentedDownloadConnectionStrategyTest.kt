package os.kei.core.download.segmented

import kotlin.test.assertEquals
import okhttp3.Protocol
import org.junit.Test

class SegmentedDownloadConnectionStrategyTest {
    @Test
    fun `adaptive shares HTTP 1 connection pool`() {
        assertEquals(
            SegmentedDownloadConnectionStrategy.Shared,
            resolveConnectionStrategy(
                configured = SegmentedDownloadConnectionStrategy.Adaptive,
                protocol = Protocol.HTTP_1_1,
            ),
        )
    }

    @Test
    fun `adaptive isolates HTTP 2 workers`() {
        assertEquals(
            SegmentedDownloadConnectionStrategy.IsolatedPerWorker,
            resolveConnectionStrategy(
                configured = SegmentedDownloadConnectionStrategy.Adaptive,
                protocol = Protocol.HTTP_2,
            ),
        )
    }

    @Test
    fun `explicit strategies remain unchanged`() {
        SegmentedDownloadConnectionStrategy.entries
            .filterNot { it == SegmentedDownloadConnectionStrategy.Adaptive }
            .forEach { strategy ->
                assertEquals(
                    strategy,
                    resolveConnectionStrategy(strategy, Protocol.HTTP_2),
                )
            }
    }
}
