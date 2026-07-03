package os.kei.feature.github.model

import os.kei.feature.github.domain.GitHubRefreshScope
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.github.domain.GitHubTrackedRefreshFailure
import os.kei.feature.github.domain.GitHubTrackedRefreshSlowItem

enum class GitHubRefreshHistoryOutcome {
    Completed,
    Cancelled,
    Failed,
}

data class GitHubRefreshHistoryFailureSummary(
    val trackId: String,
    val owner: String,
    val repo: String,
    val packageName: String,
    val appLabel: String,
    val sourceMode: String,
    val message: String,
    val elapsedMs: Long = 0L,
)

data class GitHubRefreshHistorySlowItem(
    val trackId: String,
    val owner: String,
    val repo: String,
    val packageName: String,
    val appLabel: String,
    val sourceMode: String,
    val elapsedMs: Long,
    val status: String,
    val message: String,
    val strategyId: String = "",
    val snapshotElapsedMs: Long = 0L,
    val snapshotFromCache: Boolean = false,
    val profileElapsedMs: Long = 0L,
    val profileFromCache: Boolean = false,
    val preciseApkElapsedMs: Long = 0L,
    val preciseApkRequested: Boolean = false,
    val fallbackStrategyId: String = "",
)

data class GitHubRefreshHistoryRecord(
    val id: String,
    val sessionId: Long,
    val scope: GitHubRefreshScope,
    val source: GitHubRefreshSource,
    val outcome: GitHubRefreshHistoryOutcome,
    val totalTrackedCount: Int,
    val targetCount: Int,
    val completedCount: Int,
    val updatableCount: Int,
    val preReleaseUpdateCount: Int,
    val failedCount: Int,
    val startedAtMillis: Long,
    val finishedAtMillis: Long,
    val elapsedMs: Long,
    val p50ItemMs: Long = 0L,
    val p95ItemMs: Long = 0L,
    val maxItemMs: Long = 0L,
    val maxConcurrency: Int = 0,
    val directApkConcurrency: Int = 0,
    val fdroidConcurrency: Int = 0,
    val repositoryItemCount: Int = 0,
    val directApkItemCount: Int = 0,
    val fdroidItemCount: Int = 0,
    val otherItemCount: Int = 0,
    val slowItems: List<GitHubRefreshHistorySlowItem> = emptyList(),
    val failureSummaries: List<GitHubRefreshHistoryFailureSummary> = emptyList(),
    val note: String = "",
)

fun GitHubTrackedRefreshFailure.toGitHubRefreshHistoryFailureSummary(): GitHubRefreshHistoryFailureSummary =
    GitHubRefreshHistoryFailureSummary(
        trackId = trackId,
        owner = owner,
        repo = repo,
        packageName = packageName,
        appLabel = appLabel,
        sourceMode = sourceMode.storageId,
        message = message,
        elapsedMs = elapsedMs,
    )

fun GitHubTrackedRefreshSlowItem.toGitHubRefreshHistorySlowItem(): GitHubRefreshHistorySlowItem =
    GitHubRefreshHistorySlowItem(
        trackId = trackId,
        owner = owner,
        repo = repo,
        packageName = packageName,
        appLabel = appLabel,
        sourceMode = sourceMode,
        elapsedMs = elapsedMs,
        status = status,
        message = message,
        strategyId = strategyId,
        snapshotElapsedMs = snapshotElapsedMs,
        snapshotFromCache = snapshotFromCache,
        profileElapsedMs = profileElapsedMs,
        profileFromCache = profileFromCache,
        preciseApkElapsedMs = preciseApkElapsedMs,
        preciseApkRequested = preciseApkRequested,
        fallbackStrategyId = fallbackStrategyId,
    )
