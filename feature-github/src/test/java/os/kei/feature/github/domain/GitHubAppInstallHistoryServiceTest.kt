package os.kei.feature.github.domain

import android.content.Intent
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import os.kei.feature.github.model.GitHubAppInstallHistoryAction
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedAppInstallSnapshot

class GitHubAppInstallHistoryServiceTest {
    @Test
    fun `package added records install for tracked app`() {
        val result =
            GitHubAppInstallHistoryService.buildPackageChangeResult(
                trackedItems = listOf(trackedApp()),
                previousSnapshot = null,
                currentSnapshot = snapshot(versionName = "1.0", versionCode = 10L),
                packageName = "dev.example.app",
                action = Intent.ACTION_PACKAGE_ADDED,
                replacing = false,
                changedAtMillis = 1_000L,
            )

        assertEquals(GitHubAppInstallHistoryAction.Installed, result.records.single().action)
        assertEquals(-1L, result.records.single().previousVersionCode)
        assertEquals(10L, result.records.single().currentVersionCode)
        assertEquals("dev.example.app", result.nextSnapshot?.packageName)
        assertFalse(result.removeSnapshot)
    }

    @Test
    fun `package update sequence keeps previous snapshot during replacing removal`() {
        val previous = snapshot(versionName = "1.0", versionCode = 10L)

        val result =
            GitHubAppInstallHistoryService.buildPackageChangeResult(
                trackedItems = listOf(trackedApp()),
                previousSnapshot = previous,
                currentSnapshot = null,
                packageName = "dev.example.app",
                action = Intent.ACTION_PACKAGE_REMOVED,
                replacing = true,
                changedAtMillis = 1_000L,
            )

        assertEquals(emptyList(), result.records)
        assertEquals(previous, result.nextSnapshot)
        assertFalse(result.removeSnapshot)
    }

    @Test
    fun `package replaced records update when version code increases`() {
        val result =
            GitHubAppInstallHistoryService.buildPackageChangeResult(
                trackedItems = listOf(trackedApp()),
                previousSnapshot = snapshot(versionName = "1.0", versionCode = 10L),
                currentSnapshot = snapshot(versionName = "1.1", versionCode = 11L),
                packageName = "dev.example.app",
                action = Intent.ACTION_PACKAGE_REPLACED,
                replacing = true,
                changedAtMillis = 1_000L,
            )

        assertEquals(GitHubAppInstallHistoryAction.Updated, result.records.single().action)
        assertEquals(10L, result.records.single().previousVersionCode)
        assertEquals(11L, result.records.single().currentVersionCode)
    }

    @Test
    fun `package replaced records downgrade when version code decreases`() {
        val result =
            GitHubAppInstallHistoryService.buildPackageChangeResult(
                trackedItems = listOf(trackedApp()),
                previousSnapshot = snapshot(versionName = "2.0", versionCode = 20L),
                currentSnapshot = snapshot(versionName = "1.0", versionCode = 10L),
                packageName = "dev.example.app",
                action = Intent.ACTION_PACKAGE_REPLACED,
                replacing = true,
                changedAtMillis = 1_000L,
            )

        assertEquals(GitHubAppInstallHistoryAction.Downgraded, result.records.single().action)
        assertEquals(20L, result.records.single().previousVersionCode)
        assertEquals(10L, result.records.single().currentVersionCode)
    }

    @Test
    fun `package removed records uninstall and removes snapshot`() {
        val result =
            GitHubAppInstallHistoryService.buildPackageChangeResult(
                trackedItems = listOf(trackedApp()),
                previousSnapshot = snapshot(versionName = "1.0", versionCode = 10L),
                currentSnapshot = null,
                packageName = "dev.example.app",
                action = Intent.ACTION_PACKAGE_REMOVED,
                replacing = false,
                changedAtMillis = 1_000L,
            )

        assertEquals(GitHubAppInstallHistoryAction.Uninstalled, result.records.single().action)
        assertEquals(10L, result.records.single().previousVersionCode)
        assertEquals(-1L, result.records.single().currentVersionCode)
        assertNull(result.nextSnapshot)
        assertTrue(result.removeSnapshot)
    }

    @Test
    fun `package replaced skips duplicate record when snapshot already matches`() {
        val previous = snapshot(versionName = "1.1", versionCode = 11L)
        val current = snapshot(versionName = "1.1", versionCode = 11L)

        val result =
            GitHubAppInstallHistoryService.buildPackageChangeResult(
                trackedItems = listOf(trackedApp()),
                previousSnapshot = previous,
                currentSnapshot = current,
                packageName = "dev.example.app",
                action = Intent.ACTION_PACKAGE_REPLACED,
                replacing = true,
                changedAtMillis = 1_000L,
            )

        assertEquals(emptyList(), result.records)
        assertEquals(current, result.nextSnapshot)
    }

    private fun trackedApp(): GitHubTrackedApp =
        GitHubTrackedApp(
            repoUrl = "https://github.com/owner/repo",
            owner = "owner",
            repo = "repo",
            packageName = "dev.example.app",
            appLabel = "Example",
        )

    private fun snapshot(
        versionName: String,
        versionCode: Long,
    ): GitHubTrackedAppInstallSnapshot =
        GitHubTrackedAppInstallSnapshot(
            packageName = "dev.example.app",
            versionName = versionName,
            versionCode = versionCode,
            isSystemApp = false,
            appLabel = "Example",
            observedAtMillis = 1_000L,
        )
}
