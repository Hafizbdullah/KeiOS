package os.kei.mcp.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import os.kei.core.log.AppLogger

data class McpStandaloneEventRequest(
    val serverName: String,
    val running: Boolean,
    val port: Int,
    val path: String,
    val clients: Int,
    val ongoing: Boolean = running,
    val onlyAlertOnce: Boolean = false,
    val primaryActionLabel: String? = null,
    val secondaryActionLabel: String? = null,
    val showSecondaryActionWhenStopped: Boolean = false,
    val outerGlow: Boolean = true,
    val overrideTitle: String? = null,
    val overrideContent: String? = null,
    val overrideOnlineText: String? = null,
    val overrideShortText: String? = null,
    val overrideProgressPercent: Int? = null,
    val overrideAccentColor: String? = null,
    val deadlineAtMs: Long? = null,
    val miFocusOrderId: String? = null,
    val targetBaAccountId: String? = null,
    val targetRoute: String? = null,
)

internal data class PreparedStandaloneEvent(
    val notification: Notification,
    val snapshot: McpNotificationSnapshot,
    val useXiaomiMagic: Boolean,
    val alreadyActive: Boolean,
)

internal suspend fun dispatchStandaloneEventAwaitingDelivery(
    context: Context,
    notificationId: Int,
    notification: Notification,
    useXiaomiMagic: Boolean,
): Boolean {
    val dispatched =
        if (useXiaomiMagic) {
            McpXiaomiMagicDispatcher.notify(
                context = context,
                notificationId = notificationId,
                notification = notification,
            )
        } else {
            val notificationManager = NotificationManagerCompat.from(context)
            if (McpXiaomiMagicDispatcher.canUseCommand()) {
                McpNotificationHelper.restoreXiaomiNetworkIfNeeded(context)
            }
            McpNotificationHelper.notifySafely(
                context,
                notificationManager,
                notificationId,
                notification,
            )
        }
    if (dispatched) {
        McpNotificationActiveStateCache.markActive(notificationId, active = true)
    }
    AppLogger.i("McpStandaloneEvent") {
        "awaited dispatch result=$dispatched id=$notificationId xiaomiMagic=$useXiaomiMagic"
    }
    return dispatched
}
