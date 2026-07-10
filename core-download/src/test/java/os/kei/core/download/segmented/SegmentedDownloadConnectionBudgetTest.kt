package os.kei.core.download.segmented

import kotlin.test.assertEquals
import org.junit.Test

class SegmentedDownloadConnectionBudgetTest {
    @Test
    fun `mobile download budget scales connections with artifact size`() {
        val options =
            SegmentedDownloadOptions(
                initialPartSizeBytes = 4L * MIB,
                minBytesPerConnection = 16L * MIB,
                maxConnections = 12,
            )

        assertEquals(2, effectiveConnectionCount(20L * MIB, options))
        assertEquals(3, effectiveConnectionCount(48L * MIB, options))
        assertEquals(6, effectiveConnectionCount(96L * MIB, options))
        assertEquals(11, effectiveConnectionCount(175L * MIB, options))
        assertEquals(12, effectiveConnectionCount(192L * MIB, options))
    }

    @Test
    fun `initial part size remains the lower bound for useful connection work`() {
        val options =
            SegmentedDownloadOptions(
                initialPartSizeBytes = 8L * MIB,
                minBytesPerConnection = 1L * MIB,
                maxConnections = 10,
            )

        assertEquals(3, effectiveConnectionCount(24L * MIB, options))
    }

    @Test
    fun `foreground boost reuses balanced sizing for low connection budgets`() {
        val options =
            SegmentedDownloadOptions(
                initialPartSizeBytes = 4L * MIB,
                minBytesPerConnection = 16L * MIB,
                maxConnections = 12,
                speedProfile = SegmentedDownloadSpeedProfile.ForegroundBoost,
            )

        assertEquals(8L * MIB, effectiveInitialPartSizeBytes(options, effectiveConnections = 4))
        assertEquals(4L * MIB, effectiveInitialPartSizeBytes(options, effectiveConnections = 5))
        assertEquals(
            SegmentedDownloadSpeedProfile.Balanced.schedulerTuning(),
            schedulerTuningFor(options, effectiveConnections = 4),
        )
        assertEquals(
            SegmentedDownloadSpeedProfile.ForegroundBoost.schedulerTuning(),
            schedulerTuningFor(options, effectiveConnections = 5),
        )
    }

    private companion object {
        const val MIB = 1024 * 1024
    }
}
