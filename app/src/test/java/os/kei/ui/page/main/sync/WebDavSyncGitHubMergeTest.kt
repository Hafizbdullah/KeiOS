package os.kei.ui.page.main.sync

import org.junit.Test
import os.kei.BuildConfig
import os.kei.feature.github.domain.GitHubTrackedItemsTransferService
import os.kei.feature.github.model.FdroidAntiFeaturePolicy
import os.kei.feature.github.model.FdroidRepositoryPresets
import os.kei.feature.github.model.FdroidTrackedAppConfig
import os.kei.feature.github.model.FdroidVersionSelectionMode
import os.kei.feature.github.model.GitHubTrackedLocalAppType
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedSourceMode
import os.kei.feature.github.model.GitHubTrackedUpdateIntervalMode
import os.kei.feature.github.model.defaultKeiOsTrackedApp
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebDavSyncGitHubMergeTest {
    @Test
    fun `merge updates existing item and preserves local app type`() {
        val existingSelfTrack =
            defaultKeiOsTrackedApp(packageName = BuildConfig.APPLICATION_ID).copy(
                localAppType = GitHubTrackedLocalAppType.System,
                preferPreRelease = false,
            )

        val merged =
            mergeGitHubTrackedItemsForSync(
                existingItems = listOf(existingSelfTrack),
                importedItems =
                    listOf(
                        existingSelfTrack.copy(
                            localAppType = GitHubTrackedLocalAppType.Unknown,
                            preferPreRelease = true,
                        ),
                    ),
            )
        val updated = merged.first { it.id == existingSelfTrack.id }

        assertTrue(updated.preferPreRelease)
        assertEquals(GitHubTrackedLocalAppType.System, updated.localAppType)
    }

    @Test
    fun `normalization keeps self track when list is empty`() {
        val normalized = normalizeGitHubTrackedItemsForSync(emptyList())

        assertEquals(1, normalized.size)
        assertEquals(
            defaultKeiOsTrackedApp(packageName = BuildConfig.APPLICATION_ID).id,
            normalized.single().id,
        )
    }

    @Test
    fun `github tracked sync export preserves fdroid v4 config`() {
        val fdroidItem = GitHubTrackedApp(
            repoUrl = "https://apt.izzysoft.de/fdroid/repo",
            owner = "apt.izzysoft.de",
            repo = "fdroid-repo",
            packageName = "com.example.fdroid",
            appLabel = "Example",
            sourceMode = GitHubTrackedSourceMode.FdroidRepository,
            updateIntervalMode = GitHubTrackedUpdateIntervalMode.Hours24,
            fdroidConfig = FdroidTrackedAppConfig(
                selectionMode = FdroidVersionSelectionMode.VersionNameRegex,
                versionNameRegex = "^2\\.",
                antiFeaturePolicy = FdroidAntiFeaturePolicy.Custom,
                blockedAntiFeatures = listOf("Tracking"),
                repoPresetId = FdroidRepositoryPresets.IZZY_ID,
            ),
        )
        val exported = GitHubTrackedItemsTransferService.buildExportJson(
            normalizeGitHubTrackedItemsForSync(listOf(fdroidItem)),
            exportedAtMillis = 1234L,
        )
        val payload = GitHubTrackedItemsTransferService.parseImport(exported)
        val imported = payload.items.single { item -> item.packageName == "com.example.fdroid" }

        assertEquals(4, payload.schemaVersion)
        assertEquals("keios.github.tracked/v4", payload.format)
        assertEquals(GitHubTrackedSourceMode.FdroidRepository, imported.sourceMode)
        assertEquals("https://apt.izzysoft.de/fdroid/repo", imported.repoUrl)
        assertEquals(GitHubTrackedUpdateIntervalMode.Hours24, imported.updateIntervalMode)
        assertEquals(FdroidVersionSelectionMode.VersionNameRegex, imported.fdroidConfig.selectionMode)
        assertEquals("^2\\.", imported.fdroidConfig.versionNameRegex)
        assertEquals(FdroidAntiFeaturePolicy.Custom, imported.fdroidConfig.antiFeaturePolicy)
        assertEquals(listOf("Tracking"), imported.fdroidConfig.blockedAntiFeatures)
        assertEquals(FdroidRepositoryPresets.IZZY_ID, imported.fdroidConfig.repoPresetId)
    }
}
