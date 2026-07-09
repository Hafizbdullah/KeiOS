package os.kei.ui.page.main.ba

import android.content.Context
import android.content.pm.PackageManager
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.ui.page.main.ba.support.BaAccountId

internal object BaApNotificationDispatcher {
    private const val TAG = "BaApNotify"

    fun send(
        context: Context,
        currentDisplay: Int,
        limitDisplay: Int,
        thresholdDisplay: Int,
        notificationId: Int = McpNotificationHelper.BA_AP_NOTIFICATION_ID,
        accountDisplayName: String = "",
        accountId: BaAccountId? = null,
    ): Boolean {
        val notificationsGranted =
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        if (!notificationsGranted) {
            AppLogger.w(TAG) { "skip AP notification: permission missing id=$notificationId" }
            return false
        }

        return runCatching {
            val content = context.getString(
                R.string.mcp_notification_content_ap,
                currentDisplay,
                thresholdDisplay.toString(),
                limitDisplay,
            )
            McpNotificationHelper.notifyStandaloneEvent(
                context = context,
                notificationId = notificationId,
                serverName = LiveNotificationPayload.BA_AP_SERVER_NAME,
                running = true,
                port = currentDisplay,
                path = thresholdDisplay.toString(),
                clients = limitDisplay,
                overrideTitle = context.getString(R.string.ba_ap_notification_title),
                overrideContent = baAccountNotificationContent(
                    context = context,
                    accountDisplayName = accountDisplayName,
                    content = content,
                ),
                targetBaAccountId = accountId?.value,
            )
        }.onSuccess { sent ->
            AppLogger.i(TAG) {
                "send result=$sent id=$notificationId current=$currentDisplay limit=$limitDisplay account=${accountId?.value.orEmpty()}"
            }
        }.onFailure { throwable ->
            AppLogger.e(TAG, "send failed id=$notificationId", throwable)
        }.getOrDefault(false)
    }

    fun refreshIfActive(
        context: Context,
        currentDisplay: Int,
        limitDisplay: Int,
        thresholdDisplay: Int,
        notificationId: Int = McpNotificationHelper.BA_AP_NOTIFICATION_ID,
        accountDisplayName: String = "",
        accountId: BaAccountId? = null,
    ): Boolean {
        val notificationsGranted =
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        if (!notificationsGranted) {
            AppLogger.w(TAG) { "skip AP refresh: permission missing id=$notificationId" }
            return false
        }

        return runCatching {
            val content = context.getString(
                R.string.mcp_notification_content_ap,
                currentDisplay,
                thresholdDisplay.toString(),
                limitDisplay,
            )
            McpNotificationHelper.refreshStandaloneEventIfActive(
                context = context,
                notificationId = notificationId,
                serverName = LiveNotificationPayload.BA_AP_SERVER_NAME,
                running = true,
                port = currentDisplay,
                path = thresholdDisplay.toString(),
                clients = limitDisplay,
                ongoing = true,
                onlyAlertOnce = true,
                overrideTitle = context.getString(R.string.ba_ap_notification_title),
                overrideContent = baAccountNotificationContent(
                    context = context,
                    accountDisplayName = accountDisplayName,
                    content = content,
                ),
                targetBaAccountId = accountId?.value,
            )
        }.onSuccess { sent ->
            AppLogger.d(TAG) {
                "refresh result=$sent id=$notificationId current=$currentDisplay limit=$limitDisplay account=${accountId?.value.orEmpty()}"
            }
        }.onFailure { throwable ->
            AppLogger.e(TAG, "refresh failed id=$notificationId", throwable)
        }.getOrDefault(false)
    }
}
