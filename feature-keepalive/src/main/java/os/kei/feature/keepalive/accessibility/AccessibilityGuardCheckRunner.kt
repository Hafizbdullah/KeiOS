package os.kei.feature.keepalive.accessibility

import android.os.SystemClock
import kotlinx.coroutines.withTimeoutOrNull
import os.kei.core.log.AppLogger

fun interface AccessibilityGuardCheckOperation {
    suspend fun checkSelf(reason: AccessibilityGuardCheckReason): AccessibilityGuardCheckResult
}

class AccessibilityGuardCheckRunner(
    private val checkOperation: AccessibilityGuardCheckOperation,
    private val historyStore: AccessibilityGuardHistoryStore,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val wallClockMs: () -> Long = System::currentTimeMillis,
    private val elapsedClockMs: () -> Long = SystemClock::elapsedRealtime,
) {
    constructor(
        coordinator: AccessibilityGuardCoordinator,
        historyStore: AccessibilityGuardHistoryStore,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        wallClockMs: () -> Long = System::currentTimeMillis,
        elapsedClockMs: () -> Long = SystemClock::elapsedRealtime,
    ) : this(
        checkOperation = AccessibilityGuardCheckOperation { reason -> coordinator.checkSelf(reason) },
        historyStore = historyStore,
        timeoutMs = timeoutMs,
        wallClockMs = wallClockMs,
        elapsedClockMs = elapsedClockMs,
    )

    suspend fun checkAndRecord(
        reason: AccessibilityGuardCheckReason,
        triggerAction: String,
    ): AccessibilityGuardCheckResult {
        val startedAtMs = wallClockMs()
        val startedElapsedMs = elapsedClockMs()
        val result =
            withTimeoutOrNull(timeoutMs.coerceAtLeast(1L)) {
                checkOperation.checkSelf(reason)
            } ?: timeoutResult(
                reason = reason,
                startedAtMs = startedAtMs,
                startedElapsedMs = startedElapsedMs,
            )
        recordHistory(result = result, triggerAction = triggerAction)
        return result
    }

    suspend fun recordTimeout(
        reason: AccessibilityGuardCheckReason,
        triggerAction: String,
    ): AccessibilityGuardCheckResult {
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
        result: AccessibilityGuardCheckResult,
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
        reason: AccessibilityGuardCheckReason,
        startedAtMs: Long,
        startedElapsedMs: Long,
    ): AccessibilityGuardCheckResult =
        AccessibilityGuardCheckResult(
            status = AccessibilityGuardCheckStatus.TimedOut,
            reason = reason,
            checkCount = AccessibilityGuardCoordinator.SELF_CAPABILITY_CHECK_COUNT,
            healthyCount = 0,
            warningCount = AccessibilityGuardCoordinator.SELF_CAPABILITY_CHECK_COUNT,
            startedAtMs = startedAtMs,
            finishedAtMs = wallClockMs().coerceAtLeast(startedAtMs),
            elapsedMs = (elapsedClockMs() - startedElapsedMs).coerceAtLeast(timeoutMs.coerceAtLeast(1L)),
            privilegeStatus = "",
            failureReason = "timeout",
        )

    companion object {
        const val DEFAULT_TIMEOUT_MS = 12_000L
        private const val TAG = "AccessibilityGuardRunner"
    }
}
