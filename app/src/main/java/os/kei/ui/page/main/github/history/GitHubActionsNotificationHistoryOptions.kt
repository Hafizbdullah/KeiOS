package os.kei.ui.page.main.github.history

import androidx.annotation.StringRes
import os.kei.R
import os.kei.feature.github.model.GitHubAppInstallHistoryAction
import os.kei.feature.github.model.GitHubRefreshHistoryOutcome
import os.kei.feature.github.model.GitHubTrackChangeHistoryAction
import os.kei.ui.page.main.widget.chrome.TabbedPageCategory
import java.util.Locale
import com.composables.icons.lucide.R as LucideR

internal enum class GitHubHistoryMode(
    override val iconRes: Int,
    @param:StringRes override val labelRes: Int,
) : TabbedPageCategory {
    Refresh(LucideR.drawable.lucide_ic_history, R.string.github_history_mode_refresh),
    Actions(LucideR.drawable.lucide_ic_bell, R.string.github_history_mode_actions),
    Tracking(LucideR.drawable.lucide_ic_git_branch, R.string.github_history_mode_tracking),
    Apps(LucideR.drawable.lucide_ic_package, R.string.github_history_mode_apps),
}

internal enum class GitHubActionsHistoryFilterMode(
    @param:StringRes val labelRes: Int,
) {
    All(R.string.github_actions_history_filter_all),
    Success(R.string.github_actions_history_filter_success),
    Failed(R.string.github_actions_history_filter_failed),
    Running(R.string.github_actions_history_filter_running),
    AndroidArtifacts(R.string.github_actions_history_filter_android_artifacts),
}

internal enum class GitHubRefreshHistoryFilterMode(
    @param:StringRes val labelRes: Int,
) {
    All(R.string.github_actions_history_filter_all),
    Completed(R.string.github_history_refresh_filter_completed),
    Updatable(R.string.github_history_refresh_filter_updatable),
    Failed(R.string.github_actions_history_filter_failed),
    Cancelled(R.string.github_history_refresh_filter_cancelled),
}

internal enum class GitHubTrackChangeHistoryFilterMode(
    @param:StringRes val labelRes: Int,
) {
    All(R.string.github_actions_history_filter_all),
    Added(R.string.github_history_tracking_filter_added),
    Updated(R.string.github_history_tracking_filter_updated),
    Deleted(R.string.github_history_tracking_filter_deleted),
}

internal enum class GitHubAppInstallHistoryFilterMode(
    @param:StringRes val labelRes: Int,
) {
    All(R.string.github_actions_history_filter_all),
    Installed(R.string.github_history_apps_filter_installed),
    Updated(R.string.github_history_apps_filter_updated),
    Downgraded(R.string.github_history_apps_filter_downgraded),
    Uninstalled(R.string.github_history_apps_filter_uninstalled),
}

internal enum class GitHubRefreshHistorySortMode(
    @param:StringRes val labelRes: Int,
) {
    FinishedAt(R.string.github_history_refresh_sort_finished),
    Elapsed(R.string.github_history_refresh_sort_elapsed),
    TargetCount(R.string.github_history_refresh_sort_target),
    UpdateCount(R.string.github_history_refresh_sort_updates),
    FailedCount(R.string.github_history_refresh_sort_failed),
}

internal enum class GitHubTrackChangeHistorySortMode(
    @param:StringRes val labelRes: Int,
) {
    ChangedAt(R.string.github_history_tracking_sort_changed),
    App(R.string.github_actions_history_sort_app),
    Repository(R.string.github_actions_history_sort_repository),
    Source(R.string.github_history_refresh_label_source),
}

internal enum class GitHubAppInstallHistorySortMode(
    @param:StringRes val labelRes: Int,
) {
    ChangedAt(R.string.github_history_apps_sort_changed),
    App(R.string.github_actions_history_sort_app),
    Repository(R.string.github_actions_history_sort_repository),
    Version(R.string.github_history_apps_sort_version),
}

internal enum class GitHubActionsHistorySortMode(
    @param:StringRes val labelRes: Int,
) {
    NotifiedAt(R.string.github_actions_history_sort_notified),
    App(R.string.github_actions_history_sort_app),
    Repository(R.string.github_actions_history_sort_repository),
    Workflow(R.string.github_actions_history_sort_workflow),
    RunNumber(R.string.github_actions_history_sort_run),
}

internal enum class GitHubActionsHistorySortDirection(
    @param:StringRes val labelRes: Int,
) {
    Descending(R.string.github_actions_history_sort_descending),
    Ascending(R.string.github_actions_history_sort_ascending),
}

internal enum class GitHubActionsHistoryCleanupAge(
    val days: Int,
    @param:StringRes val labelRes: Int,
) {
    SevenDays(7, R.string.github_actions_history_cleanup_7d),
    ThirtyDays(30, R.string.github_actions_history_cleanup_30d),
    NinetyDays(90, R.string.github_actions_history_cleanup_90d),
}

internal fun buildGitHubActionsHistoryDisplayRecords(
    records: List<GitHubActionsNotificationHistoryUiRecord>,
    filterMode: GitHubActionsHistoryFilterMode,
    sortMode: GitHubActionsHistorySortMode,
    sortDirection: GitHubActionsHistorySortDirection,
    searchQuery: String = "",
): List<GitHubActionsNotificationHistoryUiRecord> {
    val normalizedQuery = searchQuery.normalizedHistorySearchQuery()
    val filtered =
        records.filter { item ->
            val record = item.record
            when (filterMode) {
                GitHubActionsHistoryFilterMode.All -> true
                GitHubActionsHistoryFilterMode.Success ->
                    record.conclusion.equals("success", ignoreCase = true)
                GitHubActionsHistoryFilterMode.Failed ->
                    record.conclusion.equals("failure", ignoreCase = true) ||
                        record.conclusion.equals("cancelled", ignoreCase = true) ||
                        record.conclusion.equals("timed_out", ignoreCase = true) ||
                        record.conclusion.equals("action_required", ignoreCase = true) ||
                        record.conclusion.equals("startup_failure", ignoreCase = true)
                GitHubActionsHistoryFilterMode.Running ->
                    record.status.equals("in_progress", ignoreCase = true) ||
                        record.status.equals("queued", ignoreCase = true) ||
                        record.status.equals("waiting", ignoreCase = true) ||
                        record.status.equals("pending", ignoreCase = true)
                GitHubActionsHistoryFilterMode.AndroidArtifacts -> record.androidArtifactCount > 0
            }
        }.filter { item ->
            normalizedQuery.isBlank() || item.matchesActionsHistorySearch(normalizedQuery)
        }
    val comparator = gitHubActionsHistoryComparator(sortMode)
    val sorted = filtered.sortedWith(comparator)
    return when (sortDirection) {
        GitHubActionsHistorySortDirection.Descending -> sorted
        GitHubActionsHistorySortDirection.Ascending -> sorted.asReversed()
    }
}

internal fun buildGitHubRefreshHistoryDisplayRecords(
    records: List<GitHubRefreshHistoryUiRecord>,
    filterMode: GitHubRefreshHistoryFilterMode,
    sortMode: GitHubRefreshHistorySortMode,
    sortDirection: GitHubActionsHistorySortDirection,
    searchQuery: String = "",
): List<GitHubRefreshHistoryUiRecord> {
    val normalizedQuery = searchQuery.normalizedHistorySearchQuery()
    val filtered =
        records.filter { item ->
            val record = item.record
            when (filterMode) {
                GitHubRefreshHistoryFilterMode.All -> true
                GitHubRefreshHistoryFilterMode.Completed ->
                    record.outcome == GitHubRefreshHistoryOutcome.Completed && record.failedCount == 0
                GitHubRefreshHistoryFilterMode.Updatable ->
                    record.updatableCount > 0 || record.preReleaseUpdateCount > 0
                GitHubRefreshHistoryFilterMode.Failed ->
                    record.outcome == GitHubRefreshHistoryOutcome.Failed || record.failedCount > 0
                GitHubRefreshHistoryFilterMode.Cancelled ->
                    record.outcome == GitHubRefreshHistoryOutcome.Cancelled
            }
        }.filter { item ->
            normalizedQuery.isBlank() || item.matchesRefreshHistorySearch(normalizedQuery)
        }
    val comparator = gitHubRefreshHistoryComparator(sortMode)
    val sorted = filtered.sortedWith(comparator)
    return when (sortDirection) {
        GitHubActionsHistorySortDirection.Descending -> sorted
        GitHubActionsHistorySortDirection.Ascending -> sorted.asReversed()
    }
}

internal fun buildGitHubTrackChangeHistoryDisplayRecords(
    records: List<GitHubTrackChangeHistoryUiRecord>,
    filterMode: GitHubTrackChangeHistoryFilterMode,
    sortMode: GitHubTrackChangeHistorySortMode,
    sortDirection: GitHubActionsHistorySortDirection,
    searchQuery: String = "",
): List<GitHubTrackChangeHistoryUiRecord> {
    val normalizedQuery = searchQuery.normalizedHistorySearchQuery()
    val filtered =
        records.filter { item ->
            val action = item.record.action
            when (filterMode) {
                GitHubTrackChangeHistoryFilterMode.All -> true
                GitHubTrackChangeHistoryFilterMode.Added ->
                    action == GitHubTrackChangeHistoryAction.Added
                GitHubTrackChangeHistoryFilterMode.Updated ->
                    action == GitHubTrackChangeHistoryAction.Updated
                GitHubTrackChangeHistoryFilterMode.Deleted ->
                    action == GitHubTrackChangeHistoryAction.Deleted
            }
        }.filter { item ->
            normalizedQuery.isBlank() || item.matchesTrackChangeHistorySearch(normalizedQuery)
        }
    val comparator = gitHubTrackChangeHistoryComparator(sortMode)
    val sorted = filtered.sortedWith(comparator)
    return when (sortDirection) {
        GitHubActionsHistorySortDirection.Descending -> sorted
        GitHubActionsHistorySortDirection.Ascending -> sorted.asReversed()
    }
}

internal fun buildGitHubAppInstallHistoryDisplayRecords(
    records: List<GitHubAppInstallHistoryUiRecord>,
    filterMode: GitHubAppInstallHistoryFilterMode,
    sortMode: GitHubAppInstallHistorySortMode,
    sortDirection: GitHubActionsHistorySortDirection,
    searchQuery: String = "",
): List<GitHubAppInstallHistoryUiRecord> {
    val normalizedQuery = searchQuery.normalizedHistorySearchQuery()
    val filtered =
        records.filter { item ->
            val action = item.record.action
            when (filterMode) {
                GitHubAppInstallHistoryFilterMode.All -> true
                GitHubAppInstallHistoryFilterMode.Installed ->
                    action == GitHubAppInstallHistoryAction.Installed
                GitHubAppInstallHistoryFilterMode.Updated ->
                    action == GitHubAppInstallHistoryAction.Updated
                GitHubAppInstallHistoryFilterMode.Downgraded ->
                    action == GitHubAppInstallHistoryAction.Downgraded
                GitHubAppInstallHistoryFilterMode.Uninstalled ->
                    action == GitHubAppInstallHistoryAction.Uninstalled
            }
        }.filter { item ->
            normalizedQuery.isBlank() || item.matchesAppInstallHistorySearch(normalizedQuery)
        }
    val comparator = gitHubAppInstallHistoryComparator(sortMode)
    val sorted = filtered.sortedWith(comparator)
    return when (sortDirection) {
        GitHubActionsHistorySortDirection.Descending -> sorted
        GitHubActionsHistorySortDirection.Ascending -> sorted.asReversed()
    }
}

private fun String.normalizedHistorySearchQuery(): String = trim().lowercase(Locale.ROOT)

private fun GitHubActionsNotificationHistoryUiRecord.matchesActionsHistorySearch(query: String): Boolean {
    val record = record
    return listOf(
        packageName,
        record.trackId,
        record.owner,
        record.repo,
        record.repositoryLabel,
        record.appLabel,
        record.workflowName,
        record.workflowPath,
        record.runLabel,
        record.runDisplayName,
        record.headBranch,
        record.headSha,
        record.event,
        record.status,
        record.conclusion,
        record.notificationTitle,
        record.notificationContent,
    ).containsHistoryQuery(query)
}

private fun GitHubRefreshHistoryUiRecord.matchesRefreshHistorySearch(query: String): Boolean {
    val record = record
    val failureTokens =
        record.failureSummaries.flatMap { failure ->
            listOf(
                failure.trackId,
                failure.owner,
                failure.repo,
                "${failure.owner}/${failure.repo}",
                failure.packageName,
                failure.appLabel,
                failure.sourceMode,
                failure.failureCategory,
                failure.responseType,
                failure.limitStage,
                failure.message,
            )
        }
    val slowItemTokens =
        record.slowItems.flatMap { slowItem ->
            listOf(
                slowItem.trackId,
                slowItem.owner,
                slowItem.repo,
                "${slowItem.owner}/${slowItem.repo}",
                slowItem.packageName,
                slowItem.appLabel,
                slowItem.sourceMode,
                slowItem.status,
                slowItem.message,
                slowItem.strategyId,
                slowItem.fallbackStrategyId,
                slowItem.snapshotFromCache.toString(),
                slowItem.profileFromCache.toString(),
                slowItem.preciseApkRequested.toString(),
            )
        }
    return (
        listOf(
            record.id,
            record.sessionId.toString(),
            record.scope.name,
            record.source.name,
            record.outcome.name,
            record.note,
        ) + failureTokens + slowItemTokens
    ).containsHistoryQuery(query)
}

private fun GitHubTrackChangeHistoryUiRecord.matchesTrackChangeHistorySearch(query: String): Boolean {
    val record = record
    return (
        listOf(
            record.id,
            record.trackId,
            record.previousTrackId,
            record.owner,
            record.repo,
            "${record.owner}/${record.repo}",
            record.repoUrl,
            record.packageName,
            record.appLabel,
            record.sourceMode.storageId,
            record.sourceMode.name,
            record.action.name,
            record.source.name,
            record.note,
        ) + record.changedFields.map { it.name }
    ).containsHistoryQuery(query)
}

private fun GitHubAppInstallHistoryUiRecord.matchesAppInstallHistorySearch(query: String): Boolean {
    val record = record
    return listOf(
        record.id,
        record.trackId,
        record.owner,
        record.repo,
        "${record.owner}/${record.repo}",
        record.repoUrl,
        record.packageName,
        record.appLabel,
        record.sourceMode.storageId,
        record.sourceMode.name,
        record.action.name,
        record.source.name,
        record.previousVersionName,
        record.previousVersionCode.toString(),
        record.currentVersionName,
        record.currentVersionCode.toString(),
        record.broadcastAction,
        record.broadcastUid.toString(),
        record.broadcastDataRemoved.toString(),
        record.broadcastUserInitiated.toString(),
        record.broadcastArchival.toString(),
        record.previousInstallSourceInfo.installingPackageName,
        record.previousInstallSourceInfo.installingPackageLabel,
        record.previousInstallSourceInfo.initiatingPackageName,
        record.previousInstallSourceInfo.initiatingPackageLabel,
        record.previousInstallSourceInfo.originatingPackageName,
        record.previousInstallSourceInfo.originatingPackageLabel,
        record.previousInstallSourceInfo.updateOwnerPackageName,
        record.previousInstallSourceInfo.updateOwnerPackageLabel,
        record.previousInstallSourceInfo.packageSource.toString(),
        record.currentInstallSourceInfo.installingPackageName,
        record.currentInstallSourceInfo.installingPackageLabel,
        record.currentInstallSourceInfo.initiatingPackageName,
        record.currentInstallSourceInfo.initiatingPackageLabel,
        record.currentInstallSourceInfo.originatingPackageName,
        record.currentInstallSourceInfo.originatingPackageLabel,
        record.currentInstallSourceInfo.updateOwnerPackageName,
        record.currentInstallSourceInfo.updateOwnerPackageLabel,
        record.currentInstallSourceInfo.packageSource.toString(),
        record.note,
    ).containsHistoryQuery(query)
}

private fun Iterable<String>.containsHistoryQuery(query: String): Boolean =
    any { value -> value.lowercase(Locale.ROOT).contains(query) }

private fun gitHubRefreshHistoryComparator(
    sortMode: GitHubRefreshHistorySortMode,
): Comparator<GitHubRefreshHistoryUiRecord> {
    val tieBreakers =
        compareByDescending<GitHubRefreshHistoryUiRecord> { it.record.finishedAtMillis }
            .thenByDescending { it.record.startedAtMillis }
    return when (sortMode) {
        GitHubRefreshHistorySortMode.FinishedAt ->
            compareByDescending<GitHubRefreshHistoryUiRecord> { it.record.finishedAtMillis }
                .thenByDescending { it.record.sessionId }
        GitHubRefreshHistorySortMode.Elapsed ->
            compareByDescending<GitHubRefreshHistoryUiRecord> { it.record.elapsedMs }
                .then(tieBreakers)
        GitHubRefreshHistorySortMode.TargetCount ->
            compareByDescending<GitHubRefreshHistoryUiRecord> { it.record.targetCount }
                .then(tieBreakers)
        GitHubRefreshHistorySortMode.UpdateCount ->
            compareByDescending<GitHubRefreshHistoryUiRecord> {
                it.record.updatableCount + it.record.preReleaseUpdateCount
            }.then(tieBreakers)
        GitHubRefreshHistorySortMode.FailedCount ->
            compareByDescending<GitHubRefreshHistoryUiRecord> { it.record.failedCount }
                .then(tieBreakers)
    }
}

private fun gitHubActionsHistoryComparator(
    sortMode: GitHubActionsHistorySortMode,
): Comparator<GitHubActionsNotificationHistoryUiRecord> {
    val textComparator = compareByDescending<GitHubActionsNotificationHistoryUiRecord> {
        it.record.appLabel.ifBlank { it.record.repositoryLabel }.lowercase(Locale.ROOT)
    }
    val tieBreakers =
        compareByDescending<GitHubActionsNotificationHistoryUiRecord> { it.record.notifiedAtMillis }
            .thenByDescending { it.record.runNumber }
    return when (sortMode) {
        GitHubActionsHistorySortMode.NotifiedAt ->
            compareByDescending<GitHubActionsNotificationHistoryUiRecord> { it.record.notifiedAtMillis }
                .thenByDescending { it.record.runNumber }
                .thenBy { it.record.appLabel.lowercase(Locale.ROOT) }
        GitHubActionsHistorySortMode.App ->
            textComparator.then(tieBreakers)
        GitHubActionsHistorySortMode.Repository ->
            compareByDescending<GitHubActionsNotificationHistoryUiRecord> {
                it.record.repositoryLabel.lowercase(Locale.ROOT)
            }.then(tieBreakers)
        GitHubActionsHistorySortMode.Workflow ->
            compareByDescending<GitHubActionsNotificationHistoryUiRecord> {
                it.record.workflowName
                    .ifBlank { it.record.workflowPath }
                    .lowercase(Locale.ROOT)
            }.then(tieBreakers)
        GitHubActionsHistorySortMode.RunNumber ->
            compareByDescending<GitHubActionsNotificationHistoryUiRecord> { it.record.runNumber }
                .thenByDescending { it.record.runId }
                .thenByDescending { it.record.notifiedAtMillis }
    }
}

private fun gitHubTrackChangeHistoryComparator(
    sortMode: GitHubTrackChangeHistorySortMode,
): Comparator<GitHubTrackChangeHistoryUiRecord> {
    val textComparator = compareByDescending<GitHubTrackChangeHistoryUiRecord> {
        it.record.appLabel
            .ifBlank { "${it.record.owner}/${it.record.repo}" }
            .lowercase(Locale.ROOT)
    }
    val tieBreakers =
        compareByDescending<GitHubTrackChangeHistoryUiRecord> { it.record.changedAtMillis }
            .thenByDescending { it.record.trackId }
    return when (sortMode) {
        GitHubTrackChangeHistorySortMode.ChangedAt ->
            compareByDescending<GitHubTrackChangeHistoryUiRecord> { it.record.changedAtMillis }
                .thenByDescending { it.record.trackId }
        GitHubTrackChangeHistorySortMode.App ->
            textComparator.then(tieBreakers)
        GitHubTrackChangeHistorySortMode.Repository ->
            compareByDescending<GitHubTrackChangeHistoryUiRecord> {
                "${it.record.owner}/${it.record.repo}".lowercase(Locale.ROOT)
            }.then(tieBreakers)
        GitHubTrackChangeHistorySortMode.Source ->
            compareByDescending<GitHubTrackChangeHistoryUiRecord> {
                it.record.source.name.lowercase(Locale.ROOT)
            }.then(tieBreakers)
    }
}

private fun gitHubAppInstallHistoryComparator(
    sortMode: GitHubAppInstallHistorySortMode,
): Comparator<GitHubAppInstallHistoryUiRecord> {
    val textComparator = compareByDescending<GitHubAppInstallHistoryUiRecord> {
        it.record.appLabel
            .ifBlank { "${it.record.owner}/${it.record.repo}" }
            .ifBlank { it.record.packageName }
            .lowercase(Locale.ROOT)
    }
    val tieBreakers =
        compareByDescending<GitHubAppInstallHistoryUiRecord> { it.record.changedAtMillis }
            .thenByDescending { it.record.trackId }
    return when (sortMode) {
        GitHubAppInstallHistorySortMode.ChangedAt ->
            compareByDescending<GitHubAppInstallHistoryUiRecord> { it.record.changedAtMillis }
                .thenByDescending { it.record.trackId }
        GitHubAppInstallHistorySortMode.App ->
            textComparator.then(tieBreakers)
        GitHubAppInstallHistorySortMode.Repository ->
            compareByDescending<GitHubAppInstallHistoryUiRecord> {
                "${it.record.owner}/${it.record.repo}".lowercase(Locale.ROOT)
            }.then(tieBreakers)
        GitHubAppInstallHistorySortMode.Version ->
            compareByDescending<GitHubAppInstallHistoryUiRecord> {
                maxOf(it.record.previousVersionCode, it.record.currentVersionCode)
            }.then(tieBreakers)
    }
}
