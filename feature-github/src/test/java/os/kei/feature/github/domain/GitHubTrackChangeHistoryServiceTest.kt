package os.kei.feature.github.domain

import kotlin.test.assertEquals
import org.junit.Test
import os.kei.feature.github.model.FdroidTrackedAppConfig
import os.kei.feature.github.model.GitHubTrackChangeField
import os.kei.feature.github.model.GitHubTrackChangeHistoryAction
import os.kei.feature.github.model.GitHubTrackChangeHistorySource
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedLocalAppType
import os.kei.feature.github.model.GitHubTrackedSourceMode

class GitHubTrackChangeHistoryServiceTest {
    @Test
    fun `builds add update and delete records from item diff`() {
        val kept = trackedApp(repo = "kept", appLabel = "Kept")
        val updated = trackedApp(repo = "updated", appLabel = "Old")
        val deleted = trackedApp(repo = "deleted", appLabel = "Deleted")
        val added = trackedApp(repo = "added", appLabel = "Added")

        val records =
            GitHubTrackChangeHistoryService.buildChangeRecords(
                previousItems = listOf(kept, updated, deleted),
                nextItems = listOf(
                    kept,
                    updated.copy(
                        appLabel = "New",
                        preferPreRelease = true,
                    ),
                    added,
                ),
                source = GitHubTrackChangeHistorySource.Page,
                changedAtMillis = 1_000L,
            )

        assertEquals(
            listOf(
                GitHubTrackChangeHistoryAction.Updated,
                GitHubTrackChangeHistoryAction.Added,
                GitHubTrackChangeHistoryAction.Deleted,
            ),
            records.map { it.action },
        )
        assertEquals(
            listOf(
                GitHubTrackChangeField.AppLabel,
                GitHubTrackChangeField.PreferPreRelease,
            ),
            records.first { it.action == GitHubTrackChangeHistoryAction.Updated }.changedFields,
        )
    }

    @Test
    fun `semantic update records identity change as one update`() {
        val previous = trackedApp(repo = "old", packageName = "pkg.old")
        val next = trackedApp(repo = "new", packageName = "pkg.new")

        val records =
            GitHubTrackChangeHistoryService.buildChangeRecords(
                previousItems = listOf(previous),
                nextItems = listOf(next),
                source = GitHubTrackChangeHistorySource.Page,
                changedAtMillis = 1_000L,
                semanticUpdates =
                    listOf(
                        GitHubTrackChangeSemanticUpdate(
                            previous = previous,
                            next = next,
                        ),
                    ),
            )

        assertEquals(1, records.size)
        assertEquals(GitHubTrackChangeHistoryAction.Updated, records.single().action)
        assertEquals(previous.id, records.single().previousTrackId)
        assertEquals(
            listOf(
                GitHubTrackChangeField.Repository,
                GitHubTrackChangeField.PackageName,
                GitHubTrackChangeField.AppLabel,
            ),
            records.single().changedFields,
        )
    }

    @Test
    fun `local app type only change is ignored`() {
        val previous = trackedApp(repo = "kept", appLabel = "Kept")
        val next = previous.copy(localAppType = GitHubTrackedLocalAppType.User)

        val records =
            GitHubTrackChangeHistoryService.buildChangeRecords(
                previousItems = listOf(previous),
                nextItems = listOf(next),
                source = GitHubTrackChangeHistorySource.Page,
                changedAtMillis = 1_000L,
            )

        assertEquals(emptyList(), records)
    }

    @Test
    fun `fdroid config change is tracked`() {
        val previous = trackedApp(
            repo = "fdroid",
            sourceMode = GitHubTrackedSourceMode.FdroidRepository,
            fdroidConfig = FdroidTrackedAppConfig(repoPresetId = "main"),
        )
        val next = previous.copy(
            fdroidConfig = previous.fdroidConfig.copy(repoPresetId = "izzy"),
        )

        val records =
            GitHubTrackChangeHistoryService.buildChangeRecords(
                previousItems = listOf(previous),
                nextItems = listOf(next),
                source = GitHubTrackChangeHistorySource.Import,
                changedAtMillis = 1_000L,
            )

        assertEquals(listOf(GitHubTrackChangeField.FdroidConfig), records.single().changedFields)
    }

    private fun trackedApp(
        repo: String,
        packageName: String = "pkg.$repo",
        appLabel: String = repo.replaceFirstChar { it.titlecase() },
        sourceMode: GitHubTrackedSourceMode = GitHubTrackedSourceMode.GitHubRepository,
        fdroidConfig: FdroidTrackedAppConfig = FdroidTrackedAppConfig(),
    ): GitHubTrackedApp =
        GitHubTrackedApp(
            repoUrl =
                if (sourceMode == GitHubTrackedSourceMode.FdroidRepository) {
                    "https://f-droid.org/repo"
                } else {
                    "https://github.com/owner/$repo"
                },
            owner = "owner",
            repo = repo,
            packageName = packageName,
            appLabel = appLabel,
            sourceMode = sourceMode,
            fdroidConfig = fdroidConfig,
        )
}
