package os.kei.feature.keepalive.accessibility

import os.kei.core.shizuku.ShizukuApiUtils
import os.kei.core.system.AppCommandResult

class ShizukuAccessibilitySecureSettingsBridge(
    private val commandRunner: AccessibilitySecureSettingsCommandRunner,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) : AccessibilitySecureSettingsBridge {
    constructor(
        shizukuApiUtils: ShizukuApiUtils,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ) : this(
        commandRunner = AccessibilitySecureSettingsCommandRunner { command, commandTimeoutMs ->
            shizukuApiUtils.execCommandCancellableResult(
                command = command,
                timeoutMs = commandTimeoutMs,
            )
        },
        timeoutMs = timeoutMs,
    )

    override suspend fun readEnabledServiceIds(): AccessibilitySecureSettingRead {
        val result = commandRunner.run(READ_ENABLED_ACCESSIBILITY_SERVICES_COMMAND, timeoutMs)
        if (!result.succeeded) {
            return AccessibilitySecureSettingRead(
                rawValue = result.combinedOutput(),
                ids = emptySet(),
                success = false,
                reason = result.toFailureReason(),
            )
        }
        val rawValue = result.stdout.trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()
        return AccessibilitySecureSettingRead(
            rawValue = rawValue,
            ids = parseAccessibilityServiceIds(rawValue),
            success = true,
            reason = "",
        )
    }

    private fun AppCommandResult.toFailureReason(): String =
        when {
            timedOut -> "timeout"
            cancelled -> "cancelled"
            stderr.isNotBlank() -> stderr.trim()
            stdout.isNotBlank() -> stdout.trim()
            exitCode != null -> "exit_$exitCode"
            else -> "command_failed"
        }

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 2_500L
        private const val READ_ENABLED_ACCESSIBILITY_SERVICES_COMMAND =
            "settings get secure enabled_accessibility_services"
    }
}
