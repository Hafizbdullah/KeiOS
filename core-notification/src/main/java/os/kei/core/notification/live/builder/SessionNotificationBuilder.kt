package os.kei.core.notification.live.builder

import android.app.Notification

interface SessionNotificationBuilder {
    fun build(payload: NotificationPayload): Notification
}
