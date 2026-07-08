package os.kei.feature.keepalive.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import os.kei.core.log.AppLogger
import os.kei.feature.keepalive.accessibility.AccessibilityGuardCheckReason
import os.kei.feature.keepalive.accessibility.AccessibilityGuardRuntime
import os.kei.feature.keepalive.service.AccessibilityGuardForegroundService

class AccessibilityGuardEventReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val action = intent?.action.orEmpty()
        val reason = action.toCheckReasonOrNull() ?: return
        val appContext = context.applicationContext
        val stateStore = AccessibilityGuardRuntime.newStateStore()
        val settings = stateStore.loadSettings()
        if (!shouldHandle(action = action, bootCheckEnabled = settings.bootCheckEnabled)) return

        if (settings.daemonEnabled) {
            AccessibilityGuardForegroundService.start(appContext)
        }

        AccessibilityGuardReceiverRunner.launch(
            receiver = this,
            context = appContext,
            timeoutMs = RECEIVER_TIMEOUT_MS,
            onTimeout = { timeoutContext ->
                AccessibilityGuardRuntime
                    .checkRunner(
                        context = timeoutContext,
                        stateStore = stateStore,
                    )
                    .recordTimeout(
                        reason = reason,
                        triggerAction = "$action:timeout",
                    )
            },
        ) { receiverContext ->
            AccessibilityGuardRuntime
                .checkRunner(
                    context = receiverContext,
                    stateStore = stateStore,
                    timeoutMs = RECEIVER_TIMEOUT_MS,
                )
                .checkAndRecord(
                    reason = reason,
                    triggerAction = action,
                )
        }
    }

    companion object {
        private const val TAG = "AccessibilityGuardEvent"
        private const val RECEIVER_TIMEOUT_MS = 12_000L

        internal fun shouldHandle(
            action: String,
            bootCheckEnabled: Boolean,
        ): Boolean =
            when (action) {
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                -> bootCheckEnabled
                AccessibilityGuardForegroundService.ACTION_CHECK_ACCESSIBILITY_GUARD -> true
                else -> false
            }

        fun requestCheck(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, AccessibilityGuardEventReceiver::class.java).apply {
                action = AccessibilityGuardForegroundService.ACTION_CHECK_ACCESSIBILITY_GUARD
                setPackage(appContext.packageName)
            }
            runCatching { appContext.sendBroadcast(intent) }
                .onFailure { error -> AppLogger.w(TAG, "request accessibility guard check failed", error) }
        }

        private fun String.toCheckReasonOrNull(): AccessibilityGuardCheckReason? =
            when (this) {
                Intent.ACTION_BOOT_COMPLETED -> AccessibilityGuardCheckReason.BootCompleted
                Intent.ACTION_MY_PACKAGE_REPLACED -> AccessibilityGuardCheckReason.PackageReplaced
                AccessibilityGuardForegroundService.ACTION_CHECK_ACCESSIBILITY_GUARD ->
                    AccessibilityGuardCheckReason.Manual
                else -> null
            }
    }
}
