package os.kei.feature.keepalive.accessibility

import android.content.ComponentName

data class AccessibilityServiceId(
    val packageName: String,
    val serviceName: String,
) {
    fun flatten(): String = ComponentName(packageName, serviceName).flattenToString()
}

data class AccessibilityGuardCapability(
    val privilegeReady: Boolean,
    val canReadSecureSettings: Boolean,
    val privilegeStatus: String,
    val checkedAtMs: Long,
)

enum class AccessibilityGuardCheckReason {
    Manual,
    ForegroundServiceStart,
    SecureSettingChanged,
    ScreenOn,
    BootCompleted,
    PackageReplaced,
    TimeoutRecovery,
}

enum class AccessibilityGuardCheckStatus {
    Healthy,
    Checked,
    MissingPrivilege,
    Failed,
    TimedOut,
}

data class AccessibilityGuardCheckResult(
    val status: AccessibilityGuardCheckStatus,
    val reason: AccessibilityGuardCheckReason,
    val checkCount: Int,
    val healthyCount: Int,
    val warningCount: Int,
    val startedAtMs: Long,
    val finishedAtMs: Long,
    val elapsedMs: Long,
    val privilegeStatus: String,
    val failureReason: String,
)

data class AccessibilityGuardHistoryEntry(
    val id: String,
    val timestampMs: Long,
    val reason: AccessibilityGuardCheckReason,
    val status: AccessibilityGuardCheckStatus,
    val triggerAction: String = "",
    val checkCount: Int,
    val healthyCount: Int,
    val warningCount: Int,
    val elapsedMs: Long,
    val privilegeStatus: String,
    val failureReason: String,
) {
    companion object
}

fun parseAccessibilityServiceIds(raw: String): Set<AccessibilityServiceId> {
    if (raw.isBlank()) return emptySet()
    return raw
        .split(':')
        .asSequence()
        .mapNotNull { token -> token.toAccessibilityServiceIdOrNull() }
        .distinct()
        .sortedWith(compareBy<AccessibilityServiceId> { it.packageName }.thenBy { it.serviceName })
        .toCollection(LinkedHashSet())
}

internal fun String.toAccessibilityServiceIdOrNull(): AccessibilityServiceId? {
    val flattened = trim()
    if (flattened.isBlank()) return null
    val component = ComponentName.unflattenFromString(flattened) ?: return null
    val packageName = component.packageName.trim()
    val serviceName = component.className.trim()
    if (packageName.isBlank() || serviceName.isBlank()) return null
    return AccessibilityServiceId(
        packageName = packageName,
        serviceName = serviceName,
    )
}
