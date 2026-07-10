package os.kei.core.background

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import os.kei.ui.page.main.sync.WebDavAutoSync
import java.util.concurrent.atomic.AtomicBoolean

class AppBackgroundTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != ACTION_GITHUB_TICK && action != ACTION_BA_AP_TICK && action != ACTION_WEBDAV_SYNC) return
        if (action == ACTION_GITHUB_TICK) {
            val enqueued = GitHubBackgroundRefreshJobService.enqueueNow(context)
            if (!enqueued) {
                AppBackgroundScheduler.onTickHandled(context.applicationContext, action)
            }
            return
        }
        val rescheduled = AtomicBoolean(false)
        val rescheduleOnce: suspend (Context) -> Unit = { appContext ->
            if (rescheduled.compareAndSet(false, true)) {
                AppBackgroundScheduler.onTickHandled(appContext, action)
            }
        }
        val recoverTimeout: suspend (Context) -> Unit = { appContext ->
            when (action) {
                ACTION_WEBDAV_SYNC -> WebDavAutoSync.handleScheduledTickTimeout(appContext)
                else -> Unit
            }
            rescheduleOnce(appContext)
        }
        BackgroundAsyncReceiverRunner.launch(
            receiver = this,
            context = context,
            tag = TAG,
            timeoutMs = timeoutForAction(action),
            awaitWorkerCompletionOnTimeout = action == ACTION_BA_AP_TICK,
            onTimeout = recoverTimeout
        ) { appContext ->
            try {
                when (action) {
                    ACTION_BA_AP_TICK -> AppForegroundInfoHandler.handleBaApTick(appContext)
                    ACTION_WEBDAV_SYNC -> WebDavAutoSync.handleScheduledTick(appContext)
                }
            } finally {
                rescheduleOnce(appContext)
            }
        }
    }

    companion object {
        const val ACTION_GITHUB_TICK = "os.kei.background.action.GITHUB_TICK"
        const val ACTION_BA_AP_TICK = "os.kei.background.action.BA_AP_TICK"
        const val ACTION_WEBDAV_SYNC = "os.kei.background.action.WEBDAV_SYNC"
        private const val REQUEST_CODE_GITHUB_TICK = 42001
        private const val REQUEST_CODE_BA_AP_TICK = 42002
        private const val REQUEST_CODE_WEBDAV_SYNC = 42003
        private const val BA_AP_TICK_TIMEOUT_MS = 12_000L
        private const val WEBDAV_SYNC_TIMEOUT_MS = 45_000L
        private const val TAG = "AppBackgroundTickReceiver"

        private fun timeoutForAction(action: String): Long {
            return when (action) {
                ACTION_BA_AP_TICK -> BA_AP_TICK_TIMEOUT_MS
                ACTION_WEBDAV_SYNC -> WEBDAV_SYNC_TIMEOUT_MS
                else -> BA_AP_TICK_TIMEOUT_MS
            }
        }

        fun githubTickPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AppBackgroundTickReceiver::class.java).apply {
                action = ACTION_GITHUB_TICK
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_GITHUB_TICK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun baApTickPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AppBackgroundTickReceiver::class.java).apply {
                action = ACTION_BA_AP_TICK
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BA_AP_TICK,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun webDavSyncPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AppBackgroundTickReceiver::class.java).apply {
                action = ACTION_WEBDAV_SYNC
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_WEBDAV_SYNC,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
