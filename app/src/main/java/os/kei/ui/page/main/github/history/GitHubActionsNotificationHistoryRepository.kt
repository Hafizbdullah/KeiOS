package os.kei.ui.page.main.github.history

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import os.kei.feature.github.domain.GitHubActionsService
import os.kei.feature.github.domain.GitHubAppInstallHistoryService
import os.kei.feature.github.domain.GitHubHistoryUnreadBucket
import os.kei.feature.github.domain.GitHubHistoryUnreadCounts
import os.kei.feature.github.domain.GitHubHistoryUnreadEventTimes
import os.kei.feature.github.domain.GitHubHistoryUnreadService
import os.kei.feature.github.domain.GitHubRefreshHistoryQuery
import os.kei.feature.github.domain.GitHubRefreshHistoryService
import os.kei.feature.github.domain.GitHubTrackChangeHistoryService
import os.kei.feature.github.domain.GitHubTrackService

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

internal class GitHubActionsNotificationHistoryRepository(
    private val actionsService: GitHubActionsService = GitHubActionsService(),
    private val refreshHistoryService: GitHubRefreshHistoryService = GitHubRefreshHistoryService(),
    private val trackChangeHistoryService: GitHubTrackChangeHistoryService = GitHubTrackChangeHistoryService(),
    private val appInstallHistoryService: GitHubAppInstallHistoryService = GitHubAppInstallHistoryService(),
    private val unreadService: GitHubHistoryUnreadService = GitHubHistoryUnreadService(),
    private val trackService: GitHubTrackService = GitHubTrackService(),
    private val fileIoDispatcher: CoroutineDispatcher = AppDispatchers.fileIo,
) {
    suspend fun loadHistory(): List<GitHubActionsNotificationHistoryUiRecord> {
        val records = actionsService.loadGitHubActionsNotificationHistory()
        if (records.isEmpty()) return emptyList()
        val packageNameByTrackId =
            runCatching {
                trackService
                    .loadTrackSnapshot()
                    .items
                    .associate { item -> item.id.trim() to item.packageName.trim() }
            }.getOrDefault(emptyMap())
        return records.map { record ->
            GitHubActionsNotificationHistoryUiRecord(
                record = record,
                packageName = packageNameByTrackId[record.trackId.trim()].orEmpty(),
            )
        }
    }

    suspend fun pruneOlderThanDays(
        days: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int {
        if (days <= 0) return 0
        val cutoffMillis = nowMillis - days * MILLIS_PER_DAY
        return actionsService.pruneGitHubActionsNotificationHistoryBefore(cutoffMillis)
    }

    suspend fun loadRefreshHistory(): List<GitHubRefreshHistoryUiRecord> {
        return refreshHistoryService.loadHistory().map(::GitHubRefreshHistoryUiRecord)
    }

    suspend fun pruneRefreshHistoryOlderThanDays(
        days: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int {
        if (days <= 0) return 0
        val cutoffMillis = nowMillis - days * MILLIS_PER_DAY
        return refreshHistoryService.pruneBefore(cutoffMillis)
    }

    suspend fun loadTrackChangeHistory(): List<GitHubTrackChangeHistoryUiRecord> {
        return trackChangeHistoryService.loadHistory().map(::GitHubTrackChangeHistoryUiRecord)
    }

    suspend fun pruneTrackChangeHistoryOlderThanDays(
        days: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int {
        if (days <= 0) return 0
        val cutoffMillis = nowMillis - days * MILLIS_PER_DAY
        return trackChangeHistoryService.pruneBefore(cutoffMillis)
    }

    suspend fun loadAppInstallHistory(): List<GitHubAppInstallHistoryUiRecord> {
        return appInstallHistoryService.loadHistory().map(::GitHubAppInstallHistoryUiRecord)
    }

    suspend fun pruneAppInstallHistoryOlderThanDays(
        days: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int {
        if (days <= 0) return 0
        val cutoffMillis = nowMillis - days * MILLIS_PER_DAY
        return appInstallHistoryService.pruneBefore(cutoffMillis)
    }

    suspend fun loadUnreadCounts(): GitHubHistoryUnreadCounts = unreadService.loadCounts()

    suspend fun markHistoryModeRead(
        mode: GitHubHistoryMode,
        eventTimes: GitHubHistoryUnreadEventTimes,
    ): GitHubHistoryUnreadCounts =
        unreadService.markRead(
            bucket = mode.toUnreadBucket(),
            eventTimes = eventTimes,
        )

    suspend fun buildRefreshHistoryExportJson(
        query: GitHubRefreshHistoryQuery,
    ): String {
        return refreshHistoryService.buildExportJson(query = query)
    }

    suspend fun writeText(
        contentResolver: ContentResolver,
        uri: Uri,
        content: String,
    ) {
        withContext(fileIoDispatcher) {
            contentResolver.openOutputStream(uri)?.bufferedWriter().use { writer ->
                checkNotNull(writer) { "openOutputStream returned null" }
                writer.write(content)
            }
        }
    }
}

private fun GitHubHistoryMode.toUnreadBucket(): GitHubHistoryUnreadBucket =
    when (this) {
        GitHubHistoryMode.Refresh -> GitHubHistoryUnreadBucket.Refresh
        GitHubHistoryMode.Actions -> GitHubHistoryUnreadBucket.Actions
        GitHubHistoryMode.Tracking -> GitHubHistoryUnreadBucket.Tracking
        GitHubHistoryMode.Apps -> GitHubHistoryUnreadBucket.Apps
    }
