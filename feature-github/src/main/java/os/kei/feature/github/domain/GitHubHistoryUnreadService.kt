package os.kei.feature.github.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.feature.github.data.local.GitHubActionsNotificationHistoryStore
import os.kei.feature.github.data.local.GitHubAppInstallHistoryStore
import os.kei.feature.github.data.local.GitHubHistoryUnreadStore
import os.kei.feature.github.data.local.GitHubRefreshHistoryStore
import os.kei.feature.github.data.local.GitHubTrackChangeHistoryStore

enum class GitHubHistoryUnreadBucket {
    Refresh,
    Actions,
    Tracking,
    Apps,
}

data class GitHubHistoryUnreadWatermarks(
    val refreshReadAtMillis: Long = 0L,
    val actionsReadAtMillis: Long = 0L,
    val trackingReadAtMillis: Long = 0L,
    val appsReadAtMillis: Long = 0L,
)

data class GitHubHistoryUnreadEventTimes(
    val refreshTimes: List<Long> = emptyList(),
    val actionTimes: List<Long> = emptyList(),
    val trackingTimes: List<Long> = emptyList(),
    val appTimes: List<Long> = emptyList(),
) {
    fun latestAt(bucket: GitHubHistoryUnreadBucket): Long =
        when (bucket) {
            GitHubHistoryUnreadBucket.Refresh -> refreshTimes
            GitHubHistoryUnreadBucket.Actions -> actionTimes
            GitHubHistoryUnreadBucket.Tracking -> trackingTimes
            GitHubHistoryUnreadBucket.Apps -> appTimes
        }.maxOrNull() ?: 0L
}

data class GitHubHistoryUnreadCounts(
    val refreshCount: Int = 0,
    val actionsCount: Int = 0,
    val trackingCount: Int = 0,
    val appsCount: Int = 0,
) {
    val totalCount: Int
        get() = refreshCount + actionsCount + trackingCount + appsCount
}

object GitHubHistoryUnreadCounter {
    fun buildCounts(
        eventTimes: GitHubHistoryUnreadEventTimes,
        watermarks: GitHubHistoryUnreadWatermarks,
    ): GitHubHistoryUnreadCounts =
        GitHubHistoryUnreadCounts(
            refreshCount =
                countUnread(
                    times = eventTimes.refreshTimes,
                    readAtMillis = watermarks.refreshReadAtMillis,
                ),
            actionsCount =
                countUnread(
                    times = eventTimes.actionTimes,
                    readAtMillis = watermarks.actionsReadAtMillis,
                ),
            trackingCount =
                countUnread(
                    times = eventTimes.trackingTimes,
                    readAtMillis = watermarks.trackingReadAtMillis,
                ),
            appsCount =
                countUnread(
                    times = eventTimes.appTimes,
                    readAtMillis = watermarks.appsReadAtMillis,
                ),
        )

    private fun countUnread(
        times: List<Long>,
        readAtMillis: Long,
    ): Int {
        val normalizedReadAtMillis = readAtMillis.coerceAtLeast(0L)
        return times.count { time -> time > normalizedReadAtMillis }
    }
}

class GitHubHistoryUnreadService(
    private val localDispatcher: CoroutineDispatcher = AppDispatchers.githubLocal,
) {
    suspend fun loadCounts(): GitHubHistoryUnreadCounts =
        withContext(localDispatcher) {
            GitHubHistoryUnreadCounter.buildCounts(
                eventTimes = loadEventTimes(),
                watermarks = GitHubHistoryUnreadStore.loadWatermarks(),
            )
        }

    suspend fun markRead(
        bucket: GitHubHistoryUnreadBucket,
        eventTimes: GitHubHistoryUnreadEventTimes? = null,
    ): GitHubHistoryUnreadCounts =
        withContext(localDispatcher) {
            val resolvedEventTimes = eventTimes ?: loadEventTimes()
            val latestSeenAtMillis = resolvedEventTimes.latestAt(bucket)
            val watermarks =
                GitHubHistoryUnreadStore.markRead(
                    bucket = bucket,
                    latestSeenAtMillis = latestSeenAtMillis,
                )
            GitHubHistoryUnreadCounter.buildCounts(
                eventTimes = resolvedEventTimes,
                watermarks = watermarks,
            )
        }

    private fun loadEventTimes(): GitHubHistoryUnreadEventTimes =
        GitHubHistoryUnreadEventTimes(
            refreshTimes =
                GitHubRefreshHistoryStore.load()
                    .map { record ->
                        record.finishedAtMillis.takeIf { it > 0L } ?: record.startedAtMillis
                    },
            actionTimes =
                GitHubActionsNotificationHistoryStore.load()
                    .map { record -> record.notifiedAtMillis },
            trackingTimes =
                GitHubTrackChangeHistoryStore.load()
                    .map { record -> record.changedAtMillis },
            appTimes =
                GitHubAppInstallHistoryStore.load()
                    .map { record -> record.changedAtMillis },
        )
}
