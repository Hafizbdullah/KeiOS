package os.kei.core.background

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.log.AppLogger
import os.kei.feature.github.domain.GitHubBackgroundRefreshService
import os.kei.feature.github.domain.GitHubRefreshHistoryService
import os.kei.feature.github.domain.GitHubRefreshRuntimePhase
import os.kei.feature.github.domain.GitHubRefreshRuntimeSession
import os.kei.feature.github.domain.GitHubRefreshRuntimeState
import os.kei.feature.github.domain.GitHubRefreshRuntimeStore
import os.kei.feature.github.domain.GitHubRefreshScope
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.github.domain.GitHubShortcutRefreshExecution
import os.kei.feature.github.domain.GitHubTrackedRefreshBatchProgress
import os.kei.feature.github.model.GitHubRefreshHistoryOutcome
import os.kei.feature.github.model.GitHubRefreshSchedulerDiagnostics
import os.kei.feature.github.notification.GitHubActionsUpdateNotificationHelper
import os.kei.feature.github.notification.GitHubRefreshNotificationHelper
import os.kei.ui.page.main.ba.BaAccountNotificationKind
import os.kei.ui.page.main.ba.BaApNotificationDispatcher
import os.kei.ui.page.main.ba.BaApReminderPlan
import os.kei.ui.page.main.ba.BaArenaRefreshNotificationDispatcher
import os.kei.ui.page.main.ba.BaCafeApNotificationDispatcher
import os.kei.ui.page.main.ba.BaCafeApReminderPlan
import os.kei.ui.page.main.ba.BaCafeVisitNotificationDispatcher
import os.kei.ui.page.main.ba.BaReminderCoordinator
import os.kei.ui.page.main.ba.BaSlotReminderPlan
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.BaAccountId
import os.kei.ui.page.main.ba.support.BaAccountReminderSnapshot
import os.kei.ui.page.main.ba.support.BaApReminderKind
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import kotlin.coroutines.coroutineContext

object AppForegroundInfoHandler {
    private const val GITHUB_BACKGROUND_PROGRESS_NOTIFY_MIN_TOTAL = 8
    private const val GITHUB_BACKGROUND_PROGRESS_NOTIFY_FIRST_DELAY_MS = 12_000L
    private const val GITHUB_SHORTCUT_PROGRESS_NOTIFY_BATCH_SIZE = 2
    private const val GITHUB_SHORTCUT_PROGRESS_NOTIFY_MIN_INTERVAL_MS = 500L
    private const val GITHUB_SHORTCUT_PROGRESS_NOTIFY_INTERVAL_MS = 850L

    private val githubRefreshService = GitHubBackgroundRefreshService()
    private val githubRefreshHistoryService = GitHubRefreshHistoryService()
    private val baApTickMutex = Mutex()

    suspend fun handleGitHubTick(
        context: Context,
        schedulerDiagnostics: GitHubRefreshSchedulerDiagnostics = GitHubRefreshSchedulerDiagnostics(),
        suppressQuietBackgroundCompletion: Boolean = false,
        cleanupCancellationLocally: Boolean = true,
        onRefreshSessionStarted: (GitHubRefreshRuntimeSession) -> Unit = {},
    ) {
        val progressNotifier = GitHubRefreshProgressNotifier(
            context = context,
            minTotalForInitialProgress = GITHUB_BACKGROUND_PROGRESS_NOTIFY_MIN_TOTAL,
            initialProgressDelayMs = GITHUB_BACKGROUND_PROGRESS_NOTIFY_FIRST_DELAY_MS,
        )
        val result =
            try {
                githubRefreshService.runDueRefresh(
                    context = context,
                    onRefreshStart = { session, total, totalTrackedCount ->
                        onRefreshSessionStarted(session)
                        progressNotifier.notifyInitial(session, total, totalTrackedCount)
                    },
                    onRefreshProgress = progressNotifier::notifyProgress,
                    onActionsUpdateAvailable = { snapshot ->
                        GitHubActionsUpdateNotificationHelper.notifyUpdateAvailable(
                            context = context,
                            snapshot = snapshot,
                        )
                    },
                    schedulerDiagnostics = schedulerDiagnostics,
                )
            } catch (error: CancellationException) {
                if (cleanupCancellationLocally) {
                    withContext(NonCancellable) {
                        cleanupGitHubRefreshRuntimeAndNotification(
                            context = context,
                            reason = "github tick cancelled",
                            outcome = GitHubRefreshHistoryOutcome.Cancelled,
                            schedulerDiagnostics = schedulerDiagnostics,
                        )
                    }
                }
                throw error
            } catch (error: Throwable) {
                cleanupGitHubRefreshRuntimeAndNotification(
                    context = context,
                    reason = "github tick failed",
                    outcome = GitHubRefreshHistoryOutcome.Failed,
                    schedulerDiagnostics = schedulerDiagnostics,
                )
                AppLogger.w("AppForegroundInfoHandler", "github tick failed", error)
                return
            }
        val refreshResult = result.refreshResult
        if (refreshResult != null) {
            if (
                suppressQuietBackgroundCompletion &&
                progressNotifier.session?.source == GitHubRefreshSource.BackgroundTick &&
                !refreshResult.hasNotifiableOutcome
            ) {
                runCatching {
                    GitHubRefreshNotificationHelper.cancel(
                        context = context,
                        sessionId = progressNotifier.session?.id ?: 0L,
                    )
                }
                    .onFailure { error ->
                        AppLogger.w(
                            "AppForegroundInfoHandler",
                            "github quiet background completion notification cancel failed",
                            error,
                        )
                    }
            } else {
                notifyGitHubRefreshCompletedOrCancel(
                    context = context,
                    total = refreshResult.totalCount,
                    preReleaseUpdateCount = refreshResult.preReleaseUpdateCount,
                    updatableCount = refreshResult.updatableCount,
                    failedCount = refreshResult.failedCount,
                    session = progressNotifier.session,
                    totalTrackedCount = progressNotifier.totalTrackedCount,
                )
            }
        } else if (progressNotifier.didNotify) {
            runCatching {
                GitHubRefreshNotificationHelper.cancel(
                    context = context,
                    sessionId = progressNotifier.session?.id ?: 0L,
                )
            }
                .onFailure { error ->
                    AppLogger.w(
                        "AppForegroundInfoHandler",
                        "github refresh quiet tick notification cancel failed",
                        error
                    )
                }
        }
    }

    suspend fun handleGitHubTickStopped(
        context: Context,
        schedulerDiagnostics: GitHubRefreshSchedulerDiagnostics = GitHubRefreshSchedulerDiagnostics(),
        stoppedRuntime: GitHubRefreshRuntimeState?,
    ) {
        val reason =
            schedulerDiagnostics.stopReason
                .takeIf { it.isNotBlank() }
                ?.let { "github tick stopped: $it" }
                ?: "github tick stopped"
        if (stoppedRuntime == null) return
        withContext(NonCancellable) {
            cleanupGitHubRefreshRuntimeAndNotification(
                context = context,
                reason = reason,
                outcome = GitHubRefreshHistoryOutcome.Cancelled,
                schedulerDiagnostics = schedulerDiagnostics,
                runtimeSnapshot = stoppedRuntime,
            )
        }
    }

    internal suspend fun handleGitHubShortcutRefresh(context: Context): AppShortcutGitHubRefreshResult {
        val progressNotifier = GitHubRefreshProgressNotifier(context = context)
        return when (
            val execution =
                githubRefreshService.runShortcutRefresh(
                    context = context,
                    onStart = progressNotifier::notifyInitial,
                    onProgress = progressNotifier::notifyProgress,
                    onActionsUpdateAvailable = { snapshot ->
                        GitHubActionsUpdateNotificationHelper.notifyUpdateAvailable(
                            context = context,
                            snapshot = snapshot,
                        )
                    },
                )
        ) {
            GitHubShortcutRefreshExecution.NoTrackedItems -> {
                runCatching {
                    progressNotifier.session?.let { session ->
                        GitHubRefreshNotificationHelper.cancel(context, session.id)
                    }
                }
                    .onFailure { error ->
                        AppLogger.w(
                            "AppForegroundInfoHandler",
                            "github refresh empty shortcut notification cancel failed",
                            error
                        )
                    }
                AppShortcutGitHubRefreshResult.NoTrackedItems
            }

            is GitHubShortcutRefreshExecution.Completed -> {
                val result = execution.result
                notifyGitHubRefreshCompletedOrCancel(
                context = context,
                total = result.totalCount,
                preReleaseUpdateCount = result.preReleaseUpdateCount,
                updatableCount = result.updatableCount,
                failedCount = result.failedCount,
                session = progressNotifier.session,
                totalTrackedCount = progressNotifier.totalTrackedCount,
            )
            AppBackgroundScheduler.scheduleGitHubRefresh(context)
                AppShortcutGitHubRefreshResult.Completed
            }
        }
    }

    private fun notifyGitHubRefreshCompletedOrCancel(
        context: Context,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int,
        session: GitHubRefreshRuntimeSession?,
        totalTrackedCount: Int,
    ) {
        val posted =
            runCatching {
                GitHubRefreshNotificationHelper.notifyCompleted(
                    context = context,
                    total = total,
                    preReleaseUpdateCount = preReleaseUpdateCount,
                    updatableCount = updatableCount,
                    failedCount = failedCount,
                    sessionId = session?.id ?: 0L,
                    scope = session?.scope ?: GitHubRefreshScope.AllTracked,
                    source = session?.source ?: GitHubRefreshSource.BackgroundTick,
                    totalTrackedCount = totalTrackedCount.coerceAtLeast(total),
                )
            }.getOrElse { error ->
                AppLogger.w(
                    "AppForegroundInfoHandler",
                    "github refresh completed notification failed",
                    error
                )
                false
            }
        if (!posted) {
            runCatching {
                session?.let { activeSession ->
                    GitHubRefreshNotificationHelper.cancel(context, activeSession.id)
                }
            }
                .onFailure { error ->
                    AppLogger.w(
                        "AppForegroundInfoHandler",
                        "github refresh stale notification cancel failed",
                        error
                    )
                }
        }
    }

    private suspend fun cleanupGitHubRefreshRuntimeAndNotification(
        context: Context,
        reason: String,
        outcome: GitHubRefreshHistoryOutcome,
        schedulerDiagnostics: GitHubRefreshSchedulerDiagnostics = GitHubRefreshSchedulerDiagnostics(),
        runtimeSnapshot: GitHubRefreshRuntimeState? = null,
    ) {
        val observedRuntime = runtimeSnapshot ?: GitHubRefreshRuntimeStore.state.value
        val runtime = GitHubRefreshRuntimeStore.claimBackgroundTerminalCleanup(observedRuntime)
        if (runtime == null) {
            val currentRuntime = GitHubRefreshRuntimeStore.state.value
            if (runtimeSnapshot != null) {
                if (
                    observedRuntime.phase == GitHubRefreshRuntimePhase.Completed &&
                    currentRuntime.sessionId == observedRuntime.sessionId
                ) {
                    runCatching {
                        GitHubRefreshNotificationHelper.cancel(
                            context = context,
                            sessionId = observedRuntime.sessionId,
                        )
                    }
                        .onFailure { error ->
                            AppLogger.w(
                                "AppForegroundInfoHandler",
                                "$reason completed notification cleanup failed",
                                error,
                            )
                        }
                }
                return
            }
            if (
                currentRuntime.sessionId == observedRuntime.sessionId &&
                currentRuntime.terminalCleanupClaimed
            ) {
                AppLogger.i(
                    "AppForegroundInfoHandler",
                    "$reason skipped because session ${observedRuntime.sessionId} cleanup is already claimed",
                )
                return
            }
            if (currentRuntime.running) return
            runCatching {
                GitHubRefreshNotificationHelper.cancel(
                    context = context,
                    sessionId = observedRuntime.sessionId,
                )
            }
                .onFailure { error ->
                    AppLogger.w(
                        "AppForegroundInfoHandler",
                        "$reason notification cleanup failed",
                        error,
                    )
                }
            return
        }

        runCatching {
            githubRefreshHistoryService.recordRuntimeState(
                runtime = runtime,
                outcome = outcome,
                note = reason,
                schedulerDiagnostics = schedulerDiagnostics,
            )
        }.onFailure { error ->
            AppLogger.w(
                "AppForegroundInfoHandler",
                "$reason history record failed",
                error,
            )
        }
        GitHubRefreshRuntimeStore.cancel(
            sessionId = runtime.sessionId,
            completedCount = runtime.completedCount,
            updatableCount = runtime.updatableCount,
            preReleaseUpdateCount = runtime.preReleaseUpdateCount,
            failedCount = runtime.failedCount,
        )
        val notificationRuntime = GitHubRefreshRuntimeStore.state.value
        if (
            notificationRuntime.sessionId > 0L &&
            notificationRuntime.sessionId != runtime.sessionId
        ) {
            AppLogger.i(
                "AppForegroundInfoHandler",
                "$reason skipped notification because session ${notificationRuntime.sessionId} superseded ${runtime.sessionId}",
            )
            return
        }
        if (runtime.targetCount > 0) {
            val terminalPosted =
                runCatching {
                    when (outcome) {
                        GitHubRefreshHistoryOutcome.Failed ->
                            GitHubRefreshNotificationHelper.notifyFailed(
                                context = context,
                                current = runtime.completedCount,
                                total = runtime.targetCount,
                                preReleaseUpdateCount = runtime.preReleaseUpdateCount,
                                updatableCount = runtime.updatableCount,
                                failedCount = runtime.failedCount.coerceAtLeast(1),
                                sessionId = runtime.sessionId,
                                scope = runtime.scope,
                                source = runtime.source,
                                totalTrackedCount = runtime.totalTrackedCount,
                            )

                        GitHubRefreshHistoryOutcome.Cancelled ->
                            GitHubRefreshNotificationHelper.notifyCancelled(
                                context = context,
                                current = runtime.completedCount,
                                total = runtime.targetCount,
                                preReleaseUpdateCount = runtime.preReleaseUpdateCount,
                                updatableCount = runtime.updatableCount,
                                failedCount = runtime.failedCount,
                                sessionId = runtime.sessionId,
                                scope = runtime.scope,
                                source = runtime.source,
                                totalTrackedCount = runtime.totalTrackedCount,
                            )

                        GitHubRefreshHistoryOutcome.Completed -> false
                    }
                }.getOrElse { error ->
                    AppLogger.w(
                        "AppForegroundInfoHandler",
                        "$reason terminal notification failed",
                        error
                    )
                    false
                }
            if (terminalPosted) return
        }

        runCatching {
            GitHubRefreshNotificationHelper.cancel(
                context = context,
                sessionId = runtime.sessionId,
            )
        }
            .onFailure { error ->
                AppLogger.w(
                    "AppForegroundInfoHandler",
                    "$reason notification cleanup failed",
                    error
                )
            }
    }

    private class GitHubRefreshProgressNotifier(
        private val context: Context,
        private val minTotalForInitialProgress: Int = 1,
        private val initialProgressDelayMs: Long = 0L,
    ) {
        private val mutex = Mutex()
        private var startedAtMs = 0L
        private var lastNotifyAtMs = 0L
        @Volatile
        var session: GitHubRefreshRuntimeSession? = null
            private set
        @Volatile
        var totalTrackedCount: Int = 0
            private set
        @Volatile
        var didNotify: Boolean = false
            private set

        fun notifyInitial(
            session: GitHubRefreshRuntimeSession,
            total: Int,
            totalTrackedCount: Int,
        ) {
            this.session = session
            this.totalTrackedCount = totalTrackedCount
            val nowMs = System.currentTimeMillis()
            startedAtMs = nowMs
            lastNotifyAtMs = nowMs
            if (total < minTotalForInitialProgress) return
            if (initialProgressDelayMs > 0L) return
            runCatching {
                GitHubRefreshNotificationHelper.notifyProgress(
                    context = context,
                    current = 0,
                    total = total,
                    preReleaseUpdateCount = 0,
                    updatableCount = 0,
                    failedCount = 0,
                    sessionId = session.id,
                    scope = session.scope,
                    source = session.source,
                    totalTrackedCount = totalTrackedCount
                )
            }.onSuccess { posted ->
                if (posted) didNotify = true
            }.onFailure { error ->
                AppLogger.w(
                    "AppForegroundInfoHandler",
                    "github refresh initial progress notification failed",
                    error
                )
            }
        }

        suspend fun notifyProgress(
            session: GitHubRefreshRuntimeSession,
            progress: GitHubTrackedRefreshBatchProgress,
        ) {
            if (progress.total < minTotalForInitialProgress) return
            val shouldNotify = mutex.withLock {
                if (progress.current >= progress.total) return@withLock false
                val nowMs = System.currentTimeMillis()
                val hasVisibleOutcome =
                    progress.updatableCount > 0 ||
                        progress.preReleaseUpdateCount > 0 ||
                        progress.failedCount > 0
                if (
                    !didNotify &&
                    initialProgressDelayMs > 0L &&
                    !hasVisibleOutcome &&
                    nowMs - startedAtMs < initialProgressDelayMs
                ) {
                    return@withLock false
                }
                val elapsedMs = (nowMs - lastNotifyAtMs).coerceAtLeast(0L)
                val shouldEmit =
                    !didNotify ||
                        progress.current == 1 ||
                        elapsedMs >= GITHUB_SHORTCUT_PROGRESS_NOTIFY_INTERVAL_MS ||
                        (
                            progress.current % GITHUB_SHORTCUT_PROGRESS_NOTIFY_BATCH_SIZE == 0 &&
                                elapsedMs >= GITHUB_SHORTCUT_PROGRESS_NOTIFY_MIN_INTERVAL_MS
                        )
                if (shouldEmit) {
                    lastNotifyAtMs = nowMs
                }
                shouldEmit
            }
            if (!shouldNotify) return
            runCatching {
                GitHubRefreshNotificationHelper.notifyProgress(
                    context = context,
                    current = progress.current,
                    total = progress.total,
                    preReleaseUpdateCount = progress.preReleaseUpdateCount,
                    updatableCount = progress.updatableCount,
                    failedCount = progress.failedCount,
                    sessionId = session.id,
                    scope = session.scope,
                    source = session.source,
                    totalTrackedCount = totalTrackedCount.coerceAtLeast(progress.total),
                )
            }.onSuccess { posted ->
                if (posted) didNotify = true
            }.onFailure { error ->
                AppLogger.w(
                    "AppForegroundInfoHandler",
                    "github refresh progress notification failed",
                    error
                )
            }
        }
    }

    suspend fun handleBaApTick(context: Context) {
        baApTickMutex.withLock {
            val reminderSnapshots =
                withContext(AppDispatchers.mcpServer) {
                    BASettingsStore.loadReminderSnapshots()
                }
            if (!AppBackgroundSchedulePolicy.hasEnabledBaReminder(reminderSnapshots.map { it.snapshot })) {
                resetReminderRuntimeForAccounts(reminderSnapshots)
                return
            }

            val nowMs = System.currentTimeMillis()
            reminderSnapshots.forEach { reminderSnapshot ->
                coroutineContext.ensureActive()
                handleBaReminderTick(
                    context = context,
                    reminderSnapshot = reminderSnapshot,
                    nowMs = nowMs,
                )
                yield()
            }
        }
    }

    private suspend fun resetReminderRuntimeForAccounts(reminderSnapshots: List<BaAccountReminderSnapshot>) {
        withContext(AppDispatchers.mcpServer) {
            BASettingsStore.resetReminderRuntimeForAccounts(reminderSnapshots.map { it.accountId })
        }
    }

    private suspend fun handleBaReminderTick(
        context: Context,
        reminderSnapshot: BaAccountReminderSnapshot,
        nowMs: Long,
    ) {
        val accountId = reminderSnapshot.accountId
        val accountDisplayName = reminderSnapshot.displayName
        val snapshot = reminderSnapshot.snapshot

        coroutineContext.ensureActive()
        if (snapshot.apNotifyEnabled) {
            handleBaApThresholdTick(
                context = context,
                accountId = accountId,
                accountDisplayName = accountDisplayName,
                snapshot = snapshot,
                nowMs = nowMs,
            )
        } else {
            persistBaForegroundApReminderWrites(
                accountId = accountId,
                writes = BaForegroundApReminderPersistencePolicy.disabledWrites(BaApReminderKind.Ap),
            )
        }

        coroutineContext.ensureActive()
        if (snapshot.cafeApNotifyEnabled) {
            handleBaCafeApThresholdTick(
                context = context,
                accountId = accountId,
                accountDisplayName = accountDisplayName,
                snapshot = snapshot,
                nowMs = nowMs,
            )
        } else {
            persistBaForegroundApReminderWrites(
                accountId = accountId,
                writes = BaForegroundApReminderPersistencePolicy.disabledWrites(BaApReminderKind.CafeAp),
            )
        }

        coroutineContext.ensureActive()
        if (snapshot.arenaRefreshNotifyEnabled) {
            handleBaArenaRefreshTick(
                context = context,
                accountId = accountId,
                accountDisplayName = accountDisplayName,
                snapshot = snapshot,
                nowMs = nowMs,
            )
        } else {
            withContext(AppDispatchers.mcpServer) {
                BASettingsStore.saveAccountArenaRefreshLastNotifiedSlotMs(accountId, 0L)
            }
        }

        coroutineContext.ensureActive()
        if (snapshot.cafeVisitNotifyEnabled) {
            handleBaCafeVisitTick(
                context = context,
                accountId = accountId,
                accountDisplayName = accountDisplayName,
                snapshot = snapshot,
                nowMs = nowMs,
            )
        } else {
            withContext(AppDispatchers.mcpServer) {
                BASettingsStore.saveAccountCafeVisitLastNotifiedSlotMs(accountId, 0L)
            }
        }
    }

    private suspend fun handleBaCafeApThresholdTick(
        context: Context,
        accountId: BaAccountId,
        accountDisplayName: String,
        snapshot: BaPageSnapshot,
        nowMs: Long,
    ) {
        val plan = BaReminderCoordinator.evaluateCafeApThreshold(snapshot = snapshot, nowMs = nowMs)
        persistBaCafeApReminderPlan(accountId = accountId, plan = plan)
        val notification = plan.notification ?: return
        val deliveryWrites =
            BaForegroundApReminderPersistencePolicy.deliveryWrites(
                kind = BaApReminderKind.CafeAp,
                sent = true,
                currentDisplay = notification.currentDisplay,
                advanceSuppressionAnchorAfterDelivery =
                    plan.advanceSuppressionAnchorAfterDelivery,
                clearDismissedUntilAfterDelivery =
                    plan.clearDismissedUntilAfterDelivery,
                nowMs = nowMs,
            )
        BaCafeApNotificationDispatcher.sendAwaitingDelivery(
            context = context,
            currentDisplay = notification.currentDisplay,
            limitDisplay = notification.limitDisplay,
            thresholdDisplay = notification.thresholdDisplay,
            notificationId = BaAccountNotificationKind.CafeAp.notificationId(accountId),
            accountDisplayName = accountDisplayName,
            accountId = accountId,
            onDelivered = {
                persistBaForegroundApReminderWrites(
                    accountId = accountId,
                    writes = deliveryWrites,
                    cancellationResilient = false,
                )
            },
        )
    }

    private suspend fun persistBaCafeApReminderPlan(
        accountId: BaAccountId,
        plan: BaCafeApReminderPlan,
    ) {
        if (plan.shouldSaveCafe) {
            withContext(AppDispatchers.mcpServer) {
                BASettingsStore.saveAccountBaRuntimeState(
                    accountId = accountId,
                    cafeStoredAp = plan.nextStoredAp,
                    cafeLastHourMs = plan.nextCafeLastHourMs,
                    notifyHomeOverview = false,
                )
            }
        }
        if (plan.resetLastNotifiedLevel) {
            withContext(AppDispatchers.mcpServer) {
                BASettingsStore.saveAccountCafeApLastNotifiedLevel(
                    accountId = accountId,
                    level = -1,
                )
            }
        }
        if (plan.resetSuppressionAnchor) {
            withContext(AppDispatchers.mcpServer) {
                BASettingsStore.saveAccountApSuppressionAnchor(
                    accountId = accountId,
                    kind = BaApReminderKind.CafeAp,
                    anchorAtMs = 0L,
                )
            }
        }
        if (plan.resetDismissedUntil) {
            withContext(AppDispatchers.mcpServer) {
                BASettingsStore.saveAccountApDismissedUntil(
                    accountId = accountId,
                    kind = BaApReminderKind.CafeAp,
                    dismissedUntilAtMs = 0L,
                )
            }
        }
    }

    private suspend fun handleBaApThresholdTick(
        context: Context,
        accountId: BaAccountId,
        accountDisplayName: String,
        snapshot: BaPageSnapshot,
        nowMs: Long,
    ) {
        val plan = BaReminderCoordinator.evaluateApThreshold(snapshot = snapshot, nowMs = nowMs)
        persistBaApReminderPlan(accountId = accountId, plan = plan)
        val notification = plan.notification ?: return
        val deliveryWrites =
            BaForegroundApReminderPersistencePolicy.deliveryWrites(
                kind = BaApReminderKind.Ap,
                sent = true,
                currentDisplay = notification.currentDisplay,
                advanceSuppressionAnchorAfterDelivery =
                    plan.advanceSuppressionAnchorAfterDelivery,
                clearDismissedUntilAfterDelivery =
                    plan.clearDismissedUntilAfterDelivery,
                nowMs = nowMs,
            )
        BaApNotificationDispatcher.sendAwaitingDelivery(
            context = context,
            currentDisplay = notification.currentDisplay,
            limitDisplay = notification.limitDisplay,
            thresholdDisplay = notification.thresholdDisplay,
            notificationId = BaAccountNotificationKind.Ap.notificationId(accountId),
            accountDisplayName = accountDisplayName,
            accountId = accountId,
            onDelivered = {
                persistBaForegroundApReminderWrites(
                    accountId = accountId,
                    writes = deliveryWrites,
                    cancellationResilient = false,
                )
            },
        )
    }

    private suspend fun persistBaApReminderPlan(
        accountId: BaAccountId,
        plan: BaApReminderPlan,
    ) {
        if (plan.shouldSaveAp) {
            withContext(AppDispatchers.mcpServer) {
                BASettingsStore.saveAccountBaRuntimeState(
                    accountId = accountId,
                    apCurrent = plan.nextAp,
                    apRegenBaseMs = plan.nextApRegenBaseMs,
                    notifyHomeOverview = false,
                )
            }
        }
        if (plan.resetLastNotifiedLevel) {
            withContext(AppDispatchers.mcpServer) {
                BASettingsStore.saveAccountApLastNotifiedLevel(
                    accountId = accountId,
                    level = -1,
                )
            }
        }
        if (plan.resetSuppressionAnchor) {
            withContext(AppDispatchers.mcpServer) {
                BASettingsStore.saveAccountApSuppressionAnchor(
                    accountId = accountId,
                    kind = BaApReminderKind.Ap,
                    anchorAtMs = 0L,
                )
            }
        }
        if (plan.resetDismissedUntil) {
            withContext(AppDispatchers.mcpServer) {
                BASettingsStore.saveAccountApDismissedUntil(
                    accountId = accountId,
                    kind = BaApReminderKind.Ap,
                    dismissedUntilAtMs = 0L,
                )
            }
        }
    }

    internal suspend fun persistBaForegroundApReminderWrites(
        accountId: BaAccountId,
        writes: List<BaForegroundApReminderWrite>,
        cancellationResilient: Boolean = true,
        persistWrite: suspend (BaAccountId, BaForegroundApReminderWrite) -> Unit =
            ::persistBaForegroundApReminderWrite,
    ) {
        if (writes.isEmpty()) return
        val persistAll: suspend () -> Unit = {
            writes.forEach { write ->
                persistWrite(accountId, write)
            }
        }
        if (cancellationResilient) {
            withContext(NonCancellable + AppDispatchers.mcpServer) { persistAll() }
        } else {
            withContext(AppDispatchers.mcpServer) { persistAll() }
        }
    }

    private fun persistBaForegroundApReminderWrite(
        accountId: BaAccountId,
        write: BaForegroundApReminderWrite,
    ) {
        write.lastNotifiedLevel?.let { level ->
            when (write.kind) {
                BaApReminderKind.Ap ->
                    BASettingsStore.saveAccountApLastNotifiedLevel(accountId, level)

                BaApReminderKind.CafeAp ->
                    BASettingsStore.saveAccountCafeApLastNotifiedLevel(accountId, level)
            }
        }
        write.suppressionAnchorAtMs?.let { anchorAtMs ->
            BASettingsStore.saveAccountApSuppressionAnchor(
                accountId = accountId,
                kind = write.kind,
                anchorAtMs = anchorAtMs,
            )
        }
        write.dismissedUntilAtMs?.let { dismissedUntilAtMs ->
            BASettingsStore.saveAccountApDismissedUntil(
                accountId = accountId,
                kind = write.kind,
                dismissedUntilAtMs = dismissedUntilAtMs,
            )
        }
    }

    private suspend fun handleBaCafeVisitTick(
        context: Context,
        accountId: BaAccountId,
        accountDisplayName: String,
        snapshot: BaPageSnapshot,
        nowMs: Long,
    ) {
        when (val plan = BaReminderCoordinator.evaluateCafeVisit(snapshot = snapshot, nowMs = nowMs)) {
            BaSlotReminderPlan.None -> Unit
            BaSlotReminderPlan.Reset -> {
                withContext(AppDispatchers.mcpServer) {
                    BASettingsStore.saveAccountCafeVisitLastNotifiedSlotMs(accountId, 0L)
                }
            }

            is BaSlotReminderPlan.SeedBaseline -> {
                withContext(AppDispatchers.mcpServer) {
                    BASettingsStore.saveAccountCafeVisitLastNotifiedSlotMs(accountId, plan.slotMs)
                }
            }

            is BaSlotReminderPlan.Notify -> {
                val sent = BaCafeVisitNotificationDispatcher.send(
                    context = context,
                    serverIndex = snapshot.serverIndex,
                    slotMs = plan.slotMs,
                    notificationId = BaAccountNotificationKind.CafeVisit.notificationId(accountId),
                    accountDisplayName = accountDisplayName,
                    accountId = accountId,
                )
                if (sent) {
                    withContext(AppDispatchers.mcpServer) {
                        BASettingsStore.saveAccountCafeVisitLastNotifiedSlotMs(accountId, plan.slotMs)
                    }
                }
            }
        }
    }

    private suspend fun handleBaArenaRefreshTick(
        context: Context,
        accountId: BaAccountId,
        accountDisplayName: String,
        snapshot: BaPageSnapshot,
        nowMs: Long,
    ) {
        when (val plan = BaReminderCoordinator.evaluateArenaRefresh(snapshot = snapshot, nowMs = nowMs)) {
            BaSlotReminderPlan.None -> Unit
            BaSlotReminderPlan.Reset -> {
                withContext(AppDispatchers.mcpServer) {
                    BASettingsStore.saveAccountArenaRefreshLastNotifiedSlotMs(accountId, 0L)
                }
            }

            is BaSlotReminderPlan.SeedBaseline -> {
                withContext(AppDispatchers.mcpServer) {
                    BASettingsStore.saveAccountArenaRefreshLastNotifiedSlotMs(accountId, plan.slotMs)
                }
            }

            is BaSlotReminderPlan.Notify -> {
                val sent = BaArenaRefreshNotificationDispatcher.send(
                    context = context,
                    serverIndex = snapshot.serverIndex,
                    slotMs = plan.slotMs,
                    notificationId = BaAccountNotificationKind.ArenaRefresh.notificationId(accountId),
                    accountDisplayName = accountDisplayName,
                    accountId = accountId,
                )
                if (sent) {
                    withContext(AppDispatchers.mcpServer) {
                        BASettingsStore.saveAccountArenaRefreshLastNotifiedSlotMs(accountId, plan.slotMs)
                    }
                }
            }
        }
    }

}

internal enum class AppShortcutGitHubRefreshResult {
    Completed,
    NoTrackedItems
}

internal data class BaForegroundApReminderWrite(
    val kind: BaApReminderKind,
    val lastNotifiedLevel: Int? = null,
    val suppressionAnchorAtMs: Long? = null,
    val dismissedUntilAtMs: Long? = null,
)

internal object BaForegroundApReminderPersistencePolicy {
    fun disabledWrites(kind: BaApReminderKind): List<BaForegroundApReminderWrite> =
        listOf(
            BaForegroundApReminderWrite(
                kind = kind,
                lastNotifiedLevel = -1,
            ),
            BaForegroundApReminderWrite(
                kind = kind,
                suppressionAnchorAtMs = 0L,
            ),
            BaForegroundApReminderWrite(
                kind = kind,
                dismissedUntilAtMs = 0L,
            ),
        )

    fun deliveryWrites(
        kind: BaApReminderKind,
        sent: Boolean,
        currentDisplay: Int,
        advanceSuppressionAnchorAfterDelivery: Boolean,
        clearDismissedUntilAfterDelivery: Boolean = false,
        nowMs: Long,
    ): List<BaForegroundApReminderWrite> {
        if (!sent) return emptyList()
        return buildList {
            add(
                BaForegroundApReminderWrite(
                    kind = kind,
                    lastNotifiedLevel = currentDisplay,
                ),
            )
            if (advanceSuppressionAnchorAfterDelivery) {
                add(
                    BaForegroundApReminderWrite(
                        kind = kind,
                        suppressionAnchorAtMs = nowMs,
                    ),
                )
            }
            if (clearDismissedUntilAfterDelivery) {
                add(
                    BaForegroundApReminderWrite(
                        kind = kind,
                        dismissedUntilAtMs = 0L,
                    ),
                )
            }
        }
    }
}
