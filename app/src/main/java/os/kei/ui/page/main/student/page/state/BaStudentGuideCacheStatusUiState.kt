package os.kei.ui.page.main.student.page.state

import os.kei.ui.page.main.student.BaGuideStudentDetailCacheMeta
import os.kei.ui.page.main.student.BaGuideStudentDetailFreshnessTier

internal data class BaStudentGuideCacheStatusUiState(
    val cachedAtMs: Long = 0L,
    val lastValidatedAtMs: Long = 0L,
    val nextAutoRefreshAtMs: Long = 0L,
    val freshnessTier: BaGuideStudentDetailFreshnessTier? = null,
    val validatingInBackground: Boolean = false,
    val failureCount: Int = 0,
    val lastFailureAtMs: Long = 0L,
    val nextRetryAtMs: Long = 0L,
) {
    val hasStatus: Boolean
        get() =
            cachedAtMs > 0L ||
                lastValidatedAtMs > 0L ||
                freshnessTier != null ||
                validatingInBackground ||
                failureCount > 0

    companion object {
        val Empty = BaStudentGuideCacheStatusUiState()
    }
}

internal fun BaGuideStudentDetailCacheMeta?.toCacheStatusUiState(
    validatingInBackground: Boolean = false,
): BaStudentGuideCacheStatusUiState {
    this ?: return if (validatingInBackground) {
        BaStudentGuideCacheStatusUiState(validatingInBackground = true)
    } else {
        BaStudentGuideCacheStatusUiState.Empty
    }
    return BaStudentGuideCacheStatusUiState(
        cachedAtMs = cachedAtMs,
        lastValidatedAtMs = lastValidatedAtMs,
        nextAutoRefreshAtMs = nextAutoRefreshAtMs,
        freshnessTier = freshnessTier,
        validatingInBackground = validatingInBackground,
        failureCount = failureCount,
        lastFailureAtMs = lastFailureAtMs,
        nextRetryAtMs = nextRetryAtMs,
    )
}
