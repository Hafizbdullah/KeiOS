package os.kei.feature.github.model

import os.kei.core.download.segmented.SegmentedDownloadSpeedProfile
import os.kei.feature.github.install.managedInstallDownloadSpeedProfile
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubLookupConfigManagedInstallTest {
    @Test
    fun `app managed share install defaults off`() {
        assertFalse(GitHubLookupConfig().appManagedShareInstallEnabled)
    }

    @Test
    fun `app managed share install is part of copied lookup config`() {
        val config = GitHubLookupConfig().copy(appManagedShareInstallEnabled = true)

        assertTrue(config.appManagedShareInstallEnabled)
    }

    @Test
    fun `foreground managed download boost defaults off`() {
        val config = GitHubLookupConfig()

        assertFalse(config.foregroundManagedDownloadBoostEnabled)
        assertEquals(
            SegmentedDownloadSpeedProfile.Balanced,
            config.managedInstallDownloadSpeedProfile(),
        )
    }

    @Test
    fun `foreground managed download boost maps to boost profile`() {
        val config = GitHubLookupConfig().copy(foregroundManagedDownloadBoostEnabled = true)

        assertEquals(
            SegmentedDownloadSpeedProfile.ForegroundBoost,
            config.managedInstallDownloadSpeedProfile(),
        )
    }
}
