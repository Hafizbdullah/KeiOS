package os.kei.feature.github.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import os.kei.core.concurrency.AppDispatchers
import os.kei.feature.github.model.GitHubActionsRecommendedRunSnapshot
import os.kei.feature.github.model.GitHubLookupConfig
import os.kei.feature.github.model.GitHubTrackedApp
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext

interface GitHubActionsRecommendedRunRefreshSource {
    fun loadRecommendedRunSnapshot(trackId: String): GitHubActionsRecommendedRunSnapshot?

    fun loadRecommendedRunSnapshots(): Map<String, GitHubActionsRecommendedRunSnapshot>

    suspend fun fetchRecommendedRunSnapshot(
        item: GitHubTrackedApp,
        lookupConfig: GitHubLookupConfig,
        previousWorkflowId: Long?,
        nowMs: Long = System.currentTimeMillis(),
    ): Result<GitHubActionsRecommendedRunSnapshot>

    fun saveRecommendedRunSnapshot(snapshot: GitHubActionsRecommendedRunSnapshot)

    fun removeRecommendedRunSnapshot(trackId: String)

    fun retainRecommendedRunSnapshots(trackIds: Set<String>)
}

data class GitHubActionsRecommendedRunRefreshOutcome(
    val item: GitHubTrackedApp,
    val previous: GitHubActionsRecommendedRunSnapshot?,
    val current: GitHubActionsRecommendedRunSnapshot?,
    val errorMessage: String = "",
) {
    val succeeded: Boolean
        get() = current != null

    val newerThanPrevious: Boolean
        get() = previous != null && current?.isNewerThan(previous) == true
}

data class GitHubActionsRecommendedRunRefreshResult(
    val outcomes: List<GitHubActionsRecommendedRunRefreshOutcome>,
) {
    val checkedCount: Int
        get() = outcomes.size

    val succeededCount: Int
        get() = outcomes.count { it.succeeded }

    val failedCount: Int
        get() = outcomes.count { !it.succeeded }

    val newerSnapshots: List<GitHubActionsRecommendedRunSnapshot>
        get() = outcomes.mapNotNull { outcome ->
            outcome.current?.takeIf { outcome.newerThanPrevious }
        }
}

class GitHubActionsRecommendedRunRefreshService(
    private val source: GitHubActionsRecommendedRunRefreshSource = GitHubActionsService(),
    private val networkDispatcher: CoroutineDispatcher = AppDispatchers.githubNetwork,
    private val localDispatcher: CoroutineDispatcher = AppDispatchers.githubLocal,
) {
    suspend fun refreshItems(
        items: List<GitHubTrackedApp>,
        lookupConfig: GitHubLookupConfig,
        maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY,
        retainTrackIds: Set<String>? = null,
        nowMs: Long = System.currentTimeMillis(),
        itemTimeoutMs: Long = DEFAULT_ITEM_TIMEOUT_MS,
        batchTimeoutMs: Long = DEFAULT_BATCH_TIMEOUT_MS,
    ): GitHubActionsRecommendedRunRefreshResult {
        retainTrackIds?.let { trackIds ->
            withContext(localDispatcher) {
                source.retainRecommendedRunSnapshots(trackIds)
            }
        }
        val targets = items.filter { it.checkActionsUpdates }
        if (targets.isEmpty()) {
            return GitHubActionsRecommendedRunRefreshResult(emptyList())
        }
        val outcomes = refreshTargets(
            targets = targets,
            lookupConfig = lookupConfig,
            maxConcurrency = maxConcurrency,
            nowMs = nowMs,
            itemTimeoutMs = itemTimeoutMs,
            batchTimeoutMs = batchTimeoutMs,
        )
        return GitHubActionsRecommendedRunRefreshResult(outcomes)
    }

    private suspend fun refreshTargets(
        targets: List<GitHubTrackedApp>,
        lookupConfig: GitHubLookupConfig,
        maxConcurrency: Int,
        nowMs: Long,
        itemTimeoutMs: Long,
        batchTimeoutMs: Long,
    ): List<GitHubActionsRecommendedRunRefreshOutcome> {
        val concurrency = targets.size.coerceAtMost(maxConcurrency.coerceAtLeast(1))
        val nextIndex = AtomicInteger(0)
        val batchStartNs = System.nanoTime()
        val deadlineNs = batchDeadlineNs(batchStartNs, batchTimeoutMs)
        val batchTimedOut = AtomicBoolean(false)
        val results = arrayOfNulls<GitHubActionsRecommendedRunRefreshOutcome>(targets.size)
        coroutineScope {
            List(concurrency) {
                async(networkDispatcher) {
                    while (true) {
                        if (isBatchDeadlineReached(deadlineNs)) {
                            batchTimedOut.set(true)
                            break
                        }
                        val index = nextIndex.getAndIncrement()
                        if (index >= targets.size) break
                        coroutineContext.ensureActive()
                        val item = targets[index]
                        results[index] = refreshItemWithTimeout(
                            item = item,
                            lookupConfig = lookupConfig,
                            nowMs = nowMs,
                            timeoutMs = itemTimeoutMs,
                        )
                        yield()
                    }
                }
            }.awaitAll()
        }
        results.forEachIndexed { index, outcome ->
            if (outcome == null) {
                val item = targets[index]
                val previous =
                    withContext(localDispatcher) {
                        source.loadRecommendedRunSnapshot(item.id)
                    }
                results[index] =
                    skippedOutcome(
                        item = item,
                        previous = previous,
                        errorMessage =
                            if (batchTimedOut.get() && batchTimeoutMs > 0L) {
                                "Actions refresh batch timed out after ${timeoutSeconds(batchTimeoutMs)}s before ${item.owner}/${item.repo} could refresh"
                            } else {
                                "Actions refresh stopped before ${item.owner}/${item.repo} could refresh"
                            },
                    )
            }
        }
        return results.map { outcome ->
            checkNotNull(outcome) { "Actions refresh outcome was not produced" }
        }
    }

    private suspend fun refreshItemWithTimeout(
        item: GitHubTrackedApp,
        lookupConfig: GitHubLookupConfig,
        nowMs: Long,
        timeoutMs: Long,
    ): GitHubActionsRecommendedRunRefreshOutcome {
        val previous =
            withContext(localDispatcher) {
                source.loadRecommendedRunSnapshot(item.id)
            }
        val boundedTimeoutMs = timeoutMs.coerceAtLeast(0L)
        if (boundedTimeoutMs == 0L) {
            return refreshItem(
                item = item,
                lookupConfig = lookupConfig,
                nowMs = nowMs,
                previous = previous,
            )
        }
        return withTimeoutOrNull(boundedTimeoutMs) {
            refreshItem(
                item = item,
                lookupConfig = lookupConfig,
                nowMs = nowMs,
                previous = previous,
            )
        } ?: skippedOutcome(
            item = item,
            previous = previous,
            errorMessage = "Actions refresh timed out after ${timeoutSeconds(boundedTimeoutMs)}s",
        )
    }

    private suspend fun refreshItem(
        item: GitHubTrackedApp,
        lookupConfig: GitHubLookupConfig,
        nowMs: Long,
        previous: GitHubActionsRecommendedRunSnapshot?,
    ): GitHubActionsRecommendedRunRefreshOutcome {
        return source
            .fetchRecommendedRunSnapshot(
                item = item,
                lookupConfig = lookupConfig,
                previousWorkflowId = previous?.workflowId,
                nowMs = nowMs,
            ).fold(
                onSuccess = { current ->
                    withContext(localDispatcher) {
                        source.saveRecommendedRunSnapshot(current)
                    }
                    GitHubActionsRecommendedRunRefreshOutcome(
                        item = item,
                        previous = previous,
                        current = current,
                    )
                },
                onFailure = { error ->
                    GitHubActionsRecommendedRunRefreshOutcome(
                        item = item,
                        previous = previous,
                        current = null,
                        errorMessage = error.message.orEmpty().ifBlank { error.javaClass.simpleName },
                    )
                },
            )
    }

    companion object {
        const val DEFAULT_MAX_CONCURRENCY = 2
        private const val DEFAULT_ITEM_TIMEOUT_MS = 25_000L
        private const val DEFAULT_BATCH_TIMEOUT_MS = 2L * 60L * 1000L
    }
}

private fun skippedOutcome(
    item: GitHubTrackedApp,
    previous: GitHubActionsRecommendedRunSnapshot? = null,
    errorMessage: String,
): GitHubActionsRecommendedRunRefreshOutcome =
    GitHubActionsRecommendedRunRefreshOutcome(
        item = item,
        previous = previous,
        current = null,
        errorMessage = errorMessage,
    )

private fun batchDeadlineNs(
    batchStartNs: Long,
    batchTimeoutMs: Long,
): Long {
    if (batchTimeoutMs <= 0L) return Long.MAX_VALUE
    val timeoutNs = batchTimeoutMs.saturatingMsToNs()
    if (batchStartNs > Long.MAX_VALUE - timeoutNs) return Long.MAX_VALUE
    return batchStartNs + timeoutNs
}

private fun isBatchDeadlineReached(deadlineNs: Long): Boolean =
    deadlineNs != Long.MAX_VALUE && System.nanoTime() >= deadlineNs

private fun timeoutSeconds(timeoutMs: Long): Long =
    ((timeoutMs + 999L) / 1_000L).coerceAtLeast(1L)

private fun Long.saturatingMsToNs(): Long {
    val value = coerceAtLeast(0L)
    val multiplier = 1_000_000L
    if (value > Long.MAX_VALUE / multiplier) return Long.MAX_VALUE
    return value * multiplier
}
