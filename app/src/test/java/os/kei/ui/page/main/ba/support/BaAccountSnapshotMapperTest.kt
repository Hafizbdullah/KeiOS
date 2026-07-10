package os.kei.ui.page.main.ba.support

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaAccountSnapshotMapperTest {
    @Test
    fun `active account overrides identity runtime and reminder fields`() {
        val activeAccount =
            BaAccountRecord(
                profile =
                    BaAccountProfile(
                        id = BaAccountId("account-2"),
                        serverIndex = 1,
                        displayName = "Global",
                        nickname = "Global",
                        friendCode = "GLFRIEND",
                    ),
                runtime =
                    BaAccountRuntime(
                        apLimit = 180,
                        apCurrent = 87.5,
                        apRegenBaseMs = 1000L,
                        apSyncMs = 2000L,
                        cafeLevel = 8,
                        cafeStoredAp = 90.25,
                        cafeLastHourMs = 3000L,
                        coffeeHeadpatMs = 4000L,
                        coffeeInvite1UsedMs = 5000L,
                        coffeeInvite2UsedMs = 6000L,
                    ),
                reminderRuntime =
                    BaAccountReminderRuntime(
                        apLastNotifiedLevel = 120,
                        cafeApLastNotifiedLevel = 130,
                        arenaRefreshLastNotifiedSlotMs = 7000L,
                        cafeVisitLastNotifiedSlotMs = 8000L,
                    ),
            )
        val base =
            BaPageSnapshot(
                serverIndex = 2,
                idNickname = "Base",
                idFriendCode = "ABCDEFGH",
                showEndedActivities = true,
                showCalendarPoolImages = false,
                calendarUpcomingNotifyEnabled = true,
            )
        val state =
            BaAccountStoreSnapshot(
                accounts = listOf(activeAccount),
                activeAccountId = activeAccount.profile.id,
                allAccountsFollowGlobalNotificationSettings = true,
                globalReminderSettings =
                    BaGlobalReminderSettings(
                        apNotifyEnabled = true,
                        apNotifyThreshold = 160,
                        cafeApNotifyEnabled = true,
                        cafeApNotifyThreshold = 170,
                        arenaRefreshNotifyEnabled = true,
                        cafeVisitNotifyEnabled = true,
                    ),
            )

        val snapshot = base.withActiveBaAccount(state)

        assertEquals(1, snapshot.serverIndex)
        assertEquals("Global", snapshot.idNickname)
        assertEquals("GLFRIEND", snapshot.idFriendCode)
        assertEquals(180, snapshot.apLimit)
        assertEquals(87.5, snapshot.apCurrent)
        assertEquals(8, snapshot.cafeLevel)
        assertEquals(90.25, snapshot.cafeStoredAp)
        assertEquals(120, snapshot.apLastNotifiedLevel)
        assertEquals(130, snapshot.cafeApLastNotifiedLevel)
        assertEquals(7000L, snapshot.arenaRefreshLastNotifiedSlotMs)
        assertEquals(8000L, snapshot.cafeVisitLastNotifiedSlotMs)
        assertTrue(snapshot.apNotifyEnabled)
        assertEquals(160, snapshot.apNotifyThreshold)
        assertTrue(snapshot.cafeApNotifyEnabled)
        assertEquals(170, snapshot.cafeApNotifyThreshold)
        assertTrue(snapshot.arenaRefreshNotifyEnabled)
        assertTrue(snapshot.cafeVisitNotifyEnabled)
        assertTrue(snapshot.showEndedActivities)
        assertFalse(snapshot.showCalendarPoolImages)
        assertTrue(snapshot.calendarUpcomingNotifyEnabled)
    }

    @Test
    fun `active account snapshot loads its local AP acknowledgement anchors`() {
        val accountId = BaAccountId("account-active")
        val account =
            BaAccountRecord(
                profile =
                    BaAccountProfile(
                        id = accountId,
                        serverIndex = 0,
                        displayName = "Active",
                        nickname = "Active",
                        friendCode = "ACTIVE01",
                    ),
            )
        val state =
            BaAccountStoreSnapshot(
                accounts = listOf(account),
                activeAccountId = accountId,
                allAccountsFollowGlobalNotificationSettings = true,
                globalReminderSettings = BaGlobalReminderSettings(),
            )
        val acknowledgementStore = BaApAcknowledgementStore(InMemoryBaAccountKeyValueStore())
        acknowledgementStore.setSuppressionAnchor(accountId, BaApReminderKind.Ap, 1_000L)
        acknowledgementStore.setSuppressionAnchor(accountId, BaApReminderKind.CafeAp, 2_000L)

        val snapshot =
            BaPageSnapshot()
                .withActiveBaAccount(state)
                .withLocalApAcknowledgementAnchors(accountId, acknowledgementStore)

        assertEquals(1_000L, snapshot.apSuppressionAnchorAtMs)
        assertEquals(2_000L, snapshot.cafeApSuppressionAnchorAtMs)
    }

    @Test
    fun `reminder snapshots load independent local anchors for each account`() {
        val firstId = BaAccountId("account-first")
        val secondId = BaAccountId("account-second")
        val accounts =
            listOf(
                BaAccountRecord(
                    profile =
                        BaAccountProfile(
                            id = firstId,
                            serverIndex = 0,
                            displayName = "First",
                            nickname = "First",
                            friendCode = "FIRST001",
                        ),
                ),
                BaAccountRecord(
                    profile =
                        BaAccountProfile(
                            id = secondId,
                            serverIndex = 1,
                            displayName = "Second",
                            nickname = "Second",
                            friendCode = "SECOND01",
                        ),
                ),
            )
        val state =
            BaAccountStoreSnapshot(
                accounts = accounts,
                activeAccountId = firstId,
                allAccountsFollowGlobalNotificationSettings = true,
                globalReminderSettings = BaGlobalReminderSettings(),
            )
        val acknowledgementStore = BaApAcknowledgementStore(InMemoryBaAccountKeyValueStore())
        acknowledgementStore.setSuppressionAnchor(firstId, BaApReminderKind.Ap, 1_000L)
        acknowledgementStore.setSuppressionAnchor(firstId, BaApReminderKind.CafeAp, 2_000L)
        acknowledgementStore.setSuppressionAnchor(secondId, BaApReminderKind.Ap, 3_000L)
        acknowledgementStore.setSuppressionAnchor(secondId, BaApReminderKind.CafeAp, 4_000L)

        val snapshots =
            accounts.associate { account ->
                account.profile.id to
                    BaPageSnapshot()
                        .withBaAccount(state, account)
                        .withLocalApAcknowledgementAnchors(account.profile.id, acknowledgementStore)
            }

        assertEquals(1_000L, snapshots.getValue(firstId).apSuppressionAnchorAtMs)
        assertEquals(2_000L, snapshots.getValue(firstId).cafeApSuppressionAnchorAtMs)
        assertEquals(3_000L, snapshots.getValue(secondId).apSuppressionAnchorAtMs)
        assertEquals(4_000L, snapshots.getValue(secondId).cafeApSuppressionAnchorAtMs)
    }

    @Test
    fun `base snapshot is preserved when active account is missing`() {
        val base = BaPageSnapshot(serverIndex = 0, idNickname = "Base")
        val state =
            BaAccountStoreSnapshot(
                accounts = emptyList(),
                activeAccountId = null,
                allAccountsFollowGlobalNotificationSettings = true,
                globalReminderSettings = BaGlobalReminderSettings(),
            )

        assertEquals(base, base.withActiveBaAccount(state))
    }

    @Test
    fun `custom account override maps AP read suppression mode to snapshot`() {
        val accountId = BaAccountId("account-custom")
        val account =
            BaAccountRecord(
                profile =
                    BaAccountProfile(
                        id = accountId,
                        serverIndex = 1,
                        displayName = "Custom",
                        nickname = "Custom",
                        friendCode = "CUSTOM01",
                        notificationMode = BaAccountNotificationMode.Custom,
                    ),
                reminderOverride =
                    BaAccountReminderOverride(
                        accountId = accountId,
                        keepApRemindersReadUntilBelowThreshold = false,
                    ),
            )
        val state =
            BaAccountStoreSnapshot(
                accounts = listOf(account),
                activeAccountId = accountId,
                allAccountsFollowGlobalNotificationSettings = false,
                globalReminderSettings = BaGlobalReminderSettings(),
            )

        val snapshot = BaPageSnapshot().withActiveBaAccount(state)

        assertFalse(snapshot.keepApRemindersReadUntilBelowThreshold)
    }

    @Test
    fun `friend code sanitizer keeps uppercase letters for default server policy`() {
        assertEquals("ABCDARIS", normalizeBaAccountFriendCodeInput("a1-b2 c3_d4 arisu"))
        assertEquals("ABCDARIS", sanitizeBaAccountFriendCode("a1-b2 c3_d4 arisu"))
        assertEquals(BA_DEFAULT_FRIEND_CODE, sanitizeBaAccountFriendCode("A1B2"))
    }
}
