package os.kei.ui.page.main.ba

internal const val BA_AP_READ_REPEAT_INTERVAL_MS = 60L * 60L * 1000L

internal data class BaApAcknowledgementDecision(
    val eligible: Boolean,
    val suppressed: Boolean = false,
    val resetSuppressionAnchor: Boolean = false,
    val nextEligibleAtMs: Long? = null,
    val bypassLastLevelDeduplication: Boolean = false,
    val advanceSuppressionAnchorAfterDelivery: Boolean = false,
)

internal object BaApAcknowledgementPolicy {
    fun evaluate(
        notificationEnabled: Boolean,
        currentDisplay: Int,
        thresholdDisplay: Int,
        keepReadUntilBelowThreshold: Boolean,
        suppressionAnchorAtMs: Long,
        nowMs: Long,
    ): BaApAcknowledgementDecision {
        val anchor = suppressionAnchorAtMs.coerceAtLeast(0L)
        if (!notificationEnabled || currentDisplay < thresholdDisplay) {
            return BaApAcknowledgementDecision(
                eligible = false,
                resetSuppressionAnchor = anchor > 0L,
            )
        }
        if (anchor <= 0L) {
            return BaApAcknowledgementDecision(eligible = true)
        }
        if (keepReadUntilBelowThreshold) {
            return BaApAcknowledgementDecision(
                eligible = false,
                suppressed = true,
            )
        }
        val nextEligibleAtMs =
            if (anchor > Long.MAX_VALUE - BA_AP_READ_REPEAT_INTERVAL_MS) {
                Long.MAX_VALUE
            } else {
                anchor + BA_AP_READ_REPEAT_INTERVAL_MS
            }
        if (nowMs < nextEligibleAtMs) {
            return BaApAcknowledgementDecision(
                eligible = false,
                suppressed = true,
                nextEligibleAtMs = nextEligibleAtMs,
            )
        }
        return BaApAcknowledgementDecision(
            eligible = true,
            bypassLastLevelDeduplication = true,
            advanceSuppressionAnchorAfterDelivery = true,
        )
    }
}
