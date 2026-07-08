package os.kei.feature.keepalive.accessibility

data class AccessibilityServiceId(
    val packageName: String,
    val serviceName: String,
)

data class AccessibilityServiceSnapshot(
    val id: AccessibilityServiceId,
    val label: String,
    val packageLabel: String,
    val enabled: Boolean,
    val guarded: Boolean,
    val installed: Boolean,
    val system: Boolean,
)

data class AccessibilityGuardCapability(
    val shizukuReady: Boolean,
    val canReadSecureSettings: Boolean,
    val canWriteSecureSettings: Boolean,
    val notificationReady: Boolean,
    val foregroundServiceAllowed: Boolean,
)

enum class AccessibilityGuardRestoreReason {
    Manual,
    ForegroundServiceStart,
    SecureSettingChanged,
    ScreenOn,
    BootCompleted,
    PackageReplaced,
    TimeoutRecovery,
}

enum class AccessibilityGuardRestoreStatus {
    Restored,
    SkippedNoTargets,
    SkippedMissingPrivilege,
    SkippedAlreadyEnabled,
    SkippedCooldown,
    Failed,
    TimedOut,
}

data class AccessibilityGuardRestoreResult(
    val status: AccessibilityGuardRestoreStatus,
    val reason: AccessibilityGuardRestoreReason,
    val selectedIds: Set<AccessibilityServiceId>,
    val beforeEnabledIds: Set<AccessibilityServiceId>,
    val afterEnabledIds: Set<AccessibilityServiceId>,
    val restoredIds: Set<AccessibilityServiceId>,
    val skippedIds: Set<AccessibilityServiceId>,
    val startedAtMs: Long,
    val finishedAtMs: Long,
    val elapsedMs: Long,
    val shizukuStatus: String,
    val failureReason: String,
) {
    val changed: Boolean
        get() = restoredIds.isNotEmpty() || beforeEnabledIds != afterEnabledIds
}

data class AccessibilityGuardHistoryEntry(
    val id: String,
    val timestampMs: Long,
    val reason: AccessibilityGuardRestoreReason,
    val status: AccessibilityGuardRestoreStatus,
    val selectedCount: Int,
    val restoredCount: Int,
    val skippedCount: Int,
    val elapsedMs: Long,
    val shizukuStatus: String,
    val failureReason: String,
    val serviceIds: List<AccessibilityServiceId>,
)
