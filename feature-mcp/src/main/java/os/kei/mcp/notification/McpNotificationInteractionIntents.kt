package os.kei.mcp.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

internal object McpNotificationInteractionIntents {
    private const val MARK_READ_REQUEST_BASE = 210_200
    private const val DISMISS_REQUEST_BASE = 310_200

    fun buildMarkReadIntent(
        context: Context,
        notificationId: Int,
        serverName: String,
        targetBaAccountId: String?,
    ): Intent =
        buildInteractionIntent(
            context = context,
            action = McpNotificationMarkReadContract.ACTION,
            notificationId = notificationId,
            serverName = serverName,
            targetBaAccountId = targetBaAccountId,
        )

    fun markReadPendingIntent(
        context: Context,
        notificationId: Int,
        serverName: String,
        targetBaAccountId: String?,
    ): PendingIntent =
        broadcastPendingIntent(
            context = context,
            requestCode = MARK_READ_REQUEST_BASE + notificationId,
            intent =
                buildMarkReadIntent(
                    context = context,
                    notificationId = notificationId,
                    serverName = serverName,
                    targetBaAccountId = targetBaAccountId,
                ),
        )

    fun buildDismissIntent(
        context: Context,
        notificationId: Int,
        serverName: String,
        targetBaAccountId: String?,
    ): Intent =
        buildInteractionIntent(
            context = context,
            action = McpNotificationDismissContract.ACTION,
            notificationId = notificationId,
            serverName = serverName,
            targetBaAccountId = targetBaAccountId,
        )

    fun dismissPendingIntent(
        context: Context,
        notificationId: Int,
        serverName: String,
        targetBaAccountId: String?,
    ): PendingIntent =
        broadcastPendingIntent(
            context = context,
            requestCode = DISMISS_REQUEST_BASE + notificationId,
            intent =
                buildDismissIntent(
                    context = context,
                    notificationId = notificationId,
                    serverName = serverName,
                    targetBaAccountId = targetBaAccountId,
                ),
        )

    private fun buildInteractionIntent(
        context: Context,
        action: String,
        notificationId: Int,
        serverName: String,
        targetBaAccountId: String?,
    ): Intent =
        Intent().apply {
            setClassName(
                context.packageName,
                McpNotificationActionContract.MI_FOCUS_ACTION_RECEIVER_CLASS_NAME,
            )
            this.action = action
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            putExtra(McpNotificationMarkReadContract.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(McpNotificationMarkReadContract.EXTRA_SERVER_NAME, serverName)
            targetBaAccountId?.let {
                putExtra(McpNotificationMarkReadContract.EXTRA_TARGET_BA_ACCOUNT_ID, it)
            }
        }

    private fun broadcastPendingIntent(
        context: Context,
        requestCode: Int,
        intent: Intent,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
