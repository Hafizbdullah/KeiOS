package os.kei.ui.page.main.ba

import android.content.Context
import android.content.pm.PackageManager
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.mcp.notification.McpNotificationHelper

/**
 * Reports the result of a daily-done run that was triggered from a quick-settings tile.
 *
 * A notification rather than a toast, because neither of the cheaper channels actually works from a tile:
 * Android 12 and up suppress toasts posted by a background app, and a tile click does not count as
 * foreground; and a tile added at the icon-only size renders neither its label nor its subtitle, so
 * writing the outcome there would be invisible too.
 *
 * One fixed id, deliberately not per-account: the message summarises the whole run, so a second run
 * should replace the first rather than stack.
 */
internal object BaDailyDoneNotificationDispatcher {
    private const val TAG = "BaDailyNotify"

    /** Well clear of the per-account (43k..443k) and craft (500k..1.1M) id ranges. */
    private const val NOTIFICATION_ID = 1_200_000

    fun send(
        context: Context,
        changedAccounts: Int,
        craftSlotsStarted: Int,
    ): Boolean {
        val granted =
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) {
            AppLogger.w(TAG) { "skip daily-done notification: permission missing" }
            return false
        }
        val detail =
            context.getString(
                R.string.ba_daily_done_toast_applied_format,
                changedAccounts,
                craftSlotsStarted,
            )
        return runCatching {
            McpNotificationHelper.notifyStandaloneEvent(
                context = context,
                notificationId = NOTIFICATION_ID,
                serverName = LiveNotificationPayload.BA_DAILY_DONE_SERVER_NAME,
                running = true,
                port = 0,
                path = detail,
                clients = 0,
                ongoing = false,
                overrideContent = detail,
            )
        }.onFailure { throwable ->
            AppLogger.e(TAG, "daily-done notification failed", throwable)
        }.getOrDefault(false)
    }
}
