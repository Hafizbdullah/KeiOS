package os.kei.feature.github.domain

import os.kei.feature.github.data.local.GitHubTrackSnapshot
import os.kei.feature.github.model.GitHubActionsRecommendedRunSnapshot
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.actionsUpdateIntervalMs
import os.kei.feature.github.model.excludesAutomaticReleaseRefresh
import os.kei.feature.github.model.updateIntervalMs

internal const val GITHUB_BACKGROUND_RELEASE_TARGET_LIMIT = 4
internal const val GITHUB_BACKGROUND_ACTIONS_TARGET_LIMIT = 4

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
            val due =
                checkedAtMillis <= 0L ||
                    (nowMs - checkedAtMillis).coerceAtLeast(0L) >=
                    item.updateIntervalMs(snapshot.refreshIntervalHours)
            if (due) item to checkedAtMillis else null
        }
        .oldestCheckedFirst()
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
            val due =
                checkedAtMillis <= 0L ||
                    (nowMs - checkedAtMillis).coerceAtLeast(0L) >=
                    item.actionsUpdateIntervalMs(refreshIntervalHours)
            if (due) item to checkedAtMillis else null
        }
        .toList()
        .oldestCheckedFirst()
        .take(maxTargets.coerceAtLeast(0))
}

private fun List<Pair<GitHubTrackedApp, Long>>.oldestCheckedFirst(): List<GitHubTrackedApp> =
    sortedBy { (_, checkedAtMillis) ->
        checkedAtMillis.takeIf { it > 0L } ?: Long.MIN_VALUE
    }.map { (item, _) -> item }
