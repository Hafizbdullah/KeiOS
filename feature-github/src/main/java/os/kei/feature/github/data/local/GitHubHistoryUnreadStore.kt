package os.kei.feature.github.data.local

import com.tencent.mmkv.MMKV
import os.kei.core.prefs.KeiMmkv
import os.kei.feature.github.domain.GitHubHistoryUnreadBucket
import os.kei.feature.github.domain.GitHubHistoryUnreadWatermarks

object GitHubHistoryUnreadStore {
    private const val KV_ID = "github_history_unread"
    private const val KEY_REFRESH_READ_AT = "refresh_read_at"
    private const val KEY_ACTIONS_READ_AT = "actions_read_at"
    private const val KEY_TRACKING_READ_AT = "tracking_read_at"
    private const val KEY_APPS_READ_AT = "apps_read_at"

    private val store: MMKV by lazy { KeiMmkv.byId(KV_ID) }

    private fun kv(): MMKV = store

    fun loadWatermarks(): GitHubHistoryUnreadWatermarks {
        val kv = kv()
        return GitHubHistoryUnreadWatermarks(
            refreshReadAtMillis = kv.decodeLong(KEY_REFRESH_READ_AT, 0L).coerceAtLeast(0L),
            actionsReadAtMillis = kv.decodeLong(KEY_ACTIONS_READ_AT, 0L).coerceAtLeast(0L),
            trackingReadAtMillis = kv.decodeLong(KEY_TRACKING_READ_AT, 0L).coerceAtLeast(0L),
            appsReadAtMillis = kv.decodeLong(KEY_APPS_READ_AT, 0L).coerceAtLeast(0L),
        )
    }

    fun markRead(
        bucket: GitHubHistoryUnreadBucket,
        latestSeenAtMillis: Long,
    ): GitHubHistoryUnreadWatermarks {
        if (latestSeenAtMillis <= 0L) return loadWatermarks()
        val previous = loadWatermarks()
        val next =
            when (bucket) {
                GitHubHistoryUnreadBucket.Refresh ->
                    previous.copy(refreshReadAtMillis = maxOf(previous.refreshReadAtMillis, latestSeenAtMillis))
                GitHubHistoryUnreadBucket.Actions ->
                    previous.copy(actionsReadAtMillis = maxOf(previous.actionsReadAtMillis, latestSeenAtMillis))
                GitHubHistoryUnreadBucket.Tracking ->
                    previous.copy(trackingReadAtMillis = maxOf(previous.trackingReadAtMillis, latestSeenAtMillis))
                GitHubHistoryUnreadBucket.Apps ->
                    previous.copy(appsReadAtMillis = maxOf(previous.appsReadAtMillis, latestSeenAtMillis))
            }
        if (next == previous) return previous
        saveWatermarks(next)
        GitHubHistoryUnreadStoreSignals.notifyChanged()
        return next
    }

    fun clear() {
        val previous = loadWatermarks()
        val kv = kv()
        kv.removeValuesForKeys(
            arrayOf(
                KEY_REFRESH_READ_AT,
                KEY_ACTIONS_READ_AT,
                KEY_TRACKING_READ_AT,
                KEY_APPS_READ_AT,
            ),
        )
        kv.trim()
        if (previous != GitHubHistoryUnreadWatermarks()) {
            GitHubHistoryUnreadStoreSignals.notifyChanged()
        }
    }

    private fun saveWatermarks(watermarks: GitHubHistoryUnreadWatermarks) {
        val kv = kv()
        kv.encode(KEY_REFRESH_READ_AT, watermarks.refreshReadAtMillis.coerceAtLeast(0L))
        kv.encode(KEY_ACTIONS_READ_AT, watermarks.actionsReadAtMillis.coerceAtLeast(0L))
        kv.encode(KEY_TRACKING_READ_AT, watermarks.trackingReadAtMillis.coerceAtLeast(0L))
        kv.encode(KEY_APPS_READ_AT, watermarks.appsReadAtMillis.coerceAtLeast(0L))
    }
}
