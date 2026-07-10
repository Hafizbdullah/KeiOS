package os.kei.ui.page.main.ba.support

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BASettingsStoreApAcknowledgementReconciliationTest {
    @Test
    fun `reconcile clears disabled and below threshold anchors while preserving active above threshold anchors`() {
        val backingStore = InMemoryBaAccountKeyValueStore()
        val acknowledgementStore = BaApAcknowledgementStore(backingStore)
        val disabledId = BaAccountId("disabled")
        val belowId = BaAccountId("below")
        val aboveId = BaAccountId("above")
        acknowledgementStore.setSuppressionAnchor(disabledId, BaApReminderKind.Ap, 1_000L)
        acknowledgementStore.setSuppressionAnchor(disabledId, BaApReminderKind.CafeAp, 2_000L)
        acknowledgementStore.setSuppressionAnchor(belowId, BaApReminderKind.Ap, 3_000L)
        acknowledgementStore.setSuppressionAnchor(belowId, BaApReminderKind.CafeAp, 4_000L)
        acknowledgementStore.setSuppressionAnchor(aboveId, BaApReminderKind.Ap, 5_000L)
        acknowledgementStore.setSuppressionAnchor(aboveId, BaApReminderKind.CafeAp, 6_000L)

        val changed = BASettingsStore.reconcileApAcknowledgements(
            accountState =
                BaAccountStoreSnapshot(
                    accounts =
                        listOf(
                            account(disabledId, enabled = false, ap = 130.0, cafeAp = 130.0),
                            account(belowId, enabled = true, ap = 119.0, cafeAp = 119.0),
                            account(aboveId, enabled = true, ap = 130.0, cafeAp = 130.0),
                        ),
                    activeAccountId = aboveId,
                    allAccountsFollowGlobalNotificationSettings = true,
                    globalReminderSettings =
                        BaGlobalReminderSettings(
                            apNotifyEnabled = true,
                            apNotifyThreshold = 120,
                            cafeApNotifyEnabled = true,
                            cafeApNotifyThreshold = 120,
                            keepApRemindersReadUntilBelowThreshold = true,
                        ),
                ),
            baseSnapshot = BaPageSnapshot(),
            acknowledgementStore = acknowledgementStore,
            nowMs = NOW_MS,
        )

        assertTrue(changed)
        assertEquals(0L, acknowledgementStore.loadSuppressionAnchor(disabledId, BaApReminderKind.Ap))
        assertEquals(0L, acknowledgementStore.loadSuppressionAnchor(disabledId, BaApReminderKind.CafeAp))
        assertEquals(0L, acknowledgementStore.loadSuppressionAnchor(belowId, BaApReminderKind.Ap))
        assertEquals(0L, acknowledgementStore.loadSuppressionAnchor(belowId, BaApReminderKind.CafeAp))
        assertEquals(5_000L, acknowledgementStore.loadSuppressionAnchor(aboveId, BaApReminderKind.Ap))
        assertEquals(6_000L, acknowledgementStore.loadSuppressionAnchor(aboveId, BaApReminderKind.CafeAp))
    }

    private fun account(
        accountId: BaAccountId,
        enabled: Boolean,
        ap: Double,
        cafeAp: Double,
    ): BaAccountRecord =
        BaAccountRecord(
            profile =
                BaAccountProfile(
                    id = accountId,
                    serverIndex = 2,
                    displayName = accountId.value,
                    nickname = accountId.value,
                    friendCode = "ABC12345",
                    enabled = enabled,
                ),
            runtime =
                BaAccountRuntime(
                    apCurrent = ap,
                    apRegenBaseMs = NOW_MS,
                    cafeStoredAp = cafeAp,
                    cafeLastHourMs = NOW_MS,
                ),
        )

    private companion object {
        private const val NOW_MS = 20_000_000L
    }
}
