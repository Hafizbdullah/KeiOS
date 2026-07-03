package os.kei.ui.page.main.github.history

import org.junit.Test
import os.kei.feature.github.model.GitHubActionsNotificationHistoryRecord
import os.kei.feature.github.model.GitHubTrackChangeField
import os.kei.feature.github.model.GitHubTrackChangeHistoryAction
import os.kei.feature.github.model.GitHubTrackChangeHistoryRecord
import os.kei.feature.github.model.GitHubTrackChangeHistorySource
import os.kei.feature.github.model.GitHubTrackedSourceMode
import kotlin.test.assertEquals

class GitHubActionsNotificationHistoryOptionsTest {
    @Test
    fun `filters by actions outcome and android artifacts`() {
        val records =
            listOf(
                createUiRecord(appLabel = "Stable", conclusion = "success", androidArtifactCount = 1),
                createUiRecord(appLabel = "Failed", conclusion = "failure"),
                createUiRecord(appLabel = "Cancelled", conclusion = "cancelled"),
                createUiRecord(appLabel = "Running", status = "in_progress", conclusion = ""),
            )

        assertEquals(
            listOf("Stable"),
            records.display(filterMode = GitHubActionsHistoryFilterMode.Success).appLabels(),
        )
        assertEquals(
            listOf("Cancelled", "Failed"),
            records.display(filterMode = GitHubActionsHistoryFilterMode.Failed).appLabels(),
        )
        assertEquals(
            listOf("Running"),
            records.display(filterMode = GitHubActionsHistoryFilterMode.Running).appLabels(),
        )
        assertEquals(
            listOf("Stable"),
            records.display(filterMode = GitHubActionsHistoryFilterMode.AndroidArtifacts).appLabels(),
        )
    }

    @Test
    fun `sorts by time app repository workflow and run`() {
        val records =
            listOf(
                createUiRecord(
                    appLabel = "Beta",
                    repo = "middle",
                    workflowName = "Package",
                    runNumber = 20L,
                    notifiedAtMillis = 2000L,
                ),
                createUiRecord(
                    appLabel = "Alpha",
                    repo = "omega",
                    workflowName = "Release",
                    runNumber = 10L,
                    notifiedAtMillis = 1000L,
                ),
                createUiRecord(
                    appLabel = "Zulu",
                    repo = "alpha",
                    workflowName = "Build",
                    runNumber = 30L,
                    notifiedAtMillis = 3000L,
                ),
            )

        assertEquals(
            listOf("Zulu", "Beta", "Alpha"),
            records.display(sortMode = GitHubActionsHistorySortMode.NotifiedAt).appLabels(),
        )
        assertEquals(
            listOf("Alpha", "Beta", "Zulu"),
            records.display(
                sortMode = GitHubActionsHistorySortMode.App,
                sortDirection = GitHubActionsHistorySortDirection.Ascending,
            ).appLabels(),
        )
        assertEquals(
            listOf("Alpha", "Beta", "Zulu"),
            records.display(sortMode = GitHubActionsHistorySortMode.Repository).appLabels(),
        )
        assertEquals(
            listOf("Alpha", "Beta", "Zulu"),
            records.display(sortMode = GitHubActionsHistorySortMode.Workflow).appLabels(),
        )
        assertEquals(
            listOf("Zulu", "Beta", "Alpha"),
            records.display(sortMode = GitHubActionsHistorySortMode.RunNumber).appLabels(),
        )
    }

    @Test
    fun `filters sorts and searches tracking history`() {
        val records =
            listOf(
                createTrackChangeUiRecord(
                    appLabel = "Beta",
                    repo = "middle",
                    action = GitHubTrackChangeHistoryAction.Updated,
                    changedAtMillis = 2_000L,
                    changedFields = listOf(GitHubTrackChangeField.UpdateInterval),
                ),
                createTrackChangeUiRecord(
                    appLabel = "Alpha",
                    repo = "omega",
                    action = GitHubTrackChangeHistoryAction.Added,
                    changedAtMillis = 1_000L,
                ),
                createTrackChangeUiRecord(
                    appLabel = "Zulu",
                    repo = "alpha",
                    action = GitHubTrackChangeHistoryAction.Deleted,
                    changedAtMillis = 3_000L,
                ),
            )

        assertEquals(
            listOf("Alpha"),
            records.trackDisplay(filterMode = GitHubTrackChangeHistoryFilterMode.Added).trackAppLabels(),
        )
        assertEquals(
            listOf("Zulu", "Beta", "Alpha"),
            records.trackDisplay(sortMode = GitHubTrackChangeHistorySortMode.ChangedAt).trackAppLabels(),
        )
        assertEquals(
            listOf("Alpha", "Beta", "Zulu"),
            records.trackDisplay(
                sortMode = GitHubTrackChangeHistorySortMode.App,
                sortDirection = GitHubActionsHistorySortDirection.Ascending,
            ).trackAppLabels(),
        )
        assertEquals(
            listOf("Beta"),
            records.trackDisplay(searchQuery = "updateinterval").trackAppLabels(),
        )
    }

    private fun List<GitHubActionsNotificationHistoryUiRecord>.display(
        filterMode: GitHubActionsHistoryFilterMode = GitHubActionsHistoryFilterMode.All,
        sortMode: GitHubActionsHistorySortMode = GitHubActionsHistorySortMode.NotifiedAt,
        sortDirection: GitHubActionsHistorySortDirection = GitHubActionsHistorySortDirection.Descending,
    ): List<GitHubActionsNotificationHistoryUiRecord> =
        buildGitHubActionsHistoryDisplayRecords(
            records = this,
            filterMode = filterMode,
            sortMode = sortMode,
            sortDirection = sortDirection,
        )

    private fun List<GitHubActionsNotificationHistoryUiRecord>.appLabels(): List<String> =
        map { item -> item.record.appLabel }

    private fun List<GitHubTrackChangeHistoryUiRecord>.trackDisplay(
        filterMode: GitHubTrackChangeHistoryFilterMode = GitHubTrackChangeHistoryFilterMode.All,
        sortMode: GitHubTrackChangeHistorySortMode = GitHubTrackChangeHistorySortMode.ChangedAt,
        sortDirection: GitHubActionsHistorySortDirection = GitHubActionsHistorySortDirection.Descending,
        searchQuery: String = "",
    ): List<GitHubTrackChangeHistoryUiRecord> =
        buildGitHubTrackChangeHistoryDisplayRecords(
            records = this,
            filterMode = filterMode,
            sortMode = sortMode,
            sortDirection = sortDirection,
            searchQuery = searchQuery,
        )

    private fun List<GitHubTrackChangeHistoryUiRecord>.trackAppLabels(): List<String> =
        map { item -> item.record.appLabel }

    private fun createUiRecord(
        appLabel: String,
        repo: String = appLabel.lowercase(),
        workflowName: String = "CI",
        status: String = "completed",
        conclusion: String = "success",
        runNumber: Long = 1L,
        notifiedAtMillis: Long = runNumber * 1000L,
        androidArtifactCount: Int = 0,
    ): GitHubActionsNotificationHistoryUiRecord =
        GitHubActionsNotificationHistoryUiRecord(
            record =
                GitHubActionsNotificationHistoryRecord(
                    trackId = "owner/$repo|pkg.$repo",
                    owner = "owner",
                    repo = repo,
                    appLabel = appLabel,
                    workflowId = runNumber,
                    workflowName = workflowName,
                    workflowPath = ".github/workflows/$repo.yml",
                    runId = runNumber * 100L,
                    runNumber = runNumber,
                    runAttempt = 1,
                    runDisplayName = "Run $runNumber",
                    headBranch = "main",
                    headSha = "abcdef$runNumber",
                    event = "workflow_dispatch",
                    status = status,
                    conclusion = conclusion,
                    htmlUrl = "https://github.com/owner/$repo/actions/runs/${runNumber * 100L}",
                    artifactCount = androidArtifactCount,
                    androidArtifactCount = androidArtifactCount,
                    checkedAtMillis = notifiedAtMillis - 100L,
                    notifiedAtMillis = notifiedAtMillis,
                    notificationTitle = "Actions",
                    notificationContent = "$appLabel #$runNumber",
                ),
            packageName = "pkg.$repo",
        )

    private fun createTrackChangeUiRecord(
        appLabel: String,
        repo: String = appLabel.lowercase(),
        action: GitHubTrackChangeHistoryAction = GitHubTrackChangeHistoryAction.Updated,
        changedAtMillis: Long = 1_000L,
        changedFields: List<GitHubTrackChangeField> = emptyList(),
    ): GitHubTrackChangeHistoryUiRecord =
        GitHubTrackChangeHistoryUiRecord(
            record =
                GitHubTrackChangeHistoryRecord(
                    id = "$repo-$changedAtMillis",
                    trackId = "owner/$repo|pkg.$repo",
                    action = action,
                    source = GitHubTrackChangeHistorySource.Page,
                    changedAtMillis = changedAtMillis,
                    owner = "owner",
                    repo = repo,
                    repoUrl = "https://github.com/owner/$repo",
                    packageName = "pkg.$repo",
                    appLabel = appLabel,
                    sourceMode = GitHubTrackedSourceMode.GitHubRepository,
                    changedFields = changedFields,
                ),
        )
}
