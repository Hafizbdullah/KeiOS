package os.kei.ui.page.main.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.mcp.notification.McpAppIntentContract
import os.kei.mcp.notification.McpNotificationHelper
import kotlin.math.roundToInt

internal object WebDavSyncNotificationDispatcher {
    private const val TAG = "WebDavSyncNotify"
    private const val COLOR_RUNNING = "#2563EB"
    private const val COLOR_SUCCESS = "#22C55E"
    private const val COLOR_REVIEW = "#F59E0B"
    private const val COLOR_FAILED = "#E25B6A"
    private const val COLOR_SKIPPED = "#64748B"

    fun notifyStarted(
        context: Context,
        operation: WebDavSyncNotificationOperation,
        total: Int,
    ): Boolean =
        notifyState(
            context = context,
            operation = operation,
            status = WebDavSyncNotificationStatus.Running,
            current = 0,
            total = total,
            succeeded = 0,
            failed = 0,
            skipped = 0,
            onlyAlertOnce = false,
        )

    fun notifyProgress(
        context: Context,
        operation: WebDavSyncNotificationOperation,
        current: Int,
        total: Int,
        succeeded: Int,
        failed: Int,
        skipped: Int,
    ): Boolean =
        notifyState(
            context = context,
            operation = operation,
            status = WebDavSyncNotificationStatus.Running,
            current = current,
            total = total,
            succeeded = succeeded,
            failed = failed,
            skipped = skipped,
            onlyAlertOnce = true,
        )

    fun notifyFinished(
        context: Context,
        operation: WebDavSyncNotificationOperation,
        status: WebDavAutoSyncStatus,
        total: Int,
        succeeded: Int,
        failed: Int,
        skipped: Int,
    ): Boolean {
        val notificationStatus =
            when (status) {
                WebDavAutoSyncStatus.Success -> WebDavSyncNotificationStatus.Success
                WebDavAutoSyncStatus.NeedsReview -> WebDavSyncNotificationStatus.NeedsReview
                WebDavAutoSyncStatus.Failed -> WebDavSyncNotificationStatus.Failed
                WebDavAutoSyncStatus.Skipped -> WebDavSyncNotificationStatus.Skipped
                WebDavAutoSyncStatus.Running -> WebDavSyncNotificationStatus.Running
            }
        return notifyState(
            context = context,
            operation = operation,
            status = notificationStatus,
            current = total.coerceAtLeast(succeeded + failed + skipped),
            total = total,
            succeeded = succeeded,
            failed = failed,
            skipped = skipped,
            onlyAlertOnce = false,
        )
    }

    fun notifyFailed(
        context: Context,
        operation: WebDavSyncNotificationOperation,
        total: Int,
    ): Boolean =
        notifyState(
            context = context,
            operation = operation,
            status = WebDavSyncNotificationStatus.Failed,
            current = 0,
            total = total,
            succeeded = 0,
            failed = 1,
            skipped = 0,
            onlyAlertOnce = false,
        )

    fun cancel(context: Context) {
        McpNotificationHelper.cancelNotification(
            context = context,
            notificationId = McpNotificationHelper.WEBDAV_SYNC_NOTIFICATION_ID,
        )
    }

    private fun notifyState(
        context: Context,
        operation: WebDavSyncNotificationOperation,
        status: WebDavSyncNotificationStatus,
        current: Int,
        total: Int,
        succeeded: Int,
        failed: Int,
        skipped: Int,
        onlyAlertOnce: Boolean,
    ): Boolean {
        if (!canPostNotifications(context)) {
            AppLogger.w(TAG) { "skip WebDAV notification: permission missing operation=$operation status=$status" }
            return false
        }
        val safeTotal = total.coerceAtLeast(1)
        val safeCurrent = current.coerceIn(0, safeTotal)
        val statusLabel = status.label(context)
        val operationLabel = operation.label(context)
        val content =
            if (status == WebDavSyncNotificationStatus.Running) {
                context.getString(
                    R.string.webdav_sync_notification_running_content,
                    operationLabel,
                    safeCurrent,
                    safeTotal,
                )
            } else {
                context.getString(
                    R.string.webdav_sync_notification_terminal_content,
                    statusLabel,
                    succeeded.coerceAtLeast(0),
                    safeTotal,
                    failed.coerceAtLeast(0),
                    skipped.coerceAtLeast(0),
                )
            }
        val shortText =
            if (status == WebDavSyncNotificationStatus.Running) {
                "$safeCurrent/$safeTotal"
            } else {
                status.shortLabel(context)
            }
        return runCatching {
            McpNotificationHelper.notifyStandaloneEvent(
                context = context,
                notificationId = McpNotificationHelper.WEBDAV_SYNC_NOTIFICATION_ID,
                serverName = LiveNotificationPayload.WEBDAV_SYNC_SERVER_NAME,
                running = status == WebDavSyncNotificationStatus.Running,
                port = percent(safeCurrent, safeTotal),
                path = operation.name.lowercase(),
                clients = safeTotal,
                ongoing = status == WebDavSyncNotificationStatus.Running,
                onlyAlertOnce = onlyAlertOnce,
                primaryActionLabel = context.getString(R.string.webdav_sync_notification_action_open),
                secondaryActionLabel = context.getString(R.string.common_mark_read),
                showSecondaryActionWhenStopped = true,
                overrideTitle = context.getString(R.string.webdav_sync_notification_title, operationLabel),
                overrideContent = content,
                overrideOnlineText = if (status == WebDavSyncNotificationStatus.Running) {
                    operationLabel
                } else {
                    statusLabel
                },
                overrideShortText = shortText,
                overrideProgressPercent = percent(safeCurrent, safeTotal),
                overrideAccentColor = status.accentColor,
                targetRoute = McpAppIntentContract.TARGET_ROUTE_WEBDAV_SYNC,
                miFocusOrderId = "webdav-sync",
            )
        }.onSuccess { sent ->
            AppLogger.i(TAG) {
                "send result=$sent operation=$operation status=$status progress=$safeCurrent/$safeTotal " +
                    "success=$succeeded failed=$failed skipped=$skipped"
            }
        }.onFailure { error ->
            AppLogger.e(TAG, "send failed operation=$operation status=$status", error)
        }.getOrDefault(false)
    }

    private fun canPostNotifications(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun percent(current: Int, total: Int): Int {
        val safeTotal = total.coerceAtLeast(1)
        return ((current.coerceIn(0, safeTotal).toFloat() / safeTotal.toFloat()) * 100f)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun WebDavSyncNotificationOperation.label(context: Context): String =
        context.getString(
            when (this) {
                WebDavSyncNotificationOperation.RemoteProbe -> R.string.webdav_sync_notification_operation_refresh
                WebDavSyncNotificationOperation.Sync -> R.string.webdav_sync_notification_operation_sync
                WebDavSyncNotificationOperation.Upload -> R.string.webdav_sync_notification_operation_upload
                WebDavSyncNotificationOperation.Download -> R.string.webdav_sync_notification_operation_download
            }
        )

    private fun WebDavSyncNotificationStatus.label(context: Context): String =
        context.getString(
            when (this) {
                WebDavSyncNotificationStatus.Running -> R.string.webdav_sync_notification_status_running
                WebDavSyncNotificationStatus.Success -> R.string.webdav_sync_notification_status_success
                WebDavSyncNotificationStatus.NeedsReview -> R.string.webdav_sync_notification_status_review
                WebDavSyncNotificationStatus.Failed -> R.string.webdav_sync_notification_status_failed
                WebDavSyncNotificationStatus.Skipped -> R.string.webdav_sync_notification_status_skipped
            }
        )

    private fun WebDavSyncNotificationStatus.shortLabel(context: Context): String =
        context.getString(
            when (this) {
                WebDavSyncNotificationStatus.Running -> R.string.webdav_sync_notification_short_running
                WebDavSyncNotificationStatus.Success -> R.string.webdav_sync_notification_short_success
                WebDavSyncNotificationStatus.NeedsReview -> R.string.webdav_sync_notification_short_review
                WebDavSyncNotificationStatus.Failed -> R.string.webdav_sync_notification_short_failed
                WebDavSyncNotificationStatus.Skipped -> R.string.webdav_sync_notification_short_skipped
            }
        )

    private val WebDavSyncNotificationStatus.accentColor: String
        get() =
            when (this) {
                WebDavSyncNotificationStatus.Running -> COLOR_RUNNING
                WebDavSyncNotificationStatus.Success -> COLOR_SUCCESS
                WebDavSyncNotificationStatus.NeedsReview -> COLOR_REVIEW
                WebDavSyncNotificationStatus.Failed -> COLOR_FAILED
                WebDavSyncNotificationStatus.Skipped -> COLOR_SKIPPED
            }
}

internal enum class WebDavSyncNotificationOperation {
    RemoteProbe,
    Sync,
    Upload,
    Download,
}

private enum class WebDavSyncNotificationStatus {
    Running,
    Success,
    NeedsReview,
    Failed,
    Skipped,
}

internal fun WebDavBatchKind.toNotificationOperation(): WebDavSyncNotificationOperation =
    when (this) {
        WebDavBatchKind.Sync -> WebDavSyncNotificationOperation.Sync
        WebDavBatchKind.Upload -> WebDavSyncNotificationOperation.Upload
        WebDavBatchKind.Download -> WebDavSyncNotificationOperation.Download
    }
