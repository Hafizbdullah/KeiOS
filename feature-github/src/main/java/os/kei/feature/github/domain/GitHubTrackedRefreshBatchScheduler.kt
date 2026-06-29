package os.kei.feature.github.domain

import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.isDirectApkTrack
import os.kei.feature.github.model.isFdroidRepositoryTrack
import os.kei.feature.github.model.isGitBackedRepositoryTrack

private const val DEFAULT_DIRECT_APK_REFRESH_CONCURRENCY = 2
private const val DEFAULT_FDROID_REFRESH_CONCURRENCY = 2
private const val BACKGROUND_SMALL_BATCH_REFRESH_CONCURRENCY = 2
private const val BACKGROUND_MEDIUM_BATCH_REFRESH_CONCURRENCY = 4
private const val BACKGROUND_LARGE_BATCH_REFRESH_CONCURRENCY = 6
private const val SMALL_BATCH_REFRESH_CONCURRENCY = 4
private const val MEDIUM_BATCH_REFRESH_CONCURRENCY = 6
private const val LARGE_BATCH_REFRESH_CONCURRENCY = 8
private const val MEDIUM_BATCH_THRESHOLD = 16
private const val LARGE_BATCH_THRESHOLD = 48

data class GitHubTrackedRefreshWorkItem(
    val originalIndex: Int,
    val item: GitHubTrackedApp
)

object GitHubTrackedRefreshBatchScheduler {
    fun buildFairRefreshOrder(
        trackedItems: List<GitHubTrackedApp>
    ): List<GitHubTrackedRefreshWorkItem> {
        if (trackedItems.size <= 1) {
            return trackedItems.mapIndexed { index, item ->
                GitHubTrackedRefreshWorkItem(index, item)
            }
        }
        val repositoryItems = ArrayDeque<GitHubTrackedRefreshWorkItem>()
        val directApkItems = ArrayDeque<GitHubTrackedRefreshWorkItem>()
        val fdroidItems = ArrayDeque<GitHubTrackedRefreshWorkItem>()
        val otherItems = ArrayDeque<GitHubTrackedRefreshWorkItem>()
        trackedItems.forEachIndexed { index, item ->
            val workItem = GitHubTrackedRefreshWorkItem(index, item)
            when {
                item.isGitBackedRepositoryTrack() -> repositoryItems += workItem
                item.isDirectApkTrack() -> directApkItems += workItem
                item.isFdroidRepositoryTrack() -> fdroidItems += workItem
                else -> otherItems += workItem
            }
        }
        return buildRoundRobinSourceOrder(
            repositoryItems = repositoryItems,
            directApkItems = directApkItems,
            fdroidItems = fdroidItems,
            otherItems = otherItems
        )
    }

    fun directApkConcurrency(maxConcurrency: Int): Int {
        return maxConcurrency
            .coerceAtLeast(1)
            .coerceAtMost(DEFAULT_DIRECT_APK_REFRESH_CONCURRENCY)
    }

    fun fdroidConcurrency(maxConcurrency: Int): Int {
        return maxConcurrency
            .coerceAtLeast(1)
            .coerceAtMost(DEFAULT_FDROID_REFRESH_CONCURRENCY)
    }

    fun refreshConcurrency(itemCount: Int): Int {
        if (itemCount <= 0) return 1
        val target = when {
            itemCount >= LARGE_BATCH_THRESHOLD -> LARGE_BATCH_REFRESH_CONCURRENCY
            itemCount >= MEDIUM_BATCH_THRESHOLD -> MEDIUM_BATCH_REFRESH_CONCURRENCY
            else -> SMALL_BATCH_REFRESH_CONCURRENCY
        }
        return target.coerceAtMost(itemCount)
    }

    fun backgroundRefreshConcurrency(itemCount: Int): Int {
        if (itemCount <= 0) return 1
        val target = when {
            itemCount >= LARGE_BATCH_THRESHOLD -> BACKGROUND_LARGE_BATCH_REFRESH_CONCURRENCY
            itemCount >= MEDIUM_BATCH_THRESHOLD -> BACKGROUND_MEDIUM_BATCH_REFRESH_CONCURRENCY
            else -> BACKGROUND_SMALL_BATCH_REFRESH_CONCURRENCY
        }
        return target.coerceAtMost(itemCount)
    }

    private fun <T> buildRoundRobinSourceOrder(
        repositoryItems: ArrayDeque<T>,
        directApkItems: ArrayDeque<T>,
        fdroidItems: ArrayDeque<T>,
        otherItems: ArrayDeque<T>
    ): List<T> {
        return buildList(
            repositoryItems.size + directApkItems.size + fdroidItems.size + otherItems.size
        ) {
            val lanes = listOf(repositoryItems, directApkItems, fdroidItems)
            var laneIndex = 0
            while (lanes.any { lane -> lane.isNotEmpty() }) {
                val selectedLane =
                    lanes.indices
                        .map { offset -> (laneIndex + offset) % lanes.size }
                        .first { index -> lanes[index].isNotEmpty() }
                add(lanes[selectedLane].removeFirst())
                laneIndex = (selectedLane + 1) % lanes.size
            }
            while (otherItems.isNotEmpty()) {
                add(otherItems.removeFirst())
            }
        }
    }
}
