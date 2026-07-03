package os.kei.feature.github.domain

import android.content.Intent
import org.junit.Test
import os.kei.feature.github.model.GitHubAppInstallHistoryAction
import os.kei.feature.github.model.GitHubTrackChangeHistoryAction
import os.kei.feature.github.model.GitHubTrackChangeHistorySource
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedAppInstallSnapshot
import os.kei.feature.github.model.GitHubTrackedSourceMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubHistoryV4DeviceFixtureTest {
    @Test
    fun `real device fixture import can produce track history for every source mode`() {
        val records =
            GitHubTrackChangeHistoryService.buildChangeRecords(
                previousItems = emptyList(),
                nextItems = GitHubTrackExportV4DeviceFixture.trackedItems,
                source = GitHubTrackChangeHistorySource.Import,
                changedAtMillis = 1_783_081_672_942L,
            )

        assertEquals(GitHubTrackExportV4DeviceFixture.expectedItemCount, records.size)
        assertEquals(
            setOf(GitHubTrackChangeHistoryAction.Added),
            records.map { it.action }.toSet(),
        )
        assertEquals(
            GitHubTrackExportV4DeviceFixture.expectedGitHubRepositoryCount,
            records.count { it.sourceMode == GitHubTrackedSourceMode.GitHubRepository },
        )
        assertEquals(
            GitHubTrackExportV4DeviceFixture.expectedGitRepositoryCount,
            records.count { it.sourceMode == GitHubTrackedSourceMode.GitRepository },
        )
        assertEquals(
            GitHubTrackExportV4DeviceFixture.expectedDirectApkCount,
            records.count { it.sourceMode == GitHubTrackedSourceMode.DirectApk },
        )
        assertTrue(records.all { it.source == GitHubTrackChangeHistorySource.Import })
        assertEquals(
            GitHubTrackedSourceMode.GitRepository,
            records.first { it.packageName == "com.mt.pronto" }.sourceMode,
        )
        assertEquals(
            GitHubTrackedSourceMode.DirectApk,
            records.first { it.packageName == "org.telegram.messenger.beta" }.sourceMode,
        )
    }

    @Test
    fun `real device fixture app history records installs updates downgrades and removals`() {
        val install =
            buildPackageChange(
                item = GitHubTrackExportV4DeviceFixture.itemByPackage("org.telegram.messenger.beta"),
                previousSnapshot = null,
                currentSnapshot = snapshot(
                    packageName = "org.telegram.messenger.beta",
                    versionName = "12.0.0",
                    versionCode = 12_000L,
                    appLabel = "Telegram Beta",
                ),
                action = Intent.ACTION_PACKAGE_ADDED,
            )
        val update =
            buildPackageChange(
                item = GitHubTrackExportV4DeviceFixture.itemByPackage("in.sunilpaulmathew.ashell"),
                previousSnapshot = snapshot(
                    packageName = "in.sunilpaulmathew.ashell",
                    versionName = "6.0",
                    versionCode = 60L,
                    appLabel = "aShell",
                ),
                currentSnapshot = snapshot(
                    packageName = "in.sunilpaulmathew.ashell",
                    versionName = "6.1",
                    versionCode = 61L,
                    appLabel = "aShell",
                ),
                action = Intent.ACTION_PACKAGE_REPLACED,
                replacing = true,
            )
        val downgrade =
            buildPackageChange(
                item = GitHubTrackExportV4DeviceFixture.itemByPackage("com.mt.pronto"),
                previousSnapshot = snapshot(
                    packageName = "com.mt.pronto",
                    versionName = "3.0",
                    versionCode = 30L,
                    appLabel = "大狗记",
                ),
                currentSnapshot = snapshot(
                    packageName = "com.mt.pronto",
                    versionName = "2.9",
                    versionCode = 29L,
                    appLabel = "大狗记",
                ),
                action = Intent.ACTION_PACKAGE_REPLACED,
                replacing = true,
            )
        val uninstall =
            buildPackageChange(
                item = GitHubTrackExportV4DeviceFixture.itemByPackage("os.kei"),
                previousSnapshot = snapshot(
                    packageName = "os.kei",
                    versionName = "1.9.2",
                    versionCode = 192L,
                    appLabel = "KeiOS",
                ),
                currentSnapshot = null,
                action = Intent.ACTION_PACKAGE_REMOVED,
            )

        assertEquals(GitHubAppInstallHistoryAction.Installed, install.records.single().action)
        assertEquals(GitHubTrackedSourceMode.DirectApk, install.records.single().sourceMode)
        assertEquals(
            GitHubAppInstallHistoryAction.Updated,
            update.records.single().action,
        )
        assertEquals(GitHubTrackedSourceMode.GitRepository, update.records.single().sourceMode)
        assertEquals(
            GitHubAppInstallHistoryAction.Downgraded,
            downgrade.records.single().action,
        )
        assertEquals(GitHubTrackedSourceMode.GitRepository, downgrade.records.single().sourceMode)
        assertEquals(
            GitHubAppInstallHistoryAction.Uninstalled,
            uninstall.records.single().action,
        )
        assertEquals(GitHubTrackedSourceMode.GitHubRepository, uninstall.records.single().sourceMode)
        assertEquals(true, uninstall.removeSnapshot)
    }

    private fun buildPackageChange(
        item: GitHubTrackedApp,
        previousSnapshot: GitHubTrackedAppInstallSnapshot?,
        currentSnapshot: GitHubTrackedAppInstallSnapshot?,
        action: String,
        replacing: Boolean = false,
    ) =
        GitHubAppInstallHistoryService.buildPackageChangeResult(
            trackedItems = listOf(item),
            previousSnapshot = previousSnapshot,
            currentSnapshot = currentSnapshot,
            packageName = item.packageName,
            action = action,
            replacing = replacing,
            changedAtMillis = 1_783_081_672_942L,
        )

    private fun snapshot(
        packageName: String,
        versionName: String,
        versionCode: Long,
        appLabel: String,
    ): GitHubTrackedAppInstallSnapshot =
        GitHubTrackedAppInstallSnapshot(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            isSystemApp = false,
            appLabel = appLabel,
            observedAtMillis = 1_783_081_672_942L,
        )
}
