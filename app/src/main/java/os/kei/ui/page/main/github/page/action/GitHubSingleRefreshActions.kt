package os.kei.ui.page.main.github.page.action

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import os.kei.R
import os.kei.feature.github.domain.GitHubTrackedRefreshBatchEvaluator
import os.kei.feature.github.domain.GitHubTrackedRefreshBatchRunner
import os.kei.feature.github.model.GitHubRepositoryProfilePurpose
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.VersionCheckUi
import os.kei.ui.page.main.github.statusMessage
import os.kei.ui.page.main.github.state.toUi

internal class GitHubSingleRefreshActions(
    private val owner: GitHubRefreshActions,
    private val assetActions: GitHubAssetActions,
    private val actionsRunRefreshCoordinator: GitHubActionsRecommendedRunRefreshCoordinator,
) {
    private val context get() = owner.context
    private val durableScope get() = owner.durableScope
    private val state get() = owner.state
    private val clock get() = owner.clock

    fun refreshItem(
        item: GitHubTrackedApp,
        showToastOnError: Boolean = false,
        keepCurrentVisualWhileRefreshing: Boolean = false,
        profilePurposeOverride: GitHubRepositoryProfilePurpose? = null,
        forceRefresh: Boolean = false,
        onUpdated: ((VersionCheckUi) -> Unit)? = null,
    ): Job =
        durableScope.launch {
            refreshItemNow(
                item = item,
                showToastOnError = showToastOnError,
                keepCurrentVisualWhileRefreshing = keepCurrentVisualWhileRefreshing,
                profilePurposeOverride = profilePurposeOverride,
                forceRefresh = forceRefresh,
                onUpdated = onUpdated,
            )
        }

    suspend fun refreshItemNow(
        item: GitHubTrackedApp,
        showToastOnError: Boolean = false,
        keepCurrentVisualWhileRefreshing: Boolean = false,
        profilePurposeOverride: GitHubRepositoryProfilePurpose? = null,
        forceRefresh: Boolean = false,
        persistAfterUpdate: Boolean = true,
        refreshActionsAfterUpdate: Boolean = true,
        batchEvaluator: GitHubTrackedRefreshBatchEvaluator? = null,
        onUpdated: ((VersionCheckUi) -> Unit)? = null,
    ) {
        val previousState = state.checkStates[item.id] ?: VersionCheckUi()
        assetActions.clearApkAssetCacheNow(
            item = item,
            itemState = previousState,
            allowLatestReleaseFallback = true,
        )
        val checkingMessage = context.getString(R.string.github_msg_checking)
        state.checkStates[item.id] =
            if (keepCurrentVisualWhileRefreshing) {
                previousState.copy(message = checkingMessage)
            } else {
                previousState.copy(
                    loading = true,
                    message = checkingMessage,
                )
            }
        val itemState =
            owner
                .mergeDirectApkRemoteFallback(
                    item = item,
                    resolvedState = resolveSingleItemState(
                        item = item,
                        profilePurposeOverride = profilePurposeOverride,
                        forceRefresh = forceRefresh,
                        previousState = previousState,
                        batchEvaluator = batchEvaluator,
                    ),
                    previousState = previousState,
                ).copy(checkedAtMillis = clock.nowMs())
        if (state.trackedItems.none { it.id == item.id }) return
        if (showToastOnError && itemState.failed) {
            owner.env.toast(itemState.statusMessage(context))
        }
        state.checkStates[item.id] = itemState
        if (persistAfterUpdate) owner.mergeCheckCacheNow(targetIds = setOf(item.id))
        if (refreshActionsAfterUpdate) actionsRunRefreshCoordinator.refreshItemInBackground(item)
        onUpdated?.invoke(itemState)
    }

    private suspend fun resolveSingleItemState(
        item: GitHubTrackedApp,
        profilePurposeOverride: GitHubRepositoryProfilePurpose?,
        forceRefresh: Boolean,
        previousState: VersionCheckUi,
        batchEvaluator: GitHubTrackedRefreshBatchEvaluator?,
    ): VersionCheckUi {
        val evaluator =
            batchEvaluator ?: GitHubTrackedRefreshBatchEvaluator(
                trackedItems = listOf(item),
                existingRepositoryProfileProvider = { previousState.repositoryProfile },
            )
        val result = GitHubTrackedRefreshBatchRunner.run(
            context = context,
            items = listOf(item),
            maxConcurrency = 1,
            batchTimeoutMs = GITHUB_REFRESH_PAGE_BACKGROUND_BATCH_TIMEOUT_MS,
            refreshTimestampMs = clock.nowMs(),
            evaluator = { _, tracked ->
                evaluator.evaluateTrackedApp(
                    context = context,
                    item = tracked,
                    profilePurposeOverride = profilePurposeOverride,
                    forceRefresh = forceRefresh,
                )
            },
        )
        return result.cacheEntries.getValue(item.id).toUi()
    }
}
