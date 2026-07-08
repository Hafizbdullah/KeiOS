package os.kei.core.background

import com.tencent.mmkv.MMKV
import os.kei.core.prefs.KeiMmkv

data class AppBackgroundRecoverySnapshot(
    val lastAction: String = "",
    val lastFinishedAtMs: Long = 0L,
    val lastElapsedMs: Long = 0L,
    val lastFailed: Boolean = false,
    val lastFailureReason: String = "",
    val recoveryCount: Int = 0,
)

object AppBackgroundRecoveryStore {
    private val store: MMKV by lazy { KeiMmkv.byId(KV_ID) }

    fun loadSnapshot(): AppBackgroundRecoverySnapshot =
        AppBackgroundRecoverySnapshot(
            lastAction = store.decodeString(KEY_LAST_ACTION, "").orEmpty(),
            lastFinishedAtMs = store.decodeLong(KEY_LAST_FINISHED_AT_MS, 0L),
            lastElapsedMs = store.decodeLong(KEY_LAST_ELAPSED_MS, 0L),
            lastFailed = store.decodeBool(KEY_LAST_FAILED, false),
            lastFailureReason = store.decodeString(KEY_LAST_FAILURE_REASON, "").orEmpty(),
            recoveryCount = store.decodeInt(KEY_RECOVERY_COUNT, 0).coerceAtLeast(0),
        )

    fun recordSucceeded(
        action: String,
        startedElapsedRealtimeMs: Long,
        finishedAtMs: Long = System.currentTimeMillis(),
        finishedElapsedRealtimeMs: Long = android.os.SystemClock.elapsedRealtime(),
    ) {
        val nextCount = store.decodeInt(KEY_RECOVERY_COUNT, 0).coerceAtLeast(0) + 1
        store.encode(KEY_LAST_ACTION, action)
        store.encode(KEY_LAST_FINISHED_AT_MS, finishedAtMs.coerceAtLeast(0L))
        store.encode(
            KEY_LAST_ELAPSED_MS,
            (finishedElapsedRealtimeMs - startedElapsedRealtimeMs).coerceAtLeast(0L)
        )
        store.encode(KEY_LAST_FAILED, false)
        store.encode(KEY_LAST_FAILURE_REASON, "")
        store.encode(KEY_RECOVERY_COUNT, nextCount)
    }

    fun recordFailed(
        action: String,
        startedElapsedRealtimeMs: Long,
        error: Throwable,
        finishedAtMs: Long = System.currentTimeMillis(),
        finishedElapsedRealtimeMs: Long = android.os.SystemClock.elapsedRealtime(),
    ) {
        val nextCount = store.decodeInt(KEY_RECOVERY_COUNT, 0).coerceAtLeast(0) + 1
        store.encode(KEY_LAST_ACTION, action)
        store.encode(KEY_LAST_FINISHED_AT_MS, finishedAtMs.coerceAtLeast(0L))
        store.encode(
            KEY_LAST_ELAPSED_MS,
            (finishedElapsedRealtimeMs - startedElapsedRealtimeMs).coerceAtLeast(0L)
        )
        store.encode(KEY_LAST_FAILED, true)
        store.encode(KEY_LAST_FAILURE_REASON, error.javaClass.simpleName.ifBlank { "Throwable" })
        store.encode(KEY_RECOVERY_COUNT, nextCount)
    }

    private const val KV_ID = "app_background_recovery"
    private const val KEY_LAST_ACTION = "last_action"
    private const val KEY_LAST_FINISHED_AT_MS = "last_finished_at_ms"
    private const val KEY_LAST_ELAPSED_MS = "last_elapsed_ms"
    private const val KEY_LAST_FAILED = "last_failed"
    private const val KEY_LAST_FAILURE_REASON = "last_failure_reason"
    private const val KEY_RECOVERY_COUNT = "recovery_count"
}
