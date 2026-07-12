package os.kei.feature.github.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.toColorInt
import os.kei.MainActivity
import os.kei.R
import os.kei.core.notification.focus.MI_FOCUS_DEFAULT_BUSINESS
import os.kei.core.notification.focus.MiFocusExpandedComponent
import os.kei.core.notification.focus.MiFocusExpandedProgress
import os.kei.core.notification.focus.MiFocusExpandedSpec
import os.kei.core.notification.focus.MiFocusExpandedText
import os.kei.core.notification.focus.MiFocusIslandBigTemplate
import os.kei.core.notification.focus.MiFocusIslandPic
import os.kei.core.notification.focus.MiFocusIslandProgress
import os.kei.core.notification.focus.MiFocusIslandSmallTemplate
import os.kei.core.notification.focus.MiFocusIslandSpec
import os.kei.core.notification.focus.MiFocusIslandText
import os.kei.core.notification.focus.MiFocusNotificationAction
import os.kei.core.notification.focus.MiFocusNotificationSpec
import os.kei.core.notification.focus.MiFocusNotificationTemplate
import os.kei.core.notification.focus.MiFocusPictureRef
import os.kei.core.intent.PendingIntentLaunchOptionsCompat
import os.kei.core.log.AppLogger
import os.kei.core.notification.live.NotificationHelper
import os.kei.core.notification.live.builder.NotificationRenderStyle
import os.kei.core.prefs.SuperIslandFloatBehavior
import os.kei.core.prefs.UiPrefs
import os.kei.feature.github.domain.GitHubRefreshScope
import os.kei.feature.github.domain.GitHubRefreshSource
import os.kei.feature.notification.MiFocusNotificationActions
import os.kei.mcp.notification.McpNotificationHelper
import kotlin.math.roundToInt

object GitHubRefreshNotificationHelper {
    private const val TAG = "GitHubRefreshNotify"
    const val CHANNEL_ID = "github_refresh_channel_v2"
    const val NOTIFICATION_ID = 38990
    private const val LEGACY_DEFAULT_ISLAND_CHANNEL_ID = "github_refresh_island_channel_v1"
    private val ISLAND_ICON_RES_ID = R.drawable.ic_github_invertocat_island_blue
    private const val MI_COLOR_RUNNING = "#3B82F6"
    private const val MI_COLOR_SUCCESS = "#22C55E"
    private const val MI_COLOR_UPDATE = "#22C55E"
    private const val MI_COLOR_DANGER = "#E25B6A"
    private const val MI_COLOR_NEUTRAL = "#64748B"
    private const val MI_COLOR_NEUTRAL_DARK = "#CBD5E1"
    private const val MI_PROGRESS_TRACK_COLOR = "#334155"
    private const val MI_LABEL_TEXT_COLOR = "#FFFFFF"
    private const val RUNNING_PROGRESS_FLOOR = 6
    private const val RUNNING_PROGRESS_CEILING = 96

    private val progressLock = Any()
    private var activeNotificationSessionId = 0L
    private var lastRunningTotal = 0
    private var lastDisplayedProgressPercent = 0

    private data class NotificationBuildResult(
        val notification: Notification,
        val style: RenderStyle,
        val useXiaomiMagic: Boolean
    )

    private data class SuperIslandFloatPolicy(
        val behavior: SuperIslandFloatBehavior,
        val firstFloat: Boolean,
        val allowFloat: Boolean
    )

    private data class MiIslandRefreshStyle(
        val accentColor: String,
        val contentColor: String = MI_COLOR_NEUTRAL,
        val contentColorDark: String = MI_COLOR_NEUTRAL_DARK,
        val progressColor: String = accentColor,
        val progressTrackColor: String = MI_PROGRESS_TRACK_COLOR,
        val highlightTerminalText: Boolean = true
    )

    private data class RefreshState(
        val current: Int,
        val total: Int,
        val preReleaseUpdateCount: Int,
        val updatableCount: Int,
        val failedCount: Int,
        val running: Boolean,
        val cancelled: Boolean,
        val displayProgressPercent: Int,
        val sessionId: Long,
        val scope: GitHubRefreshScope,
        val source: GitHubRefreshSource,
        val totalTrackedCount: Int
    ) {
        val safeTotal: Int = total.coerceAtLeast(1)
        val safeCurrent: Int = current.coerceIn(0, safeTotal)
        val safeTotalTrackedCount: Int = totalTrackedCount.coerceAtLeast(safeTotal)
        val progressPercent: Int =
            displayProgressPercent.coerceIn(0, 100)

        val shortText: String
            get() = "$safeCurrent/$safeTotal"

        val targetText: String
            get() = "$safeCurrent/$safeTotal"

        val totalUpdateCount: Int
            get() = (preReleaseUpdateCount + updatableCount).coerceAtLeast(0)
    }

    private enum class RenderStyle {
        MI_ISLAND,
        LIVE_UPDATE
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(LEGACY_DEFAULT_ISLAND_CHANNEL_ID) != null) {
            manager.deleteNotificationChannel(LEGACY_DEFAULT_ISLAND_CHANNEL_ID)
        }
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.github_refresh_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.github_refresh_channel_desc)
                setShowBadge(false)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyProgress(
        context: Context,
        current: Int,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int,
        sessionId: Long = 0L,
        scope: GitHubRefreshScope = GitHubRefreshScope.AllTracked,
        source: GitHubRefreshSource = GitHubRefreshSource.Page,
        totalTrackedCount: Int = total
    ): Boolean {
        val safeTotal = total.coerceAtLeast(1)
        val safeCurrent = current.coerceIn(0, safeTotal)
        val isComplete = total > 0 && safeCurrent >= safeTotal
        val displayProgressPercent = resolveDisplayProgressPercent(
            sessionId = sessionId,
            current = safeCurrent,
            total = safeTotal,
            running = !isComplete,
            cancelled = false
        ) ?: return false
        return notifyInternal(
            context = context,
            state = RefreshState(
                current = safeCurrent,
                total = safeTotal,
                preReleaseUpdateCount = preReleaseUpdateCount,
                updatableCount = updatableCount,
                failedCount = failedCount,
                running = !isComplete,
                cancelled = false,
                displayProgressPercent = displayProgressPercent,
                sessionId = sessionId,
                scope = scope,
                source = source,
                totalTrackedCount = totalTrackedCount
            ),
            onlyAlertOnce = true
        )
    }

    fun notifyCompleted(
        context: Context,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int,
        sessionId: Long = 0L,
        scope: GitHubRefreshScope = GitHubRefreshScope.AllTracked,
        source: GitHubRefreshSource = GitHubRefreshSource.Page,
        totalTrackedCount: Int = total
    ): Boolean {
        val displayProgressPercent = resolveDisplayProgressPercent(
            sessionId = sessionId,
            current = total,
            total = total,
            running = false,
            cancelled = false
        ) ?: return false
        return notifyInternal(
            context = context,
            state = RefreshState(
                current = total,
                total = total,
                preReleaseUpdateCount = preReleaseUpdateCount,
                updatableCount = updatableCount,
                failedCount = failedCount,
                running = false,
                cancelled = false,
                displayProgressPercent = displayProgressPercent,
                sessionId = sessionId,
                scope = scope,
                source = source,
                totalTrackedCount = totalTrackedCount
            ),
            onlyAlertOnce = true
        )
    }

    fun notifyFailed(
        context: Context,
        current: Int,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int,
        sessionId: Long = 0L,
        scope: GitHubRefreshScope = GitHubRefreshScope.AllTracked,
        source: GitHubRefreshSource = GitHubRefreshSource.Page,
        totalTrackedCount: Int = total
    ): Boolean {
        val safeTotal = total.coerceAtLeast(1)
        val safeCurrent = current.coerceIn(0, safeTotal)
        val displayProgressPercent = resolveFailedDisplayProgressPercent(
            sessionId = sessionId,
            current = safeCurrent,
            total = safeTotal,
        ) ?: return false
        return notifyInternal(
            context = context,
            state = RefreshState(
                current = safeCurrent,
                total = safeTotal,
                preReleaseUpdateCount = preReleaseUpdateCount,
                updatableCount = updatableCount,
                failedCount = failedCount.coerceAtLeast(1),
                running = false,
                cancelled = false,
                displayProgressPercent = displayProgressPercent,
                sessionId = sessionId,
                scope = scope,
                source = source,
                totalTrackedCount = totalTrackedCount
            ),
            onlyAlertOnce = true
        )
    }

    fun notifyCancelled(
        context: Context,
        current: Int,
        total: Int,
        preReleaseUpdateCount: Int,
        updatableCount: Int,
        failedCount: Int,
        sessionId: Long = 0L,
        scope: GitHubRefreshScope = GitHubRefreshScope.AllTracked,
        source: GitHubRefreshSource = GitHubRefreshSource.Page,
        totalTrackedCount: Int = total
    ): Boolean {
        val displayProgressPercent = resolveDisplayProgressPercent(
            sessionId = sessionId,
            current = current,
            total = total,
            running = false,
            cancelled = true
        ) ?: return false
        return notifyInternal(
            context = context,
            state = RefreshState(
                current = current,
                total = total,
                preReleaseUpdateCount = preReleaseUpdateCount,
                updatableCount = updatableCount,
                failedCount = failedCount,
                running = false,
                cancelled = true,
                displayProgressPercent = displayProgressPercent,
                sessionId = sessionId,
                scope = scope,
                source = source,
                totalTrackedCount = totalTrackedCount
            ),
            onlyAlertOnce = true
        )
    }

    fun cancel(context: Context) {
        AppLogger.d(TAG) { "cancel notification id=$NOTIFICATION_ID" }
        resetNotificationRuntime()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun cancel(
        context: Context,
        sessionId: Long,
    ): Boolean = synchronized(progressLock) {
        if (sessionId <= 0L || activeNotificationSessionId != sessionId) {
            return@synchronized false
        }
        AppLogger.d(TAG) { "cancel notification id=$NOTIFICATION_ID session=$sessionId" }
        activeNotificationSessionId = 0L
        resetDisplayProgressLocked()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        true
    }

    private fun resolveDisplayProgressPercent(
        sessionId: Long,
        current: Int,
        total: Int,
        running: Boolean,
        cancelled: Boolean
    ): Int? = synchronized(progressLock) {
        if (!activateNotificationSessionLocked(sessionId)) return@synchronized null
        val safeTotal = total.coerceAtLeast(1)
        val safeCurrent = current.coerceIn(0, safeTotal)
        if (cancelled) {
            val cancelledProgress = lastDisplayedProgressPercent.coerceIn(0, RUNNING_PROGRESS_CEILING)
            resetDisplayProgressLocked()
            return@synchronized cancelledProgress
        }
        if (!running) {
            resetDisplayProgressLocked()
            return@synchronized 100
        }
        if (safeTotal != lastRunningTotal || safeCurrent == 0) {
            lastRunningTotal = safeTotal
            lastDisplayedProgressPercent = 0
        }
        val rawProgress = ((safeCurrent.toFloat() / safeTotal.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(RUNNING_PROGRESS_FLOOR, RUNNING_PROGRESS_CEILING)
        val smoothedProgress = rawProgress
            .coerceAtLeast(lastDisplayedProgressPercent)
            .coerceAtMost(RUNNING_PROGRESS_CEILING)
        lastDisplayedProgressPercent = smoothedProgress
        smoothedProgress
    }

    private fun resetNotificationRuntime() = synchronized(progressLock) {
        activeNotificationSessionId = 0L
        resetDisplayProgressLocked()
    }

    private fun resolveFailedDisplayProgressPercent(
        sessionId: Long,
        current: Int,
        total: Int,
    ): Int? = synchronized(progressLock) {
        if (!activateNotificationSessionLocked(sessionId)) return@synchronized null
        val safeTotal = total.coerceAtLeast(1)
        val safeCurrent = current.coerceIn(0, safeTotal)
        resetDisplayProgressLocked()
        ((safeCurrent.toFloat() / safeTotal.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun activateNotificationSessionLocked(sessionId: Long): Boolean {
        if (sessionId <= 0L) return true
        if (activeNotificationSessionId != 0L && sessionId < activeNotificationSessionId) {
            return false
        }
        if (activeNotificationSessionId != sessionId) {
            activeNotificationSessionId = sessionId
            resetDisplayProgressLocked()
        }
        return true
    }

    private fun resetDisplayProgressLocked() {
        lastRunningTotal = 0
        lastDisplayedProgressPercent = 0
    }

    private fun resolveTitle(context: Context, state: RefreshState): String {
        return when {
            state.running -> context.getString(R.string.github_refresh_title_running)
            state.cancelled -> context.getString(R.string.github_refresh_title_cancelled)
            state.failedCount > 0 && state.safeCurrent < state.safeTotal ->
                context.getString(R.string.github_refresh_title_failed)

            state.failedCount > 0 ->
                context.getString(R.string.github_refresh_title_partial_failed)

            else -> context.getString(R.string.github_refresh_title_completed)
        }
    }

    private fun resolveContent(context: Context, state: RefreshState): String {
        val body = if (state.failedCount > 0) {
            context.getString(
                R.string.github_refresh_content_with_failed,
                state.safeCurrent,
                state.safeTotal,
                state.preReleaseUpdateCount,
                state.updatableCount,
                state.failedCount
            )
        } else {
            context.getString(
                R.string.github_refresh_content,
                state.safeCurrent,
                state.safeTotal,
                state.preReleaseUpdateCount,
                state.updatableCount
            )
        }
        return context.getString(
            R.string.github_refresh_content_scoped,
            resolveScopeText(context, state, compact = false),
            body
        )
    }

    private fun resolveMiIslandTitle(context: Context, state: RefreshState): String {
        return when {
            state.running -> context.getString(R.string.github_refresh_mi_title_running)
            state.cancelled -> context.getString(R.string.github_refresh_mi_title_cancelled)
            state.failedCount > 0 && state.safeCurrent < state.safeTotal ->
                context.getString(R.string.github_refresh_mi_title_failed)

            state.failedCount > 0 ->
                context.getString(R.string.github_refresh_mi_title_partial_failed)

            else -> context.getString(R.string.github_refresh_mi_title_completed)
        }
    }

    private fun resolveMiIslandContent(context: Context, state: RefreshState): String {
        val scope = resolveScopeText(context, state, compact = true)
        val progress = resolveCompactFractionText(context, state)
        return when {
            state.failedCount > 0 && state.totalUpdateCount > 0 ->
                context.getString(
                    R.string.github_refresh_mi_content_failed,
                    scope,
                    progress,
                    state.totalUpdateCount,
                    state.failedCount
                )

            state.failedCount > 0 ->
                context.getString(
                    R.string.github_refresh_mi_content_failed_only,
                    scope,
                    progress,
                    state.failedCount
                )

            state.totalUpdateCount > 0 ->
                context.getString(
                    R.string.github_refresh_mi_content,
                    scope,
                    progress,
                    state.totalUpdateCount
                )

            else ->
                context.getString(
                    R.string.github_refresh_mi_content_base,
                    scope,
                    progress
                )
        }
    }

    private fun resolveMiIslandStatusLabel(context: Context, state: RefreshState): String {
        return when {
            state.running -> context.getString(R.string.github_refresh_mi_label_running)
            state.cancelled -> context.getString(R.string.github_refresh_mi_label_cancelled)
            state.failedCount > 0 -> context.getString(
                R.string.github_refresh_mi_label_failed,
                state.failedCount
            )

            state.totalUpdateCount > 0 -> context.getString(
                R.string.github_refresh_mi_label_updates,
                state.totalUpdateCount
            )

            else -> context.getString(R.string.github_refresh_mi_label_completed)
        }
    }

    private fun resolveMiIslandRefreshStyle(state: RefreshState): MiIslandRefreshStyle {
        return when {
            state.running -> MiIslandRefreshStyle(
                accentColor = MI_COLOR_RUNNING,
                highlightTerminalText = false
            )

            state.cancelled -> MiIslandRefreshStyle(
                accentColor = MI_COLOR_NEUTRAL,
                highlightTerminalText = false
            )

            state.failedCount > 0 -> MiIslandRefreshStyle(
                accentColor = MI_COLOR_DANGER
            )

            state.totalUpdateCount > 0 -> MiIslandRefreshStyle(
                accentColor = MI_COLOR_UPDATE
            )

            else -> MiIslandRefreshStyle(
                accentColor = MI_COLOR_SUCCESS
            )
        }
    }

    private fun resolveScopeText(
        context: Context,
        state: RefreshState,
        compact: Boolean
    ): String {
        return when (state.scope) {
            GitHubRefreshScope.AllTracked ->
                context.getString(
                    if (compact) R.string.github_refresh_scope_all_compact else R.string.github_refresh_scope_all,
                    state.safeTotalTrackedCount
                )

            GitHubRefreshScope.DueTracked ->
                context.getString(
                    if (compact) R.string.github_refresh_scope_due_compact else R.string.github_refresh_scope_due,
                    state.safeTotal
                )

            GitHubRefreshScope.VisibleTracked ->
                context.getString(
                    if (compact) R.string.github_refresh_scope_visible_compact else R.string.github_refresh_scope_visible,
                    state.safeTotal
                )

            GitHubRefreshScope.RequestedTracked ->
                context.getString(
                    if (compact) R.string.github_refresh_scope_requested_compact else R.string.github_refresh_scope_requested,
                    state.safeTotal
                )

            GitHubRefreshScope.MissingCache ->
                context.getString(
                    if (compact) R.string.github_refresh_scope_missing_compact else R.string.github_refresh_scope_missing,
                    state.safeTotal
                )

            GitHubRefreshScope.SingleTracked ->
                context.getString(R.string.github_refresh_scope_single)

            GitHubRefreshScope.ShortcutAllTracked ->
                context.getString(
                    if (compact) {
                        R.string.github_refresh_scope_shortcut_all_compact
                    } else {
                        R.string.github_refresh_scope_shortcut_all
                    },
                    state.safeTotalTrackedCount
                )
        }
    }

    private fun resolveCompactProgressText(context: Context, state: RefreshState): String {
        return context.getString(R.string.github_refresh_progress_percent, state.progressPercent)
    }

    private fun resolveCompactFractionText(context: Context, state: RefreshState): String {
        return context.getString(
            R.string.github_refresh_progress_fraction,
            state.safeCurrent,
            state.safeTotal
        )
    }

    private fun resolveCompactStateContent(context: Context, state: RefreshState): String? {
        return when {
            state.cancelled -> context.getString(R.string.github_refresh_island_cancelled)
            state.failedCount > 0 ->
                context.getString(R.string.github_refresh_failed_short_with_count, state.failedCount)
            else -> null
        }
    }

    private fun resolveCompactStateTitle(context: Context, state: RefreshState): String {
        return when {
            state.cancelled -> context.getString(R.string.github_refresh_island_cancelled)
            state.failedCount > 0 ->
                context.getString(
                    R.string.github_refresh_failed_short_with_count,
                    state.failedCount
                )

            else -> context.getString(R.string.github_refresh_island_completed)
        }
    }

    private fun notifyInternal(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ): Boolean {
        ensureChannel(context)
        val buildResult = buildNotification(
            context = context,
            state = state,
            onlyAlertOnce = onlyAlertOnce
        )
        AppLogger.d(TAG) {
            "dispatch session=${state.sessionId} id=$NOTIFICATION_ID style=${buildResult.style} " +
                "xiaomiMagic=${buildResult.useXiaomiMagic} running=${state.running} " +
                "cancelled=${state.cancelled} scope=${state.scope} source=${state.source} " +
                "progress=${state.safeCurrent}/${state.safeTotal} display=${state.progressPercent} " +
                "totalTracked=${state.safeTotalTrackedCount} updatable=${state.updatableCount} " +
                "prerelease=${state.preReleaseUpdateCount} failed=${state.failedCount}"
        }
        val dispatched =
            synchronized(progressLock) {
                if (!isCurrentNotificationSessionLocked(state.sessionId)) {
                    AppLogger.i(
                        TAG,
                        "skip stale notification dispatch session=${state.sessionId} " +
                            "active=$activeNotificationSessionId",
                    )
                    false
                } else {
                    McpNotificationHelper.dispatchNotification(
                        context = context,
                        notificationId = NOTIFICATION_ID,
                        notification = buildResult.notification,
                        useXiaomiMagic =
                            buildResult.style == RenderStyle.MI_ISLAND && buildResult.useXiaomiMagic,
                    )
                }
            }
        if (!dispatched) {
            AppLogger.w(
                TAG,
                "dispatch failed session=${state.sessionId} running=${state.running} " +
                    "scope=${state.scope} source=${state.source} progress=${state.safeCurrent}/${state.safeTotal}"
            )
        }
        AppLogger.d(TAG) {
            "dispatchResult session=${state.sessionId} id=$NOTIFICATION_ID dispatched=$dispatched " +
                "style=${buildResult.style} xiaomiMagic=${buildResult.useXiaomiMagic}"
        }
        return dispatched
    }

    private fun isCurrentNotificationSessionLocked(sessionId: Long): Boolean =
        if (sessionId > 0L) {
            activeNotificationSessionId == sessionId
        } else {
            activeNotificationSessionId == 0L
        }

    private fun buildNotification(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ): NotificationBuildResult {
        val helper = NotificationHelper(context)
        val preferSuperIsland = UiPrefs.isSuperIslandNotificationEnabled(defaultValue = false)
        val bypassRestriction = UiPrefs.isSuperIslandBypassRestrictionEnabled(defaultValue = false)
        val decision = helper.resolveRenderDecision(
            preferSuperIsland = preferSuperIsland,
            bypassRestriction = bypassRestriction,
        )
        val style = if (decision.style == NotificationRenderStyle.MI_ISLAND) {
            RenderStyle.MI_ISLAND
        } else {
            RenderStyle.LIVE_UPDATE
        }
        AppLogger.i(
            TAG,
            "buildNotification ${decision.logSummary()}"
        )
        AppLogger.d(TAG) {
            val floatBehavior = GitHubNotificationPreferences.superIslandFloatBehavior()
            "buildNotificationDetail ${decision.logSummary()} channel=$CHANNEL_ID " +
                "session=${state.sessionId} scope=${state.scope} source=${state.source} " +
                "running=${state.running} floatBehavior=${floatBehavior.storageId} " +
                "firstFloat=${floatBehavior.firstFloatEnabled} finishFloat=${floatBehavior.finishFloatEnabled} " +
                "modernLiveUpdate=${helper.isModernLiveUpdateEligible}"
        }
        val notification = when (style) {
            RenderStyle.MI_ISLAND -> buildMiIslandNotification(context, state, onlyAlertOnce)
            RenderStyle.LIVE_UPDATE -> {
                if (helper.isModernLiveUpdateEligible) {
                    buildModernLiveUpdateNotification(context, state, onlyAlertOnce)
                } else {
                    buildLegacyLiveUpdateNotification(context, state, onlyAlertOnce)
                }
            }
        }
        return NotificationBuildResult(
            notification = notification,
            style = style,
            useXiaomiMagic = decision.useXiaomiMagic
        )
    }

    private fun buildModernLiveUpdateNotification(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ): Notification {
        val title = resolveTitle(context, state)
        val content = resolveContent(context, state)
        val openPendingIntent = buildOpenPendingIntent(context)
        val readPendingIntent = buildMarkReadPendingIntent(context)
        val progressColor = if (state.running) 0xFF2E7D32.toInt() else 0xFF64748B.toInt()
        val progressStyle = NotificationCompat.ProgressStyle()
            .setProgressSegments(
                listOf(NotificationCompat.ProgressStyle.Segment(100).setColor(progressColor))
            )
            .setStyledByProgress(true)
            .setProgress(state.progressPercent)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(ISLAND_ICON_RES_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openPendingIntent)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setSilent(true)
            .setOngoing(state.running)
            .setRequestPromotedOngoing(state.running)
            .setStyle(progressStyle)
            .setShortCriticalText(state.targetText)
            .setDeleteIntent(readPendingIntent)
            .addAction(0, context.getString(R.string.common_open), openPendingIntent)
            .addAction(0, context.getString(R.string.common_acknowledge), readPendingIntent)
            .build()
    }

    private fun buildLegacyLiveUpdateNotification(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ): Notification {
        val title = resolveTitle(context, state)
        val content = resolveContent(context, state)
        val openPendingIntent = buildOpenPendingIntent(context)
        val readPendingIntent = buildMarkReadPendingIntent(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(ISLAND_ICON_RES_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(
                context.getString(
                    R.string.common_progress_with_value,
                    state.targetText
                )
            )
            .setContentIntent(openPendingIntent)
            .setCategory(
                if (state.running) NotificationCompat.CATEGORY_PROGRESS
                else NotificationCompat.CATEGORY_STATUS
            )
            .setColorized(true)
            .setColor(0xFF2563EB.toInt())
            .setOngoing(state.running)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setAutoCancel(false)
            .setSilent(onlyAlertOnce)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setDeleteIntent(readPendingIntent)
            .setProgress(100, state.progressPercent, false)
            .addAction(0, context.getString(R.string.common_open), openPendingIntent)
            .addAction(0, context.getString(R.string.common_acknowledge), readPendingIntent)
            .build()
    }

    private fun buildMiIslandNotification(
        context: Context,
        state: RefreshState,
        onlyAlertOnce: Boolean
    ): Notification {
        val iconResId = ISLAND_ICON_RES_ID
        val title = resolveMiIslandTitle(context, state)
        val content = resolveMiIslandContent(context, state)
        val refreshStyle = resolveMiIslandRefreshStyle(state)
        val openPendingIntent = buildOpenPendingIntent(context)
        val readPendingIntent = buildMarkReadPendingIntent(context)
        val shortCriticalText = if (state.running) {
            resolveCompactProgressText(context, state)
        } else {
            resolveCompactFractionText(context, state)
        }
        val baseBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconResId)
            .setContentTitle(title)
            .setContentText(content.ifBlank { " " })
            .setContentIntent(openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColorized(!state.running)
            .setColor(refreshStyle.accentColor.toColorInt())
            .setOngoing(state.running)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setAutoCancel(false)
            .setShortCriticalText(shortCriticalText)
            .setDeleteIntent(readPendingIntent)

        buildMiIslandFocusExtras(
            context = context,
            state = state,
            title = title,
            content = content,
            iconResId = iconResId,
            focusOpenPendingIntent = buildFocusOpenPendingIntent(context),
            markReadPendingIntent = readPendingIntent
        )?.let(baseBuilder::addExtras)
        return baseBuilder.build()
    }

    private fun buildMiIslandFocusExtras(
        context: Context,
        state: RefreshState,
        title: String,
        content: String,
        iconResId: Int,
        focusOpenPendingIntent: PendingIntent,
        markReadPendingIntent: PendingIntent
    ) = runCatching {
        val floatPolicy = resolveSuperIslandFloatPolicy(state)
        val progressPercent = state.progressPercent.coerceIn(0, 100)
        val progressText = resolveCompactProgressText(context, state)
        val fractionText = resolveCompactFractionText(context, state)
        val compactStateTitle = resolveCompactStateTitle(context, state)
        val compactStateContent = if (state.cancelled || state.failedCount > 0) {
            fractionText
        } else {
            resolveCompactStateContent(context, state) ?: fractionText
        }
        val refreshStyle = resolveMiIslandRefreshStyle(state)
        val statusLabel = resolveMiIslandStatusLabel(context, state)
        val progress = MiFocusIslandProgress(
            progressPercent = progressPercent,
            colorReach = refreshStyle.progressColor,
            colorUnReach = refreshStyle.progressTrackColor,
        )
        val islandBigTemplates = buildList {
            add(
                MiFocusIslandBigTemplate.ImageTextLeft(
                    pic = MiFocusIslandPic(pic = MiFocusPictureRef.Display),
                )
            )
            if (state.running) {
                add(
                    MiFocusIslandBigTemplate.ProgressText(
                        text = MiFocusIslandText(
                            title = progressText,
                            content = fractionText.takeIf { it != progressText },
                        ),
                        progress = progress,
                    )
                )
            } else {
                add(
                    MiFocusIslandBigTemplate.ImageTextRight(
                        type = 3,
                        text = MiFocusIslandText(
                            title = compactStateTitle,
                            content = compactStateContent,
                            showHighlightColor = refreshStyle.highlightTerminalText,
                        ),
                    )
                )
            }
        }
        val expandedComponents = buildList {
            add(
                MiFocusExpandedComponent.Base(
                    text = MiFocusExpandedText(
                        title = title,
                        content = content.ifBlank { " " },
                        specialTitle = statusLabel,
                        colorTitle = refreshStyle.accentColor,
                        colorTitleDark = refreshStyle.accentColor,
                        colorSpecialTitle = MI_LABEL_TEXT_COLOR,
                        colorSpecialTitleDark = MI_LABEL_TEXT_COLOR,
                        colorSpecialBg = refreshStyle.accentColor,
                        colorContent = refreshStyle.contentColor,
                        colorContentDark = refreshStyle.contentColorDark,
                    )
                )
            )
            if (state.running) {
                add(
                    MiFocusExpandedComponent.Progress(
                        progress =
                            MiFocusExpandedProgress(
                                progressPercent = progressPercent,
                                colorReach = refreshStyle.progressColor,
                                colorEnd = refreshStyle.progressColor,
                            ),
                    )
                )
            }
            add(
                MiFocusExpandedComponent.Picture(
                    pic = MiFocusPictureRef.Expanded,
                    picDark = MiFocusPictureRef.Display,
                    type = 1,
                )
            )
            if (!state.running) {
                add(
                    MiFocusExpandedComponent.TextButtons(
                        actions = listOf(
                            MiFocusNotificationAction(
                                key = "github_action_open",
                                title = context.getString(R.string.common_open),
                                pendingIntent = focusOpenPendingIntent,
                                iconResId = iconResId,
                                isHighlighted = true,
                            ),
                            MiFocusNotificationAction(
                                key = "github_action_read",
                                title = context.getString(R.string.common_acknowledge),
                                pendingIntent = markReadPendingIntent,
                                iconResId = iconResId,
                            ),
                        )
                    )
                )
            }
        }
        AppLogger.d(TAG) {
            "buildMiIslandExtras session=${state.sessionId} id=$NOTIFICATION_ID " +
                "running=${state.running} behavior=${floatPolicy.behavior.storageId} " +
                "allowFloat=${floatPolicy.allowFloat} firstFloat=${floatPolicy.firstFloat} " +
                "timeoutMinutes=${if (state.running) 6 else 1} progress=$progressPercent " +
                "scope=${state.scope} source=${state.source}"
        }
        MiFocusNotificationTemplate.build(
            context = context,
            spec = MiFocusNotificationSpec(
                title = title,
                content = content.ifBlank { " " },
                displayIconResId = iconResId,
                expandedIconResId = iconResId,
                actionIconResId = iconResId,
                tickerIconResId = iconResId,
                island = MiFocusIslandSpec(
                    bigTemplates = islandBigTemplates,
                    smallTemplate =
                        if (state.running) {
                            MiFocusIslandSmallTemplate.CombinePic(
                                pic = MiFocusIslandPic(pic = MiFocusPictureRef.Display),
                                progress = progress,
                            )
                        } else {
                            MiFocusIslandSmallTemplate.Picture(
                                pic = MiFocusIslandPic(pic = MiFocusPictureRef.Display),
                            )
                        },
                    highlightColor = refreshStyle.accentColor,
                ),
                expanded = MiFocusExpandedSpec(components = expandedComponents),
                allowFloat = floatPolicy.allowFloat,
                islandFirstFloat = floatPolicy.firstFloat,
                updatable = true,
                showNotification = true,
                timeoutMinutes = if (state.running) 6 else 1,
                outerGlow = true,
                business = MI_FOCUS_DEFAULT_BUSINESS,
                notifyId = NOTIFICATION_ID.toString(),
                orderId = "github_refresh",
                ticker = title,
                compactTicker = title,
            )
        )
    }.onFailure {
        AppLogger.e(TAG, "Build local Focus protocol extras failed", it)
    }.getOrNull()

    private fun resolveSuperIslandFloatPolicy(state: RefreshState): SuperIslandFloatPolicy {
        val behavior = GitHubNotificationPreferences.superIslandFloatBehavior()
        return SuperIslandFloatPolicy(
            behavior = behavior,
            firstFloat = behavior.firstFloatEnabled,
            allowFloat = !state.running && behavior.finishFloatEnabled,
        )
    }

    private fun buildOpenPendingIntent(context: Context): PendingIntent {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE, MainActivity.TARGET_BOTTOM_PAGE_GITHUB)
        }
        return PendingIntentLaunchOptionsCompat.getUserVisibleActivity(
            context,
            2001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildFocusOpenPendingIntent(context: Context): PendingIntent {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(MainActivity.EXTRA_TARGET_BOTTOM_PAGE, MainActivity.TARGET_BOTTOM_PAGE_GITHUB)
        }
        return PendingIntent.getActivity(
            context,
            4201,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildMarkReadPendingIntent(context: Context): PendingIntent =
        MiFocusNotificationActions.markReadPendingIntent(
            context = context,
            notificationId = NOTIFICATION_ID,
            requestCode = 2002
        )
}
