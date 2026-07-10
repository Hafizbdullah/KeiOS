package os.kei.feature.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import os.kei.core.background.AppBackgroundScheduler
import os.kei.core.background.BackgroundAsyncReceiverRunner
import os.kei.mcp.notification.McpNotificationDismissContract
import os.kei.mcp.notification.McpNotificationMarkReadContract
import os.kei.mcp.notification.McpNotificationHelper
import os.kei.ui.page.main.ba.BA_AP_DISMISS_SNOOZE_INTERVAL_MS
import os.kei.ui.page.main.ba.support.BASettingsStore

internal data class BaApNotificationInteractionWrite(
    val suppressionAnchorAtMs: Long? = null,
    val dismissedUntilAtMs: Long? = null,
)

internal fun resolveBaApNotificationInteractionWrite(
    action: String,
    nowMs: Long,
): BaApNotificationInteractionWrite? =
    when (action) {
        McpNotificationMarkReadContract.ACTION ->
            BaApNotificationInteractionWrite(
                suppressionAnchorAtMs = nowMs.coerceAtLeast(0L),
                dismissedUntilAtMs = 0L,
            )

        McpNotificationDismissContract.ACTION ->
            BaApNotificationInteractionWrite(
                dismissedUntilAtMs = saturatedAdd(nowMs, BA_AP_DISMISS_SNOOZE_INTERVAL_MS),
            )

        else -> null
    }

private fun saturatedAdd(value: Long, delta: Long): Long {
    val normalized = value.coerceAtLeast(0L)
    return if (normalized > Long.MAX_VALUE - delta) Long.MAX_VALUE else normalized + delta
}

class MiFocusNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val action = intent?.action ?: return
        if (action != ACTION_MARK_READ && action != ACTION_DISMISS) return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, Int.MIN_VALUE)
        if (notificationId == Int.MIN_VALUE) return
        if (action == ACTION_MARK_READ) {
            McpNotificationHelper.cancelNotification(context, notificationId)
        } else {
            McpNotificationHelper.invalidateNotificationRuntimeState(notificationId)
        }
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
            val write =
                resolveBaApNotificationInteractionWrite(
                    action = action,
                    nowMs = System.currentTimeMillis(),
                ) ?: return@launch
            BASettingsStore.saveAccountApInteractionState(
                accountId = target.accountId,
                kind = target.kind,
                suppressionAnchorAtMs = write.suppressionAnchorAtMs,
                dismissedUntilAtMs = write.dismissedUntilAtMs,
            )
            AppBackgroundScheduler.scheduleBaApThreshold(appContext)
        }
    }

    companion object {
        private const val TAG = "MiFocusNotificationActionReceiver"
        const val ACTION_MARK_READ = McpNotificationMarkReadContract.ACTION
        const val ACTION_DISMISS = McpNotificationDismissContract.ACTION
        const val EXTRA_NOTIFICATION_ID = McpNotificationMarkReadContract.EXTRA_NOTIFICATION_ID
    }
}
