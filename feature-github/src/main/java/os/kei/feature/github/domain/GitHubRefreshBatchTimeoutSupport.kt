package os.kei.feature.github.domain

import kotlinx.coroutines.withTimeoutOrNull

internal suspend fun runGitHubRefreshWorkersWithBatchTimeout(
    batchTimeoutMs: Long,
    block: suspend () -> Unit,
): Boolean {
    val boundedTimeoutMs = batchTimeoutMs.coerceAtLeast(0L)
    if (boundedTimeoutMs == 0L) {
        block()
        return true
    }
    return withTimeoutOrNull(boundedTimeoutMs) {
        block()
        true
    } == true
}
