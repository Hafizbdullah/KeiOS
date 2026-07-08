package os.kei.feature.keepalive.accessibility

import android.content.Context
import os.kei.core.concurrency.AppDispatchers
import os.kei.core.shizuku.ShizukuApiUtils

object AccessibilityGuardRuntime {
    fun newStateStore(): AccessibilityGuardStore = AccessibilityGuardStore()

    fun coordinator(stateStore: AccessibilityGuardStateStore = newStateStore()): AccessibilityGuardCoordinator =
        AccessibilityGuardCoordinator(
            serviceRepository = AccessibilityServiceRepository(),
            secureSettingsBridge =
                ShizukuAccessibilitySecureSettingsBridge(
                    shizukuApiUtils = ShizukuApiUtils(commandDispatcher = AppDispatchers.osOperations),
                ),
            stateStore = stateStore,
        )

    fun restoreRunner(
        context: Context,
        stateStore: AccessibilityGuardStateStore = newStateStore(),
        timeoutMs: Long = AccessibilityGuardRestoreRunner.DEFAULT_TIMEOUT_MS,
    ): AccessibilityGuardRestoreRunner =
        AccessibilityGuardRestoreRunner(
            coordinator = coordinator(stateStore),
            historyStore = AccessibilityGuardHistoryStore.forContext(context.applicationContext),
            selectedIdsProvider = { stateStore.loadSettings().guardedIds },
            timeoutMs = timeoutMs,
        )
}
