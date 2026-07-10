package os.kei.core.background

import org.junit.Test
import os.kei.ui.page.main.ba.support.BaAccountId
import os.kei.ui.page.main.ba.support.BaAccountReminderSnapshot
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppBackgroundSchedulerBaReminderScheduleTest {
    @Test
    fun `BA reminder scheduling uses snapshots loaded after acknowledgement reconciliation`() {
        val accountId = BaAccountId("main")
        var reconciled = false
        val plan =
            AppBackgroundScheduler.buildBaReminderScheduleAfterAcknowledgementReconciliation(
                nowMs = NOW_MS,
                reconcile = {
                    reconciled = true
                    true
                },
                loadReminderSnapshots = {
                    listOf(
                        BaAccountReminderSnapshot(
                            accountId = accountId,
                            displayName = "Main",
                            snapshot =
                                if (reconciled) {
                                    aboveThresholdSnapshot(anchorAtMs = 0L)
                                } else {
                                    aboveThresholdSnapshot(anchorAtMs = NOW_MS - 10_000L)
                                },
                        ),
                    )
                },
            )

        assertTrue(plan.reconciled)
        val schedule = assertNotNull(plan.schedule)
        assertEquals(NOW_MS, schedule.triggerAtMillis)
    }

    private fun aboveThresholdSnapshot(anchorAtMs: Long): BaPageSnapshot =
        BaPageSnapshot(
            apNotifyEnabled = true,
            apCurrent = 130.0,
            apRegenBaseMs = NOW_MS,
            apNotifyThreshold = 120,
            apLimit = 240,
            keepApRemindersReadUntilBelowThreshold = true,
            apSuppressionAnchorAtMs = anchorAtMs,
            apLastNotifiedLevel = -1,
        )

    private companion object {
        private const val NOW_MS = 20_000_000L
    }
}
