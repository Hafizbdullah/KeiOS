package os.kei.feature.keepalive.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import os.kei.feature.keepalive.R

object AccessibilityGuardNotificationHelper {
    const val CHANNEL_ID = "accessibility_guard_service_channel_v1"
    const val FOREGROUND_NOTIFICATION_ID = 39887

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.accessibility_guard_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.accessibility_guard_notification_channel_description)
                setShowBadge(false)
            }
        manager.createNotificationChannel(channel)
    }

    fun buildForegroundNotification(context: Context): Notification {
        ensureChannel(context)
        val openPendingIntent =
            context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.let { intent ->
                    PendingIntent.getActivity(
                        context,
                        OPEN_APP_REQUEST_CODE,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_accessibility_guard_notification)
            .setContentTitle(context.getString(R.string.accessibility_guard_notification_title))
            .setContentText(context.getString(R.string.accessibility_guard_notification_content))
            .setContentIntent(openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private const val OPEN_APP_REQUEST_CODE = 39888
}
