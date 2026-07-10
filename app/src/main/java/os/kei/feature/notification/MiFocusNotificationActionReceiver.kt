package os.kei.feature.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import os.kei.core.background.AppBackgroundScheduler
import os.kei.core.background.BackgroundAsyncReceiverRunner
import os.kei.mcp.notification.McpNotificationMarkReadContract
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.ui.page.main.ba.support.BASettingsStore

class MiFocusNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action != ACTION_MARK_READ) return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, Int.MIN_VALUE)
        if (notificationId == Int.MIN_VALUE) return
        McpNotificationHelper.cancelNotification(context, notificationId)
        val serverName = intent.getStringExtra(McpNotificationMarkReadContract.EXTRA_SERVER_NAME)
        val rawAccountId =
            intent.getStringExtra(McpNotificationMarkReadContract.EXTRA_TARGET_BA_ACCOUNT_ID)
        if (serverName == null || rawAccountId == null) return
        BackgroundAsyncReceiverRunner.launch(
            receiver = this,
            context = context,
            tag = TAG,
        ) { appContext ->
            val accountIds =
                BASettingsStore.loadAccountState().accounts
                    .map { it.profile.id }
                    .toSet()
            val target =
                resolveBaApMarkReadTarget(
                    notificationId = notificationId,
                    serverName = serverName,
                    rawAccountId = rawAccountId,
                    knownAccountIds = accountIds,
                ) ?: return@launch
            BASettingsStore.saveAccountApSuppressionAnchor(
                accountId = target.accountId,
                kind = target.kind,
                anchorAtMs = System.currentTimeMillis(),
            )
            AppBackgroundScheduler.scheduleBaApThreshold(appContext)
        }
    }

    companion object {
        private const val TAG = "MiFocusNotificationActionReceiver"
        const val ACTION_MARK_READ = McpNotificationMarkReadContract.ACTION
        const val EXTRA_NOTIFICATION_ID = McpNotificationMarkReadContract.EXTRA_NOTIFICATION_ID
    }
}
