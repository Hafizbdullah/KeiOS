package os.kei.feature.keepalive.accessibility

import android.os.SystemClock
import kotlinx.coroutines.withTimeoutOrNull
import os.kei.core.log.AppLogger

fun interface AccessibilityGuardRestoreOperation {
    suspend fun restoreMissing(reason: AccessibilityGuardRestoreReason): AccessibilityGuardRestoreResult
}

class AccessibilityGuardRestoreRunner(
    private val restoreOperation: AccessibilityGuardRestoreOperation,
    private val historyStore: AccessibilityGuardHistoryStore,
    private val selectedIdsProvider: () -> Set<AccessibilityServiceId> = { emptySet() },
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val wallClockMs: () -> Long = System::currentTimeMillis,
    private val elapsedClockMs: () -> Long = SystemClock::elapsedRealtime,
) {
    constructor(
        coordinator: AccessibilityGuardCoordinator,
        historyStore: AccessibilityGuardHistoryStore,
        selectedIdsProvider: () -> Set<AccessibilityServiceId> = { emptySet() },
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        wallClockMs: () -> Long = System::currentTimeMillis,
        elapsedClockMs: () -> Long = SystemClock::elapsedRealtime,
    ) : this(
        restoreOperation = AccessibilityGuardRestoreOperation { reason -> coordinator.restoreMissing(reason) },
        historyStore = historyStore,
        selectedIdsProvider = selectedIdsProvider,
        timeoutMs = timeoutMs,
        wallClockMs = wallClockMs,
        elapsedClockMs = elapsedClockMs,
    )

    suspend fun restoreAndRecord(
        reason: AccessibilityGuardRestoreReason,
        triggerAction: String,
    ): AccessibilityGuardRestoreResult {
        val startedAtMs = wallClockMs()
        val startedElapsedMs = elapsedClockMs()
        val result =
            withTimeoutOrNull(timeoutMs.coerceAtLeast(1L)) {
                restoreOperation.restoreMissing(reason)
            } ?: timeoutResult(
                reason = reason,
                startedAtMs = startedAtMs,
                startedElapsedMs = startedElapsedMs,
            )
        recordHistory(result = result, triggerAction = triggerAction)
        return result
    }

    suspend fun recordTimeout(
        reason: AccessibilityGuardRestoreReason,
        triggerAction: String,
    ): AccessibilityGuardRestoreResult {
        val startedAtMs = wallClockMs()
        val startedElapsedMs = elapsedClockMs()
        val result =
            timeoutResult(
                reason = reason,
                startedAtMs = startedAtMs,
                startedElapsedMs = startedElapsedMs,
            )
        recordHistory(result = result, triggerAction = triggerAction)
        return result
    }

    private suspend fun recordHistory(
        result: AccessibilityGuardRestoreResult,
        triggerAction: String,
    ) {
        runCatching {
            historyStore.append(
                AccessibilityGuardHistoryEntry.fromResult(
                    result = result,
                    triggerAction = triggerAction,
                ),
            )
        }.onFailure { error ->
            AppLogger.w(TAG, "record accessibility guard history failed", error)
        }
    }

    private fun timeoutResult(
        reason: AccessibilityGuardRestoreReason,
        startedAtMs: Long,
        startedElapsedMs: Long,
    ): AccessibilityGuardRestoreResult {
        val selectedIds = selectedIdsProvider().sortedServiceIdSet()
        return AccessibilityGuardRestoreResult(
            status = AccessibilityGuardRestoreStatus.TimedOut,
            reason = reason,
            selectedIds = selectedIds,
            beforeEnabledIds = emptySet(),
            afterEnabledIds = emptySet(),
            restoredIds = emptySet(),
            skippedIds = selectedIds,
            startedAtMs = startedAtMs,
            finishedAtMs = wallClockMs().coerceAtLeast(startedAtMs),
            elapsedMs = (elapsedClockMs() - startedElapsedMs).coerceAtLeast(timeoutMs.coerceAtLeast(1L)),
            shizukuStatus = "",
            failureReason = "timeout",
        )
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 12_000L
        private const val TAG = "AccessibilityGuardRunner"
    }
}

private fun Set<AccessibilityServiceId>.sortedServiceIdSet(): Set<AccessibilityServiceId> =
    sortedWith(compareBy<AccessibilityServiceId> { it.packageName }.thenBy { it.serviceName })
        .toCollection(LinkedHashSet())
