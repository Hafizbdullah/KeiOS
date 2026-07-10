package os.kei.ui.page.main.ba.support

import org.junit.Test
import os.kei.core.background.AppBackgroundSchedulePolicy
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BASettingsStoreApAcknowledgementReconciliationTest {
    @Test
    fun `disabled account persists ordinary and cafe acknowledgement resets`() {
        val fixture =
            fixture(
                account =
                    account(
                        accountId = BaAccountId("disabled"),
                        enabled = false,
                        ap = 130.0,
                        cafeAp = 130.0,
                        apLastNotifiedLevel = 130,
                        cafeApLastNotifiedLevel = 130,
                    ),
            )
        fixture.seedAnchors(ap = 1_000L, cafeAp = 2_000L)

        val changed = fixture.reconcile()

        assertTrue(changed)
        fixture.assertResetState()
    }

    @Test
    fun `below threshold persists ordinary and cafe acknowledgement resets`() {
        val fixture =
            fixture(
                account =
                    account(
                        accountId = BaAccountId("below"),
                        enabled = true,
                        ap = 119.0,
                        cafeAp = 119.0,
                        apLastNotifiedLevel = 119,
                        cafeApLastNotifiedLevel = 119,
                    ),
            )
        fixture.seedAnchors(ap = 3_000L, cafeAp = 4_000L)

        val changed = fixture.reconcile()

        assertTrue(changed)
        fixture.assertResetState()
    }

    @Test
    fun `disabled stale AP level at cap becomes schedulable after reenable`() {
        val fixture =
            fixture(
                account =
                    account(
                        accountId = BaAccountId("cap"),
                        enabled = false,
                        ap = 240.0,
                        cafeAp = 0.0,
                        apLastNotifiedLevel = 240,
                        cafeApLastNotifiedLevel = -1,
                    ),
            )
        fixture.seedAnchors(ap = 5_000L, cafeAp = 0L)

        assertTrue(fixture.reconcile())
        val disabled = fixture.accountStore.loadAccounts().single()
        assertTrue(
            fixture.accountStore.updateAccount(
                disabled.copy(profile = disabled.profile.copy(enabled = true)),
            ),
        )
        val accountState = fixture.accountStore.loadState()
        val enabled = accountState.accounts.single()
        val snapshot =
            BaPageSnapshot()
                .withBaAccount(accountState = accountState, account = enabled)
                .withLocalApAcknowledgementAnchors(
                    accountId = enabled.profile.id,
                    acknowledgementStore = fixture.acknowledgementStore,
                )

        val schedule =
            assertNotNull(
                AppBackgroundSchedulePolicy.nextBaReminderSchedule(
                    snapshot = snapshot,
                    nowMs = NOW_MS,
                ),
            )

        assertEquals(NOW_MS, schedule.triggerAtMillis)
    }

    private fun fixture(account: BaAccountRecord): ReconciliationFixture {
        val backingStore = InMemoryBaAccountKeyValueStore()
        val accountStore = BaAccountStore(backingStore)
        accountStore.replaceAll(
            accounts = listOf(account),
            activeAccountId = account.profile.id,
        )
        accountStore.saveGlobalReminderSettings(
            BaGlobalReminderSettings(
                apNotifyEnabled = true,
                apNotifyThreshold = 120,
                cafeApNotifyEnabled = true,
                cafeApNotifyThreshold = 120,
                keepApRemindersReadUntilBelowThreshold = true,
            ),
        )
        return ReconciliationFixture(
            accountStore = accountStore,
            acknowledgementStore = BaApAcknowledgementStore(backingStore),
            accountId = account.profile.id,
        )
    }

    private fun account(
        accountId: BaAccountId,
        enabled: Boolean,
        ap: Double,
        cafeAp: Double,
        apLastNotifiedLevel: Int,
        cafeApLastNotifiedLevel: Int,
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
                    apLimit = 240,
                    apCurrent = ap,
                    apRegenBaseMs = NOW_MS,
                    cafeStoredAp = cafeAp,
                    cafeLastHourMs = NOW_MS,
                ),
            reminderRuntime =
                BaAccountReminderRuntime(
                    apLastNotifiedLevel = apLastNotifiedLevel,
                    cafeApLastNotifiedLevel = cafeApLastNotifiedLevel,
                ),
        )

    private data class ReconciliationFixture(
        val accountStore: BaAccountStore,
        val acknowledgementStore: BaApAcknowledgementStore,
        val accountId: BaAccountId,
    ) {
        fun seedAnchors(ap: Long, cafeAp: Long) {
            acknowledgementStore.setSuppressionAnchor(accountId, BaApReminderKind.Ap, ap)
            acknowledgementStore.setSuppressionAnchor(accountId, BaApReminderKind.CafeAp, cafeAp)
        }

        fun reconcile(): Boolean =
            BASettingsStore.reconcileApAcknowledgements(
                accountState = accountStore.loadState(),
                baseSnapshot = BaPageSnapshot(),
                accountStore = accountStore,
                acknowledgementStore = acknowledgementStore,
                nowMs = NOW_MS,
            )

        fun assertResetState() {
            val persisted = accountStore.loadAccounts().single().reminderRuntime
            assertEquals(-1, persisted.apLastNotifiedLevel)
            assertEquals(-1, persisted.cafeApLastNotifiedLevel)
            assertEquals(
                0L,
                acknowledgementStore.loadSuppressionAnchor(accountId, BaApReminderKind.Ap),
            )
            assertEquals(
                0L,
                acknowledgementStore.loadSuppressionAnchor(accountId, BaApReminderKind.CafeAp),
            )
        }
    }

    private companion object {
        private const val NOW_MS = 20_000_000L
    }
}
