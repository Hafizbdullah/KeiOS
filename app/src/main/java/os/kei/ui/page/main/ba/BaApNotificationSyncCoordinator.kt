package os.kei.ui.page.main.ba

import android.content.Context
import kotlinx.coroutines.withContext
import os.kei.core.concurrency.AppDispatchers
import kotlinx.coroutines.withTimeoutOrNull
import os.kei.ui.page.main.ba.support.BA_AP_MAX
import os.kei.ui.page.main.ba.support.BaAccountId

internal data class BaApNotificationSyncRequest(
    val currentDisplay: Int,
    val limitDisplay: Int,
    val thresholdDisplay: Int,
    val notifyEnabled: Boolean,
    val lastNotifiedLevel: Int,
    val notificationId: Int = BaAccountNotificationKind.Ap.legacyId,
    val accountDisplayName: String = "",
    val accountId: BaAccountId? = null,
    val keepReadUntilBelowThreshold: Boolean = true,
    val suppressionAnchorAtMs: Long = 0L,
)

internal data class BaApNotificationSyncResult(
    val lastNotifiedLevel: Int? = null,
    val suppressionAnchorAtMs: Long? = null,
)

internal data class BaApNotificationSyncPlan(
    val request: BaApNotificationSyncRequest,
    val shouldSendThresholdNotification: Boolean = false,
    val shouldRefreshActiveNotification: Boolean = true,
    val nextLastNotifiedLevel: Int? = null,
    val nextSuppressionAnchorAtMs: Long? = null,
    val advanceSuppressionAnchorAfterDelivery: Boolean = false,
)

internal object BaApNotificationSyncCoordinator {
    private const val NOTIFICATION_SYNC_TIMEOUT_MS = 1_500L

    suspend fun sync(
        context: Context,
        request: BaApNotificationSyncRequest,
    ): BaApNotificationSyncResult {
        val nowMs = System.currentTimeMillis()
        val plan = planBaApNotificationSync(request, nowMs)
        var nextLastNotifiedLevel = plan.nextLastNotifiedLevel
        val thresholdNotificationSent = if (plan.shouldSendThresholdNotification) {
            sendThresholdNotification(
                context = context,
                request = plan.request,
            )
        } else {
            false
        }
        if (thresholdNotificationSent) {
            nextLastNotifiedLevel = plan.request.currentDisplay
        } else if (plan.shouldRefreshActiveNotification || plan.shouldSendThresholdNotification) {
            refreshActiveNotification(
                context = context,
                request = plan.request,
            )
        }
        return BaApNotificationSyncResult(
            lastNotifiedLevel = nextLastNotifiedLevel,
            suppressionAnchorAtMs =
                plan.nextSuppressionAnchorAtMs ?: nowMs.takeIf {
                    thresholdNotificationSent && plan.advanceSuppressionAnchorAfterDelivery
                },
        )
    }

    private suspend fun sendThresholdNotification(
        context: Context,
        request: BaApNotificationSyncRequest,
    ): Boolean {
        return withNotificationTimeout {
            BaApNotificationDispatcher.send(
                context = context,
                currentDisplay = request.currentDisplay,
                limitDisplay = request.limitDisplay,
                thresholdDisplay = request.thresholdDisplay,
                notificationId = request.notificationId,
                accountDisplayName = request.accountDisplayName,
                accountId = request.accountId,
            )
        }
    }

    private suspend fun refreshActiveNotification(
        context: Context,
        request: BaApNotificationSyncRequest,
    ) {
        withNotificationTimeout {
            BaApNotificationDispatcher.refreshIfActive(
                context = context,
                currentDisplay = request.currentDisplay,
                limitDisplay = request.limitDisplay,
                thresholdDisplay = request.thresholdDisplay,
                notificationId = request.notificationId,
                accountDisplayName = request.accountDisplayName,
                accountId = request.accountId,
            )
        }
    }

    private suspend fun withNotificationTimeout(block: () -> Boolean): Boolean {
        return withTimeoutOrNull(NOTIFICATION_SYNC_TIMEOUT_MS) {
            withContext(AppDispatchers.baFetch) { block() }
        } ?: false
    }

    internal fun BaApNotificationSyncRequest.normalized(): BaApNotificationSyncRequest {
        val normalizedLimit = limitDisplay.coerceIn(0, BA_AP_MAX)
        return copy(
            currentDisplay = currentDisplay.coerceIn(0, BA_AP_MAX),
            limitDisplay = normalizedLimit,
            thresholdDisplay = thresholdDisplay.coerceIn(0, BA_AP_MAX),
            lastNotifiedLevel = lastNotifiedLevel.coerceIn(-1, BA_AP_MAX),
        )
    }
}

internal fun planBaApNotificationSync(
    request: BaApNotificationSyncRequest,
    nowMs: Long = System.currentTimeMillis(),
): BaApNotificationSyncPlan {
    val normalizedRequest = with(BaApNotificationSyncCoordinator) { request.normalized() }
    val resetLastNotifiedLevel = (-1).takeIf { normalizedRequest.lastNotifiedLevel != -1 }
    val acknowledgement =
        BaApAcknowledgementPolicy.evaluate(
            notificationEnabled = normalizedRequest.notifyEnabled,
            currentDisplay = normalizedRequest.currentDisplay,
            thresholdDisplay = normalizedRequest.thresholdDisplay,
            keepReadUntilBelowThreshold = normalizedRequest.keepReadUntilBelowThreshold,
            suppressionAnchorAtMs = normalizedRequest.suppressionAnchorAtMs,
            nowMs = nowMs,
        )
    val resetSuppressionAnchor = 0L.takeIf { acknowledgement.resetSuppressionAnchor }
    if (acknowledgement.suppressed) {
        return BaApNotificationSyncPlan(
            request = normalizedRequest,
            shouldRefreshActiveNotification = false,
        )
    }
    if (!acknowledgement.eligible) {
        return BaApNotificationSyncPlan(
            request = normalizedRequest,
            nextLastNotifiedLevel = resetLastNotifiedLevel,
            nextSuppressionAnchorAtMs = resetSuppressionAnchor,
        )
    }
    if (
        !acknowledgement.bypassLastLevelDeduplication &&
        normalizedRequest.currentDisplay == normalizedRequest.lastNotifiedLevel
    ) {
        return BaApNotificationSyncPlan(request = normalizedRequest)
    }
    return BaApNotificationSyncPlan(
        request = normalizedRequest,
        shouldSendThresholdNotification = true,
        shouldRefreshActiveNotification = false,
        advanceSuppressionAnchorAfterDelivery = acknowledgement.advanceSuppressionAnchorAfterDelivery,
    )
}
