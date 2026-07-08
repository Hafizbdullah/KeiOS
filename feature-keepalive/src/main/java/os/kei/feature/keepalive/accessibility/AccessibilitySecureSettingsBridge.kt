package os.kei.feature.keepalive.accessibility

import os.kei.core.system.AppCommandResult

data class AccessibilitySecureSettingRead(
    val rawValue: String,
    val ids: Set<AccessibilityServiceId>,
    val success: Boolean,
    val reason: String,
)

fun interface AccessibilitySecureSettingsCommandRunner {
    suspend fun run(
        command: String,
        timeoutMs: Long,
    ): AppCommandResult
}

interface AccessibilitySecureSettingsBridge {
    suspend fun readEnabledServiceIds(): AccessibilitySecureSettingRead
}
