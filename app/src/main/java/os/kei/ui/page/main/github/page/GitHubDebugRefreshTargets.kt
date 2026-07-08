package os.kei.ui.page.main.github.page

import os.kei.feature.github.model.GitHubTrackedApp

internal const val GitHubDebugVisibleRefreshDefaultLimit = 4

internal fun selectGitHubDebugVisibleRefreshTargets(
    visibleItems: List<GitHubTrackedApp>,
    limit: Int = GitHubDebugVisibleRefreshDefaultLimit,
): List<GitHubTrackedApp> {
    if (limit <= 0) return emptyList()
    val selected = ArrayList<GitHubTrackedApp>(limit)
    val seenIds = HashSet<String>()
    for (item in visibleItems) {
        if (selected.size >= limit) break
        val id = item.id.trim()
        if (id.isBlank() || item.packageName.isBlank()) continue
        if (seenIds.add(id)) {
            selected += item
        }
    }
    return selected
}
