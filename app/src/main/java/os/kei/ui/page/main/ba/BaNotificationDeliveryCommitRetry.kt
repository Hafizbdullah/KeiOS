package os.kei.ui.page.main.ba

import kotlinx.coroutines.CancellationException
import os.kei.core.log.AppLogger
import os.kei.mcp.notification.McpNotificationDeliveryCommitException

internal suspend fun retryBaNotificationDeliveryCommit(
    tag: String,
    deliver: suspend () -> Boolean,
): Boolean =
    try {
        deliver()
    } catch (failure: McpNotificationDeliveryCommitException) {
        AppLogger.w(tag) { "delivery commit failed; retrying active notification commit" }
        try {
            deliver()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (retryFailure: Throwable) {
            AppLogger.e(tag, "delivery commit retry failed", retryFailure)
            false
        }
    }
