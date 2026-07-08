package os.kei.core.notification.live

import android.app.Notification
import os.kei.core.notification.live.LiveNotificationPayload
import os.kei.core.notification.live.builder.NotificationRenderStyle

interface SessionNotifier {
    data class NotificationBuildResult(
        val notification: Notification,
        val style: NotificationRenderStyle,
        val useXiaomiMagic: Boolean
    )

    fun build(payload: LiveNotificationPayload): NotificationBuildResult
}

