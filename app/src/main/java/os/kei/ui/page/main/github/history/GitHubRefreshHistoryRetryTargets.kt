package os.kei.ui.page.main.github.history

import os.kei.feature.github.model.GitHubRefreshHistoryOutcome
import os.kei.feature.github.model.GitHubRefreshHistoryRecord

internal fun GitHubRefreshHistoryRecord.refreshHistoryRetryTargetIds(): List<String> {
    if (!isRefreshHistoryRetryEligible()) return emptyList()
    val originalBatchTargetIds = targetTrackIds.normalizedRefreshRetryTrackIds()
    if (outcome == GitHubRefreshHistoryOutcome.Cancelled && originalBatchTargetIds.isNotEmpty()) {
        return originalBatchTargetIds
    }
    val failedTargetIds =
        failureSummaries
            .map { it.trackId }
            .normalizedRefreshRetryTrackIds()
    if (failedTargetIds.isNotEmpty()) return failedTargetIds
    return originalBatchTargetIds
}

internal fun GitHubRefreshHistoryRecord.isRefreshHistoryRetryEligible(): Boolean =
    outcome == GitHubRefreshHistoryOutcome.Cancelled ||
        outcome == GitHubRefreshHistoryOutcome.Failed ||
        failedCount > 0

private fun List<String>.normalizedRefreshRetryTrackIds(): List<String> =
    asSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
