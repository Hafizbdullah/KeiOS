package os.kei.ui.page.main.ba

import android.content.Context
import android.content.pm.PackageManager
import os.kei.R
import os.kei.core.log.AppLogger
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.ui.page.main.ba.support.BaAccountId
import os.kei.ui.page.main.ba.support.baServerLabelRes
import os.kei.ui.page.main.ba.support.serverRefreshTimeZone
import java.util.Calendar

internal object BaCafeVisitNotificationDispatcher {
    private const val TAG = "BaCafeVisitNotify"

    fun send(
        context: Context,
        serverIndex: Int,
        slotMs: Long,
        notificationId: Int = McpNotificationHelper.BA_CAFE_VISIT_NOTIFICATION_ID,
        accountDisplayName: String = "",
        accountId: BaAccountId? = null,
    ): Boolean {
        val notificationsGranted =
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        if (!notificationsGranted) {
            AppLogger.w(TAG) {
                "skip cafe visit notification: permission missing id=$notificationId server=$serverIndex"
            }
            return false
        }
        val detailLine = buildVisitDetailLine(
            context = context,
            serverIndex = serverIndex,
            slotMs = slotMs
        )

        return runCatching {
            McpNotificationHelper.notifyStandaloneEvent(
                context = context,
                notificationId = notificationId,
                serverName = LiveNotificationPayload.BA_CAFE_VISIT_SERVER_NAME,
                running = true,
                port = 0,
                path = detailLine,
                clients = 0,
                ongoing = false,
                overrideContent = baAccountNotificationContent(
                    context = context,
                    accountDisplayName = accountDisplayName,
                    content = detailLine,
                ),
                targetBaAccountId = accountId?.value,
            )
        }.onSuccess { sent ->
            AppLogger.i(TAG) {
                "send result=$sent id=$notificationId server=$serverIndex slot=$slotMs account=${accountId?.value.orEmpty()}"
            }
        }.onFailure { throwable ->
            AppLogger.e(TAG, "send failed id=$notificationId server=$serverIndex", throwable)
        }.getOrDefault(false)
    }

    private fun buildVisitDetailLine(
        context: Context,
        serverIndex: Int,
        slotMs: Long,
    ): String {
        val timeZone = serverRefreshTimeZone(serverIndex)
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = slotMs.coerceAtLeast(0L)
        }
        val slotHour = calendar.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
        return context.getString(
            R.string.ba_cafe_visit_notification_content_detail,
            context.getString(baServerLabelRes(serverIndex)),
            slotHour
        )
    }
}
