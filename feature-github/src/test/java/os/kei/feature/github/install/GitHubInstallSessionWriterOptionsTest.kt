package os.kei.feature.github.install

import kotlin.test.assertEquals
import org.junit.Test
import os.kei.core.download.segmented.SegmentedDownloadSpeedProfile

class GitHubInstallSessionWriterOptionsTest {
    @Test
    fun `balanced managed install keeps four connections`() {
        val options = githubSegmentedDownloadOptions(SegmentedDownloadSpeedProfile.Balanced)

        assertEquals(4, options.maxConnections)
        assertEquals(8L * 1024L * 1024L, options.initialPartSizeBytes)
        assertEquals(16L * 1024L * 1024L, options.minBytesPerConnection)
    }

    @Test
    fun `foreground boost managed install caps at twelve connections`() {
        val options = githubSegmentedDownloadOptions(SegmentedDownloadSpeedProfile.ForegroundBoost)

        assertEquals(12, options.maxConnections)
        assertEquals(4L * 1024L * 1024L, options.initialPartSizeBytes)
        assertEquals(16L * 1024L * 1024L, options.minBytesPerConnection)
    }
}
