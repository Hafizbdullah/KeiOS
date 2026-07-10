package os.kei.ui.page.main.ba

internal const val BA_AP_READ_REPEAT_INTERVAL_MS = 60L * 60L * 1000L
internal const val BA_AP_DISMISS_SNOOZE_INTERVAL_MS = 60L * 60L * 1000L

internal data class BaApAcknowledgementDecision(
    val eligible: Boolean,
    val suppressed: Boolean = false,
    val resetSuppressionAnchor: Boolean = false,
    val resetDismissedUntil: Boolean = false,
    val nextEligibleAtMs: Long? = null,
    val bypassLastLevelDeduplication: Boolean = false,
    val advanceSuppressionAnchorAfterDelivery: Boolean = false,
    val clearDismissedUntilAfterDelivery: Boolean = false,
)

internal object BaApAcknowledgementPolicy {
    fun evaluate(
        notificationEnabled: Boolean,
        currentDisplay: Int,
        thresholdDisplay: Int,
        keepReadUntilBelowThreshold: Boolean,
        suppressionAnchorAtMs: Long,
        dismissedUntilAtMs: Long = 0L,
        nowMs: Long,
    ): BaApAcknowledgementDecision {
        val anchor = suppressionAnchorAtMs.coerceAtLeast(0L)
        val dismissedUntil = dismissedUntilAtMs.coerceAtLeast(0L)
        if (!notificationEnabled || currentDisplay < thresholdDisplay) {
            return BaApAcknowledgementDecision(
                eligible = false,
                resetSuppressionAnchor = anchor > 0L,
                resetDismissedUntil = dismissedUntil > 0L,
            )
        }
        if (anchor > 0L && keepReadUntilBelowThreshold) {
            return BaApAcknowledgementDecision(
                eligible = false,
                suppressed = true,
            )
        }
        val hourlyReadDeadline =
            if (anchor > 0L) {
                saturatedAdd(anchor, BA_AP_READ_REPEAT_INTERVAL_MS)
            } else {
                0L
            }
        val nextEligibleAtMs = maxOf(hourlyReadDeadline, dismissedUntil)
        if (nowMs < nextEligibleAtMs) {
            return BaApAcknowledgementDecision(
                eligible = false,
                suppressed = true,
                nextEligibleAtMs = nextEligibleAtMs,
            )
        }
        val hourlyReadExpired = hourlyReadDeadline > 0L
        val dismissalExpired = dismissedUntil > 0L
        if (!hourlyReadExpired && !dismissalExpired) {
            return BaApAcknowledgementDecision(eligible = true)
        }
        return BaApAcknowledgementDecision(
            eligible = true,
            bypassLastLevelDeduplication = true,
            advanceSuppressionAnchorAfterDelivery = hourlyReadExpired,
            clearDismissedUntilAfterDelivery = dismissalExpired,
        )
    }

    private fun saturatedAdd(value: Long, delta: Long): Long =
        if (value > Long.MAX_VALUE - delta) Long.MAX_VALUE else value + delta
}
