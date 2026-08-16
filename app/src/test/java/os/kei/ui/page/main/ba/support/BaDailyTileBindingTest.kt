package os.kei.ui.page.main.ba.support

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun id(value: String) = BaAccountId(value)

class BaDailyTileBindingTest {
    @Test
    fun `normalizing pads to the declared pool size`() {
        val state = BaDailyTileState(slotAccountIds = listOf("a")).normalized()
        assertEquals(BA_DAILY_TILE_SLOTS, state.slotAccountIds.size)
        assertEquals(id("a"), state.accountIdAt(0))
        assertNull(state.accountIdAt(1))
    }

    @Test
    fun `blank and whitespace slots read as unclaimed`() {
        val state = BaDailyTileState(slotAccountIds = listOf("", "   ", "b")).normalized()
        assertNull(state.accountIdAt(0))
        assertNull(state.accountIdAt(1))
        assertEquals(id("b"), state.accountIdAt(2))
        assertEquals(listOf(2), state.boundSlots())
    }

    @Test
    fun `claiming a slot records it and reports the free one`() {
        var state = BaDailyTileState().normalized()
        assertEquals(0, state.firstFreeSlot())
        state = state.withSlot(0, id("a"))
        assertEquals(id("a"), state.accountIdAt(0))
        assertEquals(1, state.firstFreeSlot())
        assertEquals(0, state.slotOf(id("a")))
    }

    @Test
    fun `an account can hold only one slot so re-claiming releases the old one`() {
        var state = BaDailyTileState().normalized().withSlot(0, id("a"))
        state = state.withSlot(2, id("a"))
        assertNull(state.accountIdAt(0))
        assertEquals(id("a"), state.accountIdAt(2))
        assertEquals(listOf(2), state.boundSlots())
    }

    @Test
    fun `releasing a slot leaves the others alone`() {
        var state =
            BaDailyTileState().normalized()
                .withSlot(0, id("a"))
                .withSlot(1, id("b"))
        state = state.withSlot(0, null)
        assertNull(state.accountIdAt(0))
        assertEquals(id("b"), state.accountIdAt(1))
    }

    @Test
    fun `a full pool reports no free slot`() {
        val state =
            BaDailyTileState().normalized()
                .withSlot(0, id("a"))
                .withSlot(1, id("b"))
                .withSlot(2, id("c"))
        assertNull(state.firstFreeSlot())
        assertEquals(listOf(0, 1, 2), state.boundSlots())
    }

    @Test
    fun `writing out of range is ignored`() {
        val state = BaDailyTileState().normalized()
        assertEquals(state, state.withSlot(BA_DAILY_TILE_SLOTS, id("a")))
        assertEquals(state, state.withSlot(-1, id("a")))
    }

    @Test
    fun `a deleted account frees its slot`() {
        val state =
            BaDailyTileState().normalized()
                .withSlot(0, id("a"))
                .withSlot(1, id("b"))
        val synced = state.retainingExistingAccounts(listOf(id("b")))
        assertNull(synced.accountIdAt(0))
        assertEquals(id("b"), synced.accountIdAt(1))
        assertTrue(state.needsSyncFor(listOf(id("b"))))
    }

    @Test
    fun `a disabled account keeps its slot`() {
        // Only deletion frees a slot. Disabling is reversible, and surrendering the tile could be
        // permanent because the add request is rate limited per component.
        val state = BaDailyTileState().normalized().withSlot(0, id("a"))
        val stillThere = state.retainingExistingAccounts(listOf(id("a")))
        assertEquals(id("a"), stillThere.accountIdAt(0))
        assertFalse(state.needsSyncFor(listOf(id("a"))))
    }

    @Test
    fun `an unchanged account set needs no sync`() {
        val state =
            BaDailyTileState().normalized()
                .withSlot(0, id("a"))
                .withSlot(2, id("c"))
        assertFalse(state.needsSyncFor(listOf(id("a"), id("c"), id("unbound"))))
    }

    @Test
    fun `mode defaults to all accounts and round trips`() {
        assertEquals(BaDailyTileMode.AllAccounts, BaDailyTileState().mode)
        val perAccount = BaDailyTileState(mode = BaDailyTileMode.PerAccount).normalized()
        assertEquals(BaDailyTileMode.PerAccount, perAccount.mode)
    }

    @Test
    fun `the fingerprint tracks identity and enabled state but not runtime`() {
        fun snapshot(
            displayName: String = "Main",
            enabled: Boolean = true,
            apCurrent: Double = 0.0,
        ): BaAccountStoreSnapshot =
            BaAccountStoreSnapshot(
                accounts =
                    listOf(
                        BaAccountRecord(
                            profile =
                                BaAccountProfile(
                                    id = id("a"),
                                    serverIndex = 2,
                                    displayName = displayName,
                                    nickname = "n",
                                    friendCode = "ARISUKEI",
                                    enabled = enabled,
                                ),
                            runtime = BaAccountRuntime(apCurrent = apCurrent),
                        ),
                    ),
                activeAccountId = id("a"),
                allAccountsFollowGlobalNotificationSettings = true,
                globalReminderSettings = BaGlobalReminderSettings(),
            )

        val base = baDailyTileFingerprint(snapshot())
        // An AP tick bumps the change signal but must not trigger a re-label.
        assertEquals(base, baDailyTileFingerprint(snapshot(apCurrent = 137.0)))
        // A rename or an enable flip must.
        assertTrue(base != baDailyTileFingerprint(snapshot(displayName = "Alt")))
        assertTrue(base != baDailyTileFingerprint(snapshot(enabled = false)))
    }

    @Test
    fun `the fingerprint is stable against list order`() {
        fun record(idValue: String, sortOrder: Int) =
            BaAccountRecord(
                profile =
                    BaAccountProfile(
                        id = id(idValue),
                        serverIndex = 2,
                        displayName = idValue,
                        nickname = idValue,
                        friendCode = "ARISUKEI",
                        sortOrder = sortOrder,
                    ),
            )

        fun snapshot(records: List<BaAccountRecord>) =
            BaAccountStoreSnapshot(
                accounts = records,
                activeAccountId = id("a"),
                allAccountsFollowGlobalNotificationSettings = true,
                globalReminderSettings = BaGlobalReminderSettings(),
            )

        val a = record("a", 0)
        val b = record("b", 1)
        assertEquals(
            baDailyTileFingerprint(snapshot(listOf(a, b))),
            baDailyTileFingerprint(snapshot(listOf(b, a))),
        )
    }
}
