package os.kei.ui.page.main.ba

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaApNotificationSyncCoordinatorTest {
    @Test
    fun `foreground persistent read suppresses regenerated AP`() {
        val plan =
            planBaApNotificationSync(
                request =
                    request(
                        currentDisplay = 121,
                        lastNotifiedLevel = 120,
                        keepReadUntilBelowThreshold = true,
                        suppressionAnchorAtMs = NOW_MS - 1_000L,
                    ),
                nowMs = NOW_MS,
            )

        assertFalse(plan.shouldSendThresholdNotification)
        assertFalse(plan.shouldRefreshActiveNotification)
    }

    @Test
    fun `foreground expired hourly read sends and advances anchor`() {
        val plan =
            planBaApNotificationSync(
                request =
                    request(
                        currentDisplay = 120,
                        lastNotifiedLevel = 120,
                        keepReadUntilBelowThreshold = false,
                        suppressionAnchorAtMs = NOW_MS - BA_AP_READ_REPEAT_INTERVAL_MS,
                    ),
                nowMs = NOW_MS,
            )

        assertTrue(plan.shouldSendThresholdNotification)
        assertTrue(plan.advanceSuppressionAnchorAfterDelivery)
    }

    @Test
    fun `foreground below threshold clears local read state`() {
        val plan =
            planBaApNotificationSync(
                request = request(currentDisplay = 119, suppressionAnchorAtMs = NOW_MS),
                nowMs = NOW_MS,
            )

        assertEquals(0L, plan.nextSuppressionAnchorAtMs)
        assertEquals(-1, plan.nextLastNotifiedLevel)
    }

    private fun request(
        currentDisplay: Int = 120,
        lastNotifiedLevel: Int = 120,
        keepReadUntilBelowThreshold: Boolean = true,
        suppressionAnchorAtMs: Long = 0L,
    ): BaApNotificationSyncRequest =
        BaApNotificationSyncRequest(
            currentDisplay = currentDisplay,
            limitDisplay = 240,
            thresholdDisplay = 120,
            notifyEnabled = true,
            lastNotifiedLevel = lastNotifiedLevel,
            keepReadUntilBelowThreshold = keepReadUntilBelowThreshold,
            suppressionAnchorAtMs = suppressionAnchorAtMs,
        )

    private companion object {
        const val NOW_MS = 10_000_000L
    }
}
