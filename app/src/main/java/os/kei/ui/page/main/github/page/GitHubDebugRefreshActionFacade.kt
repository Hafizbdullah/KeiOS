package os.kei.ui.page.main.github.page

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import os.kei.R
import os.kei.core.background.AppForegroundInfoHandler
import os.kei.core.background.AppShortcutGitHubRefreshResult
import os.kei.core.log.AppLogger
import os.kei.feature.github.domain.GitHubRefreshScope
import os.kei.feature.github.model.GitHubRefreshSchedulerDiagnostics
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.page.action.GitHubPageActionEnvironment
import os.kei.ui.page.main.github.page.action.GitHubRefreshActions

internal class GitHubDebugRefreshActionFacade(
    private val env: GitHubPageActionEnvironment,
    private val refreshActions: GitHubRefreshActions,
) {
    fun runBackgroundFullRefresh() {
        if (env.state.debugBackgroundFullRefreshLoading) return
        env.state.debugBackgroundFullRefreshLoading = true
        env.toast(R.string.github_debug_toast_background_full_refresh_started)
        env.scope.launch {
            try {
                val result =
                    AppForegroundInfoHandler.handleGitHubShortcutRefresh(
                        context = env.context.applicationContext,
                    )
                env.viewModel.refreshHistoryUnreadCount()
                env.toast(
                    when (result) {
                        AppShortcutGitHubRefreshResult.Completed ->
                            R.string.github_debug_toast_background_full_refresh_completed

                        AppShortcutGitHubRefreshResult.NoTrackedItems ->
                            R.string.github_debug_toast_background_full_refresh_empty
                    },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                AppLogger.w("GitHubDebugRefresh", "debug background full refresh failed", error)
                env.toast(
                    R.string.github_debug_toast_background_full_refresh_failed,
                    debugFailureMessage(error),
                )
            } finally {
                env.state.debugBackgroundFullRefreshLoading = false
            }
        }
    }

    fun runBackgroundDueRefresh() {
        if (env.state.debugBackgroundDueRefreshLoading) return
        env.state.debugBackgroundDueRefreshLoading = true
        env.toast(R.string.github_debug_toast_background_due_refresh_started)
        env.scope.launch {
            val nowMs = System.currentTimeMillis()
            try {
                AppForegroundInfoHandler.handleGitHubTick(
                    context = env.context.applicationContext,
                    schedulerDiagnostics =
                        GitHubRefreshSchedulerDiagnostics(
                            enqueuedAtMillis = nowMs,
                            startedAtMillis = nowMs,
                            stopReason = "debug_manual_due",
                        ),
                    suppressQuietBackgroundCompletion = false,
                )
                env.viewModel.refreshHistoryUnreadCount()
                env.toast(R.string.github_debug_toast_background_due_refresh_completed)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                AppLogger.w("GitHubDebugRefresh", "debug background due refresh failed", error)
                env.toast(
                    R.string.github_debug_toast_background_due_refresh_failed,
                    debugFailureMessage(error),
                )
            } finally {
                env.state.debugBackgroundDueRefreshLoading = false
            }
        }
    }

    fun refreshVisibleIncremental(visibleItems: List<GitHubTrackedApp>) {
        if (env.state.debugVisibleIncrementalRefreshLoading) return
        val targets = selectGitHubDebugVisibleRefreshTargets(visibleItems)
        if (targets.isEmpty()) {
            env.toast(R.string.github_toast_no_checkable_item)
            return
        }
        env.state.debugVisibleIncrementalRefreshLoading = true
        env.scope.launch {
            try {
                refreshActions.reloadApps(forceRefresh = true)
                refreshActions.refreshTrackedBatch(
                    targets = targets,
                    showToast = true,
                    forceRefresh = true,
                    refreshScope = GitHubRefreshScope.VisibleTracked,
                    onFinished = {
                        env.viewModel.refreshHistoryUnreadCount()
                    },
                )
                val refreshJob = env.state.refreshAllJob
                if (refreshJob == null) {
                    env.state.debugVisibleIncrementalRefreshLoading = false
                } else {
                    refreshJob.invokeOnCompletion {
                        env.scope.launch {
                            env.state.debugVisibleIncrementalRefreshLoading = false
                        }
                    }
                }
            } catch (error: CancellationException) {
                env.state.debugVisibleIncrementalRefreshLoading = false
                throw error
            } catch (error: Throwable) {
                env.state.debugVisibleIncrementalRefreshLoading = false
                AppLogger.w("GitHubDebugRefresh", "debug visible incremental refresh failed", error)
                env.toast(
                    R.string.github_debug_toast_visible_incremental_refresh_failed,
                    debugFailureMessage(error),
                )
            }
        }
    }

    private fun debugFailureMessage(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
}
