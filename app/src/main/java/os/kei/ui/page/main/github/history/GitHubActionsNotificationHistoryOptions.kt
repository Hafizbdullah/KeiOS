package os.kei.ui.page.main.github.history

import androidx.annotation.StringRes
import os.kei.R
import os.kei.feature.github.model.GitHubRefreshHistoryOutcome
import java.util.Locale

internal enum class GitHubHistoryMode(
    @param:StringRes val labelRes: Int,
) {
    Refresh(R.string.github_history_mode_refresh),
    Actions(R.string.github_history_mode_actions),
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

internal enum class GitHubRefreshHistorySortMode(
    @param:StringRes val labelRes: Int,
) {
    FinishedAt(R.string.github_history_refresh_sort_finished),
    Elapsed(R.string.github_history_refresh_sort_elapsed),
    TargetCount(R.string.github_history_refresh_sort_target),
    UpdateCount(R.string.github_history_refresh_sort_updates),
    FailedCount(R.string.github_history_refresh_sort_failed),
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
): List<GitHubActionsNotificationHistoryUiRecord> {
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
): List<GitHubRefreshHistoryUiRecord> {
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
        }
    val comparator = gitHubRefreshHistoryComparator(sortMode)
    val sorted = filtered.sortedWith(comparator)
    return when (sortDirection) {
        GitHubActionsHistorySortDirection.Descending -> sorted
        GitHubActionsHistorySortDirection.Ascending -> sorted.asReversed()
    }
}

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
