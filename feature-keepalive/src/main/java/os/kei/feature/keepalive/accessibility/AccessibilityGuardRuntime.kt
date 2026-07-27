package os.kei.feature.keepalive.accessibility

import android.content.Context
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.privilege.PrivilegedShell

object AccessibilityGuardRuntime {
    fun newStateStore(): AccessibilityGuardStore = AccessibilityGuardStore()

    fun coordinator(stateStore: AccessibilityGuardStateStore = newStateStore()): AccessibilityGuardCoordinator =
        AccessibilityGuardCoordinator(
            secureSettingsBridge =
                ShizukuAccessibilitySecureSettingsBridge(
                    privilegedShell = PrivilegedShell(commandDispatcher = AppDispatchers.osOperations),
                ),
            stateStore = stateStore,
        )

    fun checkRunner(
        context: Context,
        stateStore: AccessibilityGuardStateStore = newStateStore(),
        timeoutMs: Long = AccessibilityGuardCheckRunner.DEFAULT_TIMEOUT_MS,
    ): AccessibilityGuardCheckRunner =
        AccessibilityGuardCheckRunner(
            coordinator = coordinator(stateStore),
            historyStore = AccessibilityGuardHistoryStore.forContext(context.applicationContext),
            timeoutMs = timeoutMs,
        )
}
