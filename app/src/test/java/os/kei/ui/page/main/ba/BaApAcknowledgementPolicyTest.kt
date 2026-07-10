package os.kei.ui.page.main.ba

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BaApAcknowledgementPolicyTest {
    @Test
    fun `persistent read suppresses while AP remains above threshold`() {
        val decision =
            BaApAcknowledgementPolicy.evaluate(
                notificationEnabled = true,
                currentDisplay = 130,
                thresholdDisplay = 120,
                keepReadUntilBelowThreshold = true,
                suppressionAnchorAtMs = NOW_MS - 10_000L,
                nowMs = NOW_MS,
            )

        assertTrue(decision.suppressed)
        assertNull(decision.nextEligibleAtMs)
        assertFalse(decision.resetSuppressionAnchor)
    }

    @Test
    fun `hourly read schedules exact one hour boundary`() {
        val anchor = NOW_MS - 30L * 60L * 1000L
        val decision =
            BaApAcknowledgementPolicy.evaluate(
                notificationEnabled = true,
                currentDisplay = 130,
                thresholdDisplay = 120,
                keepReadUntilBelowThreshold = false,
                suppressionAnchorAtMs = anchor,
                nowMs = NOW_MS,
            )

        assertTrue(decision.suppressed)
        assertEquals(anchor + BA_AP_READ_REPEAT_INTERVAL_MS, decision.nextEligibleAtMs)
    }

    @Test
    fun `expired hourly read bypasses level dedupe and advances after delivery`() {
        val decision =
            BaApAcknowledgementPolicy.evaluate(
                notificationEnabled = true,
                currentDisplay = 130,
                thresholdDisplay = 120,
                keepReadUntilBelowThreshold = false,
                suppressionAnchorAtMs = NOW_MS - BA_AP_READ_REPEAT_INTERVAL_MS,
                nowMs = NOW_MS,
            )

        assertFalse(decision.suppressed)
        assertTrue(decision.bypassLastLevelDeduplication)
        assertTrue(decision.advanceSuppressionAnchorAfterDelivery)
    }

    @Test
    fun `below threshold resets read state`() {
        val decision =
            BaApAcknowledgementPolicy.evaluate(
                notificationEnabled = true,
                currentDisplay = 119,
                thresholdDisplay = 120,
                keepReadUntilBelowThreshold = true,
                suppressionAnchorAtMs = NOW_MS,
                nowMs = NOW_MS,
            )

        assertTrue(decision.resetSuppressionAnchor)
        assertFalse(decision.eligible)
    }

    private companion object {
        private const val NOW_MS = 20_000_000L
    }
}
