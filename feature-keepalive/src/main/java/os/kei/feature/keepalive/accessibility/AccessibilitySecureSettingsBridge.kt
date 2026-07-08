package os.kei.feature.keepalive.accessibility

import os.kei.core.system.AppCommandResult

data class AccessibilitySecureSettingRead(
    val rawValue: String,
    val ids: Set<AccessibilityServiceId>,
    val success: Boolean,
    val reason: String,
)

data class AccessibilitySecureSettingWrite(
    val success: Boolean,
    val changed: Boolean,
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

    suspend fun writeEnabledServiceIds(ids: Set<AccessibilityServiceId>): AccessibilitySecureSettingWrite

    suspend fun setAccessibilityEnabled(enabled: Boolean): AccessibilitySecureSettingWrite
}
