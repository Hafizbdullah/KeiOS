package os.kei.feature.github.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import os.kei.feature.github.model.GitHubCheckCacheEntry

internal const val GITHUB_BACKGROUND_CHECKPOINT_ITEM_THRESHOLD = 6
internal const val GITHUB_BACKGROUND_CHECKPOINT_INTERVAL_MS = 2_000L

internal class GitHubBackgroundRefreshCheckpointWriter(
    private val persist: suspend (Map<String, GitHubCheckCacheEntry>) -> Unit,
    private val itemThreshold: Int = GITHUB_BACKGROUND_CHECKPOINT_ITEM_THRESHOLD,
    private val intervalMs: Long = GITHUB_BACKGROUND_CHECKPOINT_INTERVAL_MS,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val pending = LinkedHashMap<String, GitHubCheckCacheEntry>()
    private var lastFlushAtMillis = nowMs()

    suspend fun append(
        trackId: String,
        entry: GitHubCheckCacheEntry,
    ) {
        mutex.withLock {
            pending[trackId] = entry
            val elapsedMs = (nowMs() - lastFlushAtMillis).coerceAtLeast(0L)
            if (pending.size >= itemThreshold.coerceAtLeast(1) || elapsedMs >= intervalMs.coerceAtLeast(0L)) {
                flushLocked()
            }
        }
    }

    suspend fun flush() {
        mutex.withLock {
            flushLocked()
        }
    }

    private suspend fun flushLocked() {
        if (pending.isEmpty()) return
        val entries = pending.toMap()
        persist(entries)
        entries.keys.forEach(pending::remove)
        lastFlushAtMillis = nowMs()
    }
}
