package os.kei.feature.github.domain

import os.kei.feature.github.data.local.GitHubTrackSnapshot
import os.kei.feature.github.model.GitHubActionsRecommendedRunSnapshot
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.actionsUpdateIntervalMs
import os.kei.feature.github.model.excludesAutomaticReleaseRefresh
import os.kei.feature.github.model.updateIntervalMs

internal const val GITHUB_BACKGROUND_RELEASE_TARGET_LIMIT = 96
internal const val GITHUB_BACKGROUND_ACTIONS_TARGET_LIMIT = 64

private const val GITHUB_BACKGROUND_MIN_COALESCE_WINDOW_MS = 10L * 60L * 1000L
private const val GITHUB_BACKGROUND_MAX_COALESCE_WINDOW_MS = 60L * 60L * 1000L

internal fun selectGitHubBackgroundReleaseTargets(
    snapshot: GitHubTrackSnapshot,
    nowMs: Long,
    maxTargets: Int = GITHUB_BACKGROUND_RELEASE_TARGET_LIMIT,
): List<GitHubTrackedApp> {
    if (snapshot.items.isEmpty()) return emptyList()
    return snapshot.items
        .mapNotNull { item ->
            if (item.excludesAutomaticReleaseRefresh()) return@mapNotNull null
            val checkedAtMillis =
                snapshot.checkCache[item.id]?.checkedAtMillis
                    ?.takeIf { it > 0L }
                    ?: snapshot.lastRefreshMs
            val intervalMs = item.updateIntervalMs(snapshot.refreshIntervalHours)
            val dueAtMillis = backgroundDueAtMillis(
                checkedAtMillis = checkedAtMillis,
                intervalMs = intervalMs,
            )
            if (
                backgroundCandidateInCoalescedWindow(
                    dueAtMillis = dueAtMillis,
                    intervalMs = intervalMs,
                    nowMs = nowMs,
                )
            ) {
                GitHubBackgroundRefreshTargetCandidate(
                    item = item,
                    checkedAtMillis = checkedAtMillis,
                    dueAtMillis = dueAtMillis,
                )
            } else {
                null
            }
        }
        .dueFirst()
        .take(maxTargets.coerceAtLeast(0))
}

internal fun selectGitHubBackgroundActionsTargets(
    items: List<GitHubTrackedApp>,
    previousById: Map<String, GitHubActionsRecommendedRunSnapshot>,
    refreshIntervalHours: Int,
    nowMs: Long,
    maxTargets: Int = GITHUB_BACKGROUND_ACTIONS_TARGET_LIMIT,
): List<GitHubTrackedApp> {
    return items
        .asSequence()
        .filter { item -> item.checkActionsUpdates }
        .mapNotNull { item ->
            val checkedAtMillis = previousById[item.id]?.checkedAtMillis ?: 0L
            val intervalMs = item.actionsUpdateIntervalMs(refreshIntervalHours)
            val dueAtMillis = backgroundDueAtMillis(
                checkedAtMillis = checkedAtMillis,
                intervalMs = intervalMs,
            )
            if (
                backgroundCandidateInCoalescedWindow(
                    dueAtMillis = dueAtMillis,
                    intervalMs = intervalMs,
                    nowMs = nowMs,
                )
            ) {
                GitHubBackgroundRefreshTargetCandidate(
                    item = item,
                    checkedAtMillis = checkedAtMillis,
                    dueAtMillis = dueAtMillis,
                )
            } else {
                null
            }
        }
        .toList()
        .dueFirst()
        .take(maxTargets.coerceAtLeast(0))
}

private data class GitHubBackgroundRefreshTargetCandidate(
    val item: GitHubTrackedApp,
    val checkedAtMillis: Long,
    val dueAtMillis: Long,
)

private fun backgroundDueAtMillis(
    checkedAtMillis: Long,
    intervalMs: Long,
): Long {
    if (checkedAtMillis <= 0L) return Long.MIN_VALUE
    return checkedAtMillis.saturatingPlus(intervalMs.coerceAtLeast(1L))
}

private fun backgroundCandidateInCoalescedWindow(
    dueAtMillis: Long,
    intervalMs: Long,
    nowMs: Long,
): Boolean {
    if (dueAtMillis <= nowMs) return true
    val coalesceWindowMs = backgroundCoalesceWindowMs(intervalMs)
    return dueAtMillis <= nowMs.saturatingPlus(coalesceWindowMs)
}

private fun backgroundCoalesceWindowMs(intervalMs: Long): Long =
    (intervalMs.coerceAtLeast(1L) / 3L)
        .coerceIn(
            GITHUB_BACKGROUND_MIN_COALESCE_WINDOW_MS,
            GITHUB_BACKGROUND_MAX_COALESCE_WINDOW_MS,
        )

private fun List<GitHubBackgroundRefreshTargetCandidate>.dueFirst(): List<GitHubTrackedApp> =
    sortedWith(
        compareBy<GitHubBackgroundRefreshTargetCandidate> { it.dueAtMillis }
            .thenBy { it.checkedAtMillis.takeIf { value -> value > 0L } ?: Long.MIN_VALUE }
    ).map { candidate -> candidate.item }

private fun Long.saturatingPlus(value: Long): Long {
    val increment = value.coerceAtLeast(0L)
    if (this > Long.MAX_VALUE - increment) return Long.MAX_VALUE
    return this + increment
}
