package os.kei.ui.page.main.github.page.action

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.feature.github.domain.GitHubRefreshBeginPolicy
import os.kei.feature.github.domain.GitHubRefreshHistoryService
import os.kei.feature.github.domain.GitHubRefreshRuntimeStore
import os.kei.feature.github.domain.GitHubRefreshScope
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.github.domain.GitHubTrackedRefreshFailure
import os.kei.feature.github.domain.GitHubTrackedRefreshBatchScheduler
import os.kei.feature.github.domain.GitHubTrackedRefreshBatchEvaluator
import os.kei.feature.github.domain.GitHubTrackedRefreshBatchRunner
import os.kei.feature.github.domain.GitHubTrackedRefreshPlanner
import os.kei.feature.github.model.GitHubRefreshHistoryOutcome
import os.kei.feature.github.model.GitHubRepositoryProfilePurpose
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.feature.github.model.GitHubTrackedReleaseCheck
import os.kei.ui.page.main.github.OverviewRefreshState
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.state.toUi
import java.util.concurrent.ConcurrentHashMap

private const val GITHUB_MISSING_CHECK_STATE_REFRESH_PARALLELISM = 4

internal data class GitHubItemRefreshRequest(
    val item: GitHubTrackedApp,
    val forceRefresh: Boolean = false,
    val profilePurposeOverride: GitHubRepositoryProfilePurpose? = null,
    val batchEvaluator: GitHubTrackedRefreshBatchEvaluator? = null
)

internal class GitHubBackgroundRefreshCoordinator(
    private val env: GitHubPageActionEnvironment,
    private val actionsRunRefreshCoordinator: GitHubActionsRecommendedRunRefreshCoordinator,
    private val prepareItemRefresh: suspend (GitHubTrackedApp, VersionCheckUi) -> Unit,
    private val evaluateItemCheck: suspend (GitHubItemRefreshRequest) -> GitHubTrackedReleaseCheck,
    private val mergeResolvedState: (GitHubTrackedApp, VersionCheckUi, VersionCheckUi) -> VersionCheckUi,
    private val persistCheckCache: suspend (Set<String>) -> Unit
) {
    private val context get() = env.context
    private val scope get() = env.scope
    private val state get() = env.state
    private val repository get() = env.repository
    private val clock get() = env.clock
    private val backgroundJobs = ConcurrentHashMap.newKeySet<Job>()
    private val refreshHistoryService = GitHubRefreshHistoryService()

    fun cancel() {
        backgroundJobs.forEach { it.cancel() }
        backgroundJobs.clear()
    }

    fun hasActiveJobs(): Boolean = backgroundJobs.any { it.isActive }

    suspend fun refreshRequestedTracksIfNeeded(): Boolean {
        val activeItems = state.trackedItems.toList()
        val validTrackIds = activeItems.mapTo(LinkedHashSet()) { it.id }
        if (validTrackIds.isEmpty()) return false
        val requestedIds = repository.consumeTrackRefreshRequests(validTrackIds)
        if (requestedIds.isEmpty()) return false
        val requestedItems = selectActiveTrackedRefreshTargets(
            requestedTrackIds = requestedIds,
            activeItems = activeItems,
        )
        refreshItemsInBackground(
            items = requestedItems,
            forceRefresh = true,
            refreshScope = GitHubRefreshScope.RequestedTracked,
            maxConcurrency = GitHubTrackedRefreshBatchScheduler.refreshConcurrency(
                requestedItems.size
            )
        )
        return true
    }

    fun refreshPartialMissingCheckStatesIfNeeded(): Boolean {
        val missingItems = GitHubTrackedRefreshPlanner.selectPartialMissingCheckStateItems(
            trackedItems = state.trackedItems.toList(),
            cachedTrackIds = state.checkStates.keys
        )
        if (missingItems.isEmpty()) return false
        refreshItemsInBackground(
            items = missingItems,
            refreshScope = GitHubRefreshScope.MissingCache,
            profilePurposeOverride = GitHubRepositoryProfilePurpose.VersionCheckFast,
            maxConcurrency = missingItems.size.coerceAtMost(
                GITHUB_MISSING_CHECK_STATE_REFRESH_PARALLELISM
            )
        )
        return true
    }

    private fun refreshItemsInBackground(
        items: List<GitHubTrackedApp>,
        forceRefresh: Boolean = false,
        refreshScope: GitHubRefreshScope,
        profilePurposeOverride: GitHubRepositoryProfilePurpose? = null,
        maxConcurrency: Int = GitHubTrackedRefreshBatchScheduler.refreshConcurrency(items.size)
    ) {
        if (items.isEmpty()) return
        var runtimeSessionId = 0L
        val job = scope.launch {
            val refreshStartedAtMs = clock.nowMs()
            val runtimeSession =
                GitHubRefreshRuntimeStore.begin(
                    scope = refreshScope,
                    source = GitHubRefreshSource.Page,
                    totalTrackedCount = state.trackedItems.size,
                    targetCount = items.size,
                    targetTrackIds = items.map { it.id },
                    policy = GitHubRefreshBeginPolicy.SkipWhenRunning,
                    nowMs = refreshStartedAtMs,
                )
            if (runtimeSession == null) {
                AppLogger.i(
                    "GitHubBackgroundRefresh",
                    "skip page background refresh scope=$refreshScope target=${items.size} because another session is running",
                )
                return@launch
            }
            runtimeSessionId = runtimeSession.id
            state.refreshSessionId = runtimeSession.id
            state.refreshTargetIds = items.mapTo(HashSet()) { it.id }
            state.refreshProgress = 0f
            state.overviewRefreshState = OverviewRefreshState.Refreshing
            val concurrency = items.size.coerceAtMost(maxConcurrency.coerceAtLeast(1))
            val previousCheckStatesById = state.checkStates.toMap()
            val batchEvaluator =
                GitHubTrackedRefreshBatchEvaluator(
                    trackedItems = items,
                    existingRepositoryProfileProvider = { item ->
                        previousCheckStatesById[item.id]?.repositoryProfile
                    },
                )
            val progressMutex = Mutex()
            var completedCount = 0
            var updatableCount = 0
            var preReleaseUpdateCount = 0
            var failedCount = 0
            var lastProgressNotifyAtMs = clock.nowMs()
            try {
                items.forEach(::markItemChecking)
                repository.notifyRefreshProgress(
                    context = context,
                    current = 0,
                    total = items.size,
                    preReleaseUpdateCount = 0,
                    updatableCount = 0,
                    failedCount = 0,
                    sessionId = runtimeSession.id,
                    scope = runtimeSession.scope,
                    source = runtimeSession.source,
                    totalTrackedCount = state.trackedItems.size,
                )
                items.forEach { item ->
                    prepareItemRefresh(item, previousCheckStatesById[item.id] ?: VersionCheckUi())
                }
                val batchResult = GitHubTrackedRefreshBatchRunner.run(
                    context = context,
                    items = items,
                    maxConcurrency = concurrency,
                    batchTimeoutMs = GITHUB_REFRESH_PAGE_BACKGROUND_BATCH_TIMEOUT_MS,
                    onItemResult = { item, check, _ ->
                        val previousState = previousCheckStatesById[item.id] ?: VersionCheckUi()
                        val itemState =
                            mergeResolvedState(
                                item,
                                check.toUi(),
                                previousState,
                            ).copy(checkedAtMillis = clock.nowMs())
                        withContext(Dispatchers.Main.immediate) {
                            if (state.trackedItems.any { tracked -> tracked.id == item.id }) {
                                state.checkStates[item.id] = itemState
                            }
                        }
                    },
                    onProgress = { progress ->
                        var progressNotifySnapshot: GitHubRefreshProgressSnapshot? = null
                        var totalTrackedCountForNotification = 0
                        withContext(Dispatchers.Main.immediate) {
                            progressMutex.withLock {
                                totalTrackedCountForNotification = state.trackedItems.size
                                completedCount = progress.current
                                updatableCount = progress.updatableCount
                                preReleaseUpdateCount = progress.preReleaseUpdateCount
                                failedCount = progress.failedCount
                                state.refreshProgress = completedCount.toFloat() / items.size.toFloat()
                                GitHubRefreshRuntimeStore.progress(
                                    sessionId = runtimeSession.id,
                                    completedCount = completedCount,
                                    updatableCount = updatableCount,
                                    preReleaseUpdateCount = preReleaseUpdateCount,
                                    failedCount = failedCount,
                                )
                                val nowMs = clock.nowMs()
                                val progressNotifyAgeMs = nowMs - lastProgressNotifyAtMs
                                val shouldNotifyProgress =
                                    completedCount < items.size &&
                                        (
                                            completedCount == 1 ||
                                                progressNotifyAgeMs >= GITHUB_REFRESH_PROGRESS_NOTIFY_INTERVAL_MS ||
                                                (
                                                    completedCount % GITHUB_REFRESH_PROGRESS_NOTIFY_BATCH_SIZE == 0 &&
                                                        progressNotifyAgeMs >=
                                                        GITHUB_REFRESH_PROGRESS_NOTIFY_MIN_INTERVAL_MS
                                                )
                                        )
                                if (shouldNotifyProgress) {
                                    lastProgressNotifyAtMs = nowMs
                                    progressNotifySnapshot =
                                        GitHubRefreshProgressSnapshot(
                                            current = completedCount,
                                            total = items.size,
                                            preReleaseUpdateCount = preReleaseUpdateCount,
                                            updatableCount = updatableCount,
                                            failedCount = failedCount,
                                        )
                                }
                            }
                        }
                        progressNotifySnapshot?.let { progressSnapshot ->
                            repository.notifyRefreshProgress(
                                context = context,
                                current = progressSnapshot.current,
                                total = progressSnapshot.total,
                                preReleaseUpdateCount = progressSnapshot.preReleaseUpdateCount,
                                updatableCount = progressSnapshot.updatableCount,
                                failedCount = progressSnapshot.failedCount,
                                sessionId = runtimeSession.id,
                                scope = runtimeSession.scope,
                                source = runtimeSession.source,
                                totalTrackedCount = totalTrackedCountForNotification,
                            )
                        }
                    },
                    evaluator = { _, item ->
                        evaluateItemCheck(
                            GitHubItemRefreshRequest(
                                item = item,
                                forceRefresh = forceRefresh,
                                profilePurposeOverride = profilePurposeOverride,
                                batchEvaluator = batchEvaluator
                            )
                        )
                    },
                )
                updatableCount = batchResult.updatableCount
                preReleaseUpdateCount = batchResult.preReleaseUpdateCount
                failedCount = batchResult.failedCount
                persistCheckCache(items.mapTo(HashSet()) { it.id })
                actionsRunRefreshCoordinator.refreshItems(items)
                GitHubRefreshRuntimeStore.complete(
                    sessionId = runtimeSession.id,
                    completedCount = batchResult.totalCount,
                    updatableCount = updatableCount,
                    preReleaseUpdateCount = preReleaseUpdateCount,
                    failedCount = failedCount,
                )
                refreshHistoryService.recordCompleted(
                    session = runtimeSession,
                    totalTrackedCount = state.trackedItems.size,
                    result = batchResult,
                    startedAtMillis = refreshStartedAtMs,
                )
                repository.notifyRefreshCompleted(
                    context = context,
                    total = items.size,
                    preReleaseUpdateCount = preReleaseUpdateCount,
                    updatableCount = updatableCount,
                    failedCount = failedCount,
                    sessionId = runtimeSession.id,
                    scope = runtimeSession.scope,
                    source = runtimeSession.source,
                    totalTrackedCount = state.trackedItems.size,
                )
                logTrackedRefreshFailures(batchResult.failures)
                state.overviewRefreshState =
                    if (failedCount > 0) {
                        OverviewRefreshState.Failed
                    } else {
                        OverviewRefreshState.Completed
                }
                state.refreshProgress = 1f
            } catch (error: CancellationException) {
                restoreInterruptedItemStates(
                    items = items,
                    previousCheckStatesById = previousCheckStatesById,
                )
                cancelRuntimeSessionAndNotify(
                    runtimeSessionId = runtimeSession.id,
                    completedCount = completedCount,
                    updatableCount = updatableCount,
                    preReleaseUpdateCount = preReleaseUpdateCount,
                    failedCount = failedCount,
                    outcome = GitHubRefreshHistoryOutcome.Cancelled,
                    note = error.message.orEmpty(),
                )
                throw error
            } catch (error: Throwable) {
                val failedMessage = error.message?.takeIf { it.isNotBlank() }
                    ?: error.javaClass.simpleName
                val terminalFailedCount = failedCount.coerceAtLeast(1)
                restoreInterruptedItemStates(
                    items = items,
                    previousCheckStatesById = previousCheckStatesById,
                    failedMessage = failedMessage,
                )
                cancelRuntimeSessionAndNotify(
                    runtimeSessionId = runtimeSession.id,
                    completedCount = completedCount,
                    updatableCount = updatableCount,
                    preReleaseUpdateCount = preReleaseUpdateCount,
                    failedCount = terminalFailedCount,
                    outcome = GitHubRefreshHistoryOutcome.Failed,
                    note = failedMessage,
                )
                if (state.refreshSessionId == runtimeSession.id) {
                    state.overviewRefreshState = OverviewRefreshState.Failed
                    state.refreshProgress = 0f
                }
                AppLogger.w(
                    "GitHubBackgroundRefresh",
                    "github page background refresh failed scope=$refreshScope target=${items.size}",
                    error,
                )
            } finally {
                if (state.refreshSessionId == runtimeSession.id) {
                    state.refreshSessionId = 0L
                    state.refreshTargetIds = emptySet()
                }
            }
        }
        backgroundJobs.add(job)
        job.invokeOnCompletion { cause ->
            if (cause != null && runtimeSessionId > 0L) {
                AppLogger.d(
                    "GitHubBackgroundRefresh",
                    "page background refresh job completed with ${cause.javaClass.simpleName}",
                )
            }
            backgroundJobs.remove(job)
        }
    }

    private suspend fun cancelRuntimeSessionAndNotify(
        runtimeSessionId: Long,
        completedCount: Int,
        updatableCount: Int,
        preReleaseUpdateCount: Int,
        failedCount: Int,
        outcome: GitHubRefreshHistoryOutcome,
        note: String,
    ) {
        withContext(NonCancellable) {
            val current = GitHubRefreshRuntimeStore.state.value
            if (current.sessionId != runtimeSessionId) return@withContext
            runCatching {
                refreshHistoryService.recordRuntimeState(
                    runtime = current,
                    outcome = outcome,
                    note = note,
                )
            }.onFailure { error ->
                AppLogger.w(
                    "GitHubBackgroundRefresh",
                    "github page background refresh history record failed",
                    error,
                )
            }
            GitHubRefreshRuntimeStore.cancel(
                sessionId = runtimeSessionId,
                completedCount = completedCount,
                updatableCount = updatableCount,
                preReleaseUpdateCount = preReleaseUpdateCount,
                failedCount = failedCount,
            )
            runCatching {
                if (outcome == GitHubRefreshHistoryOutcome.Failed) {
                    repository.notifyRefreshFailed(
                        context = context,
                        current = completedCount,
                        total = current.targetCount,
                        preReleaseUpdateCount = preReleaseUpdateCount,
                        updatableCount = updatableCount,
                        failedCount = failedCount,
                        sessionId = runtimeSessionId,
                        scope = current.scope,
                        source = current.source,
                        totalTrackedCount = current.totalTrackedCount,
                    )
                } else {
                    repository.notifyRefreshCancelled(
                        context = context,
                        current = completedCount,
                        total = current.targetCount,
                        preReleaseUpdateCount = preReleaseUpdateCount,
                        updatableCount = updatableCount,
                        failedCount = failedCount,
                        sessionId = runtimeSessionId,
                        scope = current.scope,
                        source = current.source,
                        totalTrackedCount = current.totalTrackedCount,
                    )
                }
            }.onFailure { error ->
                AppLogger.w(
                    "GitHubBackgroundRefresh",
                    "github page background refresh cancel notification failed",
                    error,
                )
            }
        }
    }

    private fun restoreInterruptedItemStates(
        items: List<GitHubTrackedApp>,
        previousCheckStatesById: Map<String, VersionCheckUi>,
        failedMessage: String? = null,
    ) {
        items.forEach { item ->
            val currentState = state.checkStates[item.id]
            if (currentState?.loading != true) return@forEach
            val previousState = previousCheckStatesById[item.id] ?: VersionCheckUi()
            state.checkStates[item.id] =
                if (failedMessage == null) {
                    previousState
                } else {
                    previousState.copy(
                        loading = false,
                        failed = true,
                        message = failedMessage,
                        checkedAtMillis = clock.nowMs(),
                    )
                }
        }
    }

    private fun markItemChecking(item: GitHubTrackedApp) {
        state.checkStates[item.id] = (state.checkStates[item.id] ?: VersionCheckUi()).copy(
            loading = true,
            message = context.getString(R.string.github_msg_checking)
        )
    }

    private fun logTrackedRefreshFailures(failures: List<GitHubTrackedRefreshFailure>) {
        failures.forEach { failure ->
            AppLogger.w(
                "GitHubBackgroundRefresh",
                "github page background refresh failed ${failure.logSummary()}",
            )
        }
    }
}
