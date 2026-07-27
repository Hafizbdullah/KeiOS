package os.kei.feature.keepalive.accessibility

import android.os.SystemClock

class AccessibilityGuardCoordinator(
    private val secureSettingsBridge: AccessibilitySecureSettingsBridge,
    private val stateStore: AccessibilityGuardStateStore,
    private val wallClockMs: () -> Long = System::currentTimeMillis,
    private val elapsedClockMs: () -> Long = SystemClock::elapsedRealtime,
) {
    suspend fun loadSnapshot(): AccessibilityGuardSnapshot {
        val settings = stateStore.loadSettings()
        val read = secureSettingsBridge.readEnabledServiceIds()
        return AccessibilityGuardSnapshot(
            settings = settings,
            capability = read.toCapability(checkedAtMs = wallClockMs()),
        )
    }

    fun setDaemonEnabled(enabled: Boolean): AccessibilityGuardSettings =
        updateSettings { current -> current.copy(daemonEnabled = enabled) }

    fun setBootCheckEnabled(enabled: Boolean): AccessibilityGuardSettings =
        updateSettings { current -> current.copy(bootCheckEnabled = enabled) }

    fun setScreenOnCheckEnabled(enabled: Boolean): AccessibilityGuardSettings =
        updateSettings { current -> current.copy(screenOnCheckEnabled = enabled) }

    suspend fun checkSelf(reason: AccessibilityGuardCheckReason): AccessibilityGuardCheckResult {
        val startedAtMs = wallClockMs()
        val startedElapsedMs = elapsedClockMs()
        val settings = stateStore.loadSettings()
        val read = secureSettingsBridge.readEnabledServiceIds()
        val enabledPolicyCount = settings.enabledPolicyCount
        val checkCount = SELF_CAPABILITY_CHECK_COUNT + enabledPolicyCount

        if (!read.success) {
            return result(
                status = AccessibilityGuardCheckStatus.MissingPrivilege,
                reason = reason,
                checkCount = checkCount,
                healthyCount = enabledPolicyCount,
                warningCount = SELF_CAPABILITY_CHECK_COUNT,
                startedAtMs = startedAtMs,
                startedElapsedMs = startedElapsedMs,
                privilegeStatus = read.reason,
                failureReason = read.reason,
            )
        }

        return result(
            status =
                if (enabledPolicyCount > 0) {
                    AccessibilityGuardCheckStatus.Healthy
                } else {
                    AccessibilityGuardCheckStatus.Checked
                },
            reason = reason,
            checkCount = checkCount,
            healthyCount = checkCount,
            warningCount = 0,
            startedAtMs = startedAtMs,
            startedElapsedMs = startedElapsedMs,
            privilegeStatus = PRIVILEGE_STATUS_READY,
        )
    }

    private fun updateSettings(
        transform: (AccessibilityGuardSettings) -> AccessibilityGuardSettings,
    ): AccessibilityGuardSettings {
        val next = transform(stateStore.loadSettings())
        stateStore.saveSettings(next)
        return next
    }

    private fun result(
        status: AccessibilityGuardCheckStatus,
        reason: AccessibilityGuardCheckReason,
        checkCount: Int,
        healthyCount: Int,
        warningCount: Int,
        startedAtMs: Long,
        startedElapsedMs: Long,
        privilegeStatus: String = "",
        failureReason: String = "",
    ): AccessibilityGuardCheckResult {
        val normalizedCheckCount = checkCount.coerceAtLeast(0)
        val finishedAtMs = wallClockMs()
        return AccessibilityGuardCheckResult(
            status = status,
            reason = reason,
            checkCount = normalizedCheckCount,
            healthyCount = healthyCount.coerceIn(0, normalizedCheckCount),
            warningCount = warningCount.coerceAtLeast(0),
            startedAtMs = startedAtMs,
            finishedAtMs = finishedAtMs,
            elapsedMs = (elapsedClockMs() - startedElapsedMs).coerceAtLeast(0L),
            privilegeStatus = privilegeStatus,
            failureReason = failureReason,
        )
    }

    companion object {
        const val SELF_CAPABILITY_CHECK_COUNT = 1
        const val PRIVILEGE_STATUS_READY = "ready"
    }
}

private val AccessibilityGuardSettings.enabledPolicyCount: Int
    get() =
        listOf(
            daemonEnabled,
            bootCheckEnabled,
            screenOnCheckEnabled,
        ).count { it }

private fun AccessibilitySecureSettingRead.toCapability(checkedAtMs: Long): AccessibilityGuardCapability =
    AccessibilityGuardCapability(
        privilegeReady = success,
        canReadSecureSettings = success,
        privilegeStatus =
            if (success) {
                AccessibilityGuardCoordinator.PRIVILEGE_STATUS_READY
            } else {
                reason
            },
        checkedAtMs = checkedAtMs.coerceAtLeast(0L),
    )
