package os.kei.feature.keepalive.accessibility

import android.content.Context
import android.os.SystemClock

class AccessibilityGuardCoordinator(
    private val serviceRepository: AccessibilityServiceRepository,
    private val secureSettingsBridge: AccessibilitySecureSettingsBridge,
    private val stateStore: AccessibilityGuardStateStore,
    private val wallClockMs: () -> Long = System::currentTimeMillis,
    private val elapsedClockMs: () -> Long = SystemClock::elapsedRealtime,
) {
    suspend fun loadSnapshot(context: Context): AccessibilityGuardSnapshot {
        val settings = stateStore.loadSettings()
        return AccessibilityGuardSnapshot(
            settings = settings,
            services =
                serviceRepository.listInstalledServices(
                    context = context,
                    guardedIds = settings.guardedIds,
                ),
        )
    }

    fun setGuarded(
        id: AccessibilityServiceId,
        guarded: Boolean,
    ): AccessibilityGuardSettings {
        val current = stateStore.loadSettings()
        val nextGuardedIds =
            if (guarded) {
                current.guardedIds + id
            } else {
                current.guardedIds - id
            }
        val next =
            current.copy(
                guardedIds = nextGuardedIds,
                cooldownUntilById = current.cooldownUntilById - id,
                failureCountById = current.failureCountById - id,
            )
        stateStore.saveSettings(next)
        return next
    }

    fun setDaemonEnabled(enabled: Boolean): AccessibilityGuardSettings =
        updateSettings { current -> current.copy(daemonEnabled = enabled) }

    fun setBootRestoreEnabled(enabled: Boolean): AccessibilityGuardSettings =
        updateSettings { current -> current.copy(bootRestoreEnabled = enabled) }

    fun setScreenOnCheckEnabled(enabled: Boolean): AccessibilityGuardSettings =
        updateSettings { current -> current.copy(screenOnCheckEnabled = enabled) }

    suspend fun restoreMissing(reason: AccessibilityGuardRestoreReason): AccessibilityGuardRestoreResult {
        val startedAtMs = wallClockMs()
        val startedElapsedMs = elapsedClockMs()
        val settings = stateStore.loadSettings()
        val selectedIds = settings.guardedIds.sortedSet()
        if (selectedIds.isEmpty()) {
            return result(
                status = AccessibilityGuardRestoreStatus.SkippedNoTargets,
                reason = reason,
                selectedIds = selectedIds,
                beforeEnabledIds = emptySet(),
                afterEnabledIds = emptySet(),
                restoredIds = emptySet(),
                skippedIds = emptySet(),
                startedAtMs = startedAtMs,
                startedElapsedMs = startedElapsedMs,
            )
        }

        val read = secureSettingsBridge.readEnabledServiceIds()
        if (!read.success) {
            return result(
                status = AccessibilityGuardRestoreStatus.SkippedMissingPrivilege,
                reason = reason,
                selectedIds = selectedIds,
                beforeEnabledIds = emptySet(),
                afterEnabledIds = emptySet(),
                restoredIds = emptySet(),
                skippedIds = selectedIds,
                startedAtMs = startedAtMs,
                startedElapsedMs = startedElapsedMs,
                shizukuStatus = read.reason,
                failureReason = read.reason,
            )
        }

        val beforeEnabledIds = read.ids.sortedSet()
        val missingIds = (selectedIds - beforeEnabledIds).sortedSet()
        if (missingIds.isEmpty()) {
            return result(
                status = AccessibilityGuardRestoreStatus.SkippedAlreadyEnabled,
                reason = reason,
                selectedIds = selectedIds,
                beforeEnabledIds = beforeEnabledIds,
                afterEnabledIds = beforeEnabledIds,
                restoredIds = emptySet(),
                skippedIds = emptySet(),
                startedAtMs = startedAtMs,
                startedElapsedMs = startedElapsedMs,
            )
        }

        val nowMs = wallClockMs()
        val cooldownIds = missingIds
            .filter { id -> (settings.cooldownUntilById[id] ?: 0L) > nowMs }
            .toSet()
            .sortedSet()
        val restoreIds = (missingIds - cooldownIds).sortedSet()
        if (restoreIds.isEmpty()) {
            return result(
                status = AccessibilityGuardRestoreStatus.SkippedCooldown,
                reason = reason,
                selectedIds = selectedIds,
                beforeEnabledIds = beforeEnabledIds,
                afterEnabledIds = beforeEnabledIds,
                restoredIds = emptySet(),
                skippedIds = cooldownIds,
                startedAtMs = startedAtMs,
                startedElapsedMs = startedElapsedMs,
            )
        }

        val targetEnabledIds = (beforeEnabledIds + restoreIds).sortedSet()
        val write = secureSettingsBridge.writeEnabledServiceIds(targetEnabledIds)
        if (!write.success) {
            recordFailureCooldown(
                current = settings,
                failedIds = restoreIds,
                nowMs = nowMs,
            )
            return result(
                status = AccessibilityGuardRestoreStatus.Failed,
                reason = reason,
                selectedIds = selectedIds,
                beforeEnabledIds = beforeEnabledIds,
                afterEnabledIds = beforeEnabledIds,
                restoredIds = emptySet(),
                skippedIds = cooldownIds,
                startedAtMs = startedAtMs,
                startedElapsedMs = startedElapsedMs,
                failureReason = write.reason,
            )
        }

        recordSuccessCooldown(
            current = settings,
            restoredIds = restoreIds,
            nowMs = nowMs,
        )
        return result(
            status = AccessibilityGuardRestoreStatus.Restored,
            reason = reason,
            selectedIds = selectedIds,
            beforeEnabledIds = beforeEnabledIds,
            afterEnabledIds = targetEnabledIds,
            restoredIds = restoreIds,
            skippedIds = cooldownIds,
            startedAtMs = startedAtMs,
            startedElapsedMs = startedElapsedMs,
        )
    }

    private fun updateSettings(
        transform: (AccessibilityGuardSettings) -> AccessibilityGuardSettings,
    ): AccessibilityGuardSettings {
        val next = transform(stateStore.loadSettings())
        stateStore.saveSettings(next)
        return next
    }

    private fun recordSuccessCooldown(
        current: AccessibilityGuardSettings,
        restoredIds: Set<AccessibilityServiceId>,
        nowMs: Long,
    ) {
        if (restoredIds.isEmpty()) return
        val cooldownUntil = nowMs + SUCCESS_COOLDOWN_MS
        stateStore.saveSettings(
            current.copy(
                cooldownUntilById =
                    current.cooldownUntilById
                        .filterKeys { id -> id !in restoredIds } +
                        restoredIds.associateWith { cooldownUntil },
                failureCountById = current.failureCountById.filterKeys { id -> id !in restoredIds },
            ),
        )
    }

    private fun recordFailureCooldown(
        current: AccessibilityGuardSettings,
        failedIds: Set<AccessibilityServiceId>,
        nowMs: Long,
    ) {
        if (failedIds.isEmpty()) return
        val nextFailureCounts =
            current.failureCountById +
                failedIds.associateWith { id -> (current.failureCountById[id] ?: 0) + 1 }
        val nextCooldowns =
            current.cooldownUntilById +
                failedIds.associateWith { id ->
                    val failures = nextFailureCounts[id] ?: 1
                    nowMs + if (failures >= REPEATED_FAILURE_COUNT) {
                        REPEATED_FAILURE_COOLDOWN_MS
                    } else {
                        SUCCESS_COOLDOWN_MS
                    }
                }
        stateStore.saveSettings(
            current.copy(
                cooldownUntilById = nextCooldowns,
                failureCountById = nextFailureCounts,
            ),
        )
    }

    private fun result(
        status: AccessibilityGuardRestoreStatus,
        reason: AccessibilityGuardRestoreReason,
        selectedIds: Set<AccessibilityServiceId>,
        beforeEnabledIds: Set<AccessibilityServiceId>,
        afterEnabledIds: Set<AccessibilityServiceId>,
        restoredIds: Set<AccessibilityServiceId>,
        skippedIds: Set<AccessibilityServiceId>,
        startedAtMs: Long,
        startedElapsedMs: Long,
        shizukuStatus: String = "",
        failureReason: String = "",
    ): AccessibilityGuardRestoreResult {
        val finishedAtMs = wallClockMs()
        return AccessibilityGuardRestoreResult(
            status = status,
            reason = reason,
            selectedIds = selectedIds.sortedSet(),
            beforeEnabledIds = beforeEnabledIds.sortedSet(),
            afterEnabledIds = afterEnabledIds.sortedSet(),
            restoredIds = restoredIds.sortedSet(),
            skippedIds = skippedIds.sortedSet(),
            startedAtMs = startedAtMs,
            finishedAtMs = finishedAtMs,
            elapsedMs = (elapsedClockMs() - startedElapsedMs).coerceAtLeast(0L),
            shizukuStatus = shizukuStatus,
            failureReason = failureReason,
        )
    }

    companion object {
        const val SUCCESS_COOLDOWN_MS = 5L * 60L * 1000L
        const val REPEATED_FAILURE_COOLDOWN_MS = 30L * 60L * 1000L
        private const val REPEATED_FAILURE_COUNT = 2
    }
}

private fun Set<AccessibilityServiceId>.sortedSet(): Set<AccessibilityServiceId> =
    sortedWith(compareBy<AccessibilityServiceId> { it.packageName }.thenBy { it.serviceName })
        .toCollection(LinkedHashSet())
