package os.kei.ui.page.main.ba.support

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val MINUTE = 60L * 1000L
private const val HOUR = 60L * MINUTE
private const val NOW = 1_700_000_000_000L

class BaDailyDonePlanTest {
    private fun snapshot(
        apCurrent: Double = 120.0,
        cafeStoredAp: Double = 300.0,
        coffeeHeadpatMs: Long = 0L,
        coffeeInvite1UsedMs: Long = 0L,
        coffeeInvite2UsedMs: Long = 0L,
        craft: BaCraftState = BaCraftState(),
        serverIndex: Int = 2,
    ): BaPageSnapshot =
        BaPageSnapshot(
            apCurrent = apCurrent,
            cafeStoredAp = cafeStoredAp,
            coffeeHeadpatMs = coffeeHeadpatMs,
            coffeeInvite1UsedMs = coffeeInvite1UsedMs,
            coffeeInvite2UsedMs = coffeeInvite2UsedMs,
            craft = craft,
            serverIndex = serverIndex,
        )

    @Test
    fun `both ap pools go to zero and re-base their anchors`() {
        val plan = planBaDailyDone(snapshot(), nowMs = NOW)
        assertEquals(0.0, plan.apCurrent)
        assertEquals(0.0, plan.cafeStoredAp)
        assertEquals(NOW, plan.apRegenBaseMs)
        assertEquals(NOW, plan.apSyncMs)
        assertEquals(floorToHourMs(NOW), plan.cafeLastHourMs)
        assertTrue(plan.outcome.apCleared)
        assertTrue(plan.outcome.cafeApCleared)
    }

    @Test
    fun `clearing is not a cafe claim - the cafe pool does not land in the player pool`() {
        val plan = planBaDailyDone(snapshot(apCurrent = 0.0, cafeStoredAp = 740.0), nowMs = NOW)
        assertEquals(0.0, plan.apCurrent)
        assertEquals(0.0, plan.cafeStoredAp)
    }

    @Test
    fun `notified levels reset so the next reminder is not deduped away`() {
        val plan = planBaDailyDone(snapshot(), nowMs = NOW)
        assertEquals(-1, plan.apLastNotifiedLevel)
        assertEquals(-1, plan.cafeApLastNotifiedLevel)
    }

    @Test
    fun `never used cooldowns all start`() {
        val plan = planBaDailyDone(snapshot(), nowMs = NOW)
        assertEquals(NOW, plan.coffeeHeadpatMs)
        assertEquals(NOW, plan.coffeeInvite1UsedMs)
        assertEquals(NOW, plan.coffeeInvite2UsedMs)
        assertTrue(plan.outcome.headpatStarted)
        assertTrue(plan.outcome.invite1Started)
        assertTrue(plan.outcome.invite2Started)
    }

    @Test
    fun `a running cooldown is left alone and not pushed out`() {
        val startedRecently = NOW - 30L * MINUTE
        val plan =
            planBaDailyDone(
                snapshot(
                    coffeeHeadpatMs = startedRecently,
                    coffeeInvite1UsedMs = startedRecently,
                    coffeeInvite2UsedMs = startedRecently,
                ),
                nowMs = NOW,
            )
        assertEquals(startedRecently, plan.coffeeHeadpatMs)
        assertEquals(startedRecently, plan.coffeeInvite1UsedMs)
        assertEquals(startedRecently, plan.coffeeInvite2UsedMs)
        assertFalse(plan.outcome.headpatStarted)
        assertFalse(plan.outcome.invite1Started)
        assertFalse(plan.outcome.invite2Started)
    }

    @Test
    fun `an elapsed invite cooldown restarts`() {
        // The invite cooldown is 20h; 21h ago is comfortably done.
        val plan = planBaDailyDone(snapshot(coffeeInvite1UsedMs = NOW - 21L * HOUR), nowMs = NOW)
        assertEquals(NOW, plan.coffeeInvite1UsedMs)
        assertTrue(plan.outcome.invite1Started)
    }

    @Test
    fun `an elapsed headpat cooldown restarts`() {
        // The headpat cooldown is 3h, and it also frees at the cafe student refresh.
        val plan = planBaDailyDone(snapshot(coffeeHeadpatMs = NOW - 4L * HOUR), nowMs = NOW)
        assertEquals(NOW, plan.coffeeHeadpatMs)
        assertTrue(plan.outcome.headpatStarted)
    }

    @Test
    fun `two generate slots are loaded with one advanced node each`() {
        val plan = planBaDailyDone(snapshot(), nowMs = NOW)
        assertEquals(BA_DAILY_DONE_CRAFT_SLOTS, plan.outcome.craftSlotsStarted)
        repeat(BA_DAILY_DONE_CRAFT_SLOTS) { index ->
            val slot = plan.craft.slotAt(BaCraftFunction.Generate, index)
            assertEquals(listOf(BaCraftGrade.High), slot.grades)
            assertEquals(NOW, slot.startedAtMs)
            assertEquals(NOW + 3L * HOUR, slot.endAtMs())
        }
    }

    @Test
    fun `the third generate slot and every fusion slot are left untouched`() {
        val plan = planBaDailyDone(snapshot(), nowMs = NOW)
        assertFalse(plan.craft.slotAt(BaCraftFunction.Generate, 2).isActive())
        repeat(BA_CRAFT_SLOT_COUNT) { index ->
            assertFalse(plan.craft.slotAt(BaCraftFunction.Fusion, index).isActive())
        }
    }

    @Test
    fun `a craft still counting down is not overwritten`() {
        val running =
            BaCraftState().withSlotAt(
                BaCraftFunction.Generate,
                0,
                BaCraftSlot(startedAtMs = NOW - HOUR, grades = List(3) { BaCraftGrade.Highest }),
            )
        val plan = planBaDailyDone(snapshot(craft = running), nowMs = NOW)
        // Slot 0 keeps its 18h craft; only slot 1 was free.
        assertEquals(1, plan.outcome.craftSlotsStarted)
        val kept = plan.craft.slotAt(BaCraftFunction.Generate, 0)
        assertEquals(3, kept.grades.size)
        assertEquals(NOW - HOUR, kept.startedAtMs)
    }

    @Test
    fun `a finished craft counts as free and is reloaded`() {
        val finished =
            BaCraftState().withSlotAt(
                BaCraftFunction.Generate,
                0,
                // A 30m craft started 5h ago: collected long since.
                BaCraftSlot(startedAtMs = NOW - 5L * HOUR, grades = listOf(BaCraftGrade.Low)),
            )
        val plan = planBaDailyDone(snapshot(craft = finished), nowMs = NOW)
        assertEquals(2, plan.outcome.craftSlotsStarted)
        val reloaded = plan.craft.slotAt(BaCraftFunction.Generate, 0)
        assertEquals(NOW, reloaded.startedAtMs)
        assertEquals(listOf(BaCraftGrade.High), reloaded.grades)
    }

    @Test
    fun `applying twice in a row changes nothing the second time`() {
        val first = planBaDailyDone(snapshot(), nowMs = NOW)
        val after =
            snapshot(
                apCurrent = first.apCurrent,
                cafeStoredAp = first.cafeStoredAp,
                coffeeHeadpatMs = first.coffeeHeadpatMs,
                coffeeInvite1UsedMs = first.coffeeInvite1UsedMs,
                coffeeInvite2UsedMs = first.coffeeInvite2UsedMs,
                craft = first.craft,
            )
        // One minute later, nothing has had time to come off cooldown.
        val second = planBaDailyDone(after, nowMs = NOW + MINUTE)
        assertFalse(second.outcome.changedAnything)
        assertEquals(0, second.outcome.craftSlotsStarted)
        assertEquals(first.coffeeHeadpatMs, second.coffeeHeadpatMs)
        assertEquals(first.craft, second.craft)
    }

    @Test
    fun `a fully spent account reports nothing changed`() {
        val plan =
            planBaDailyDone(
                snapshot(
                    apCurrent = 0.0,
                    cafeStoredAp = 0.0,
                    coffeeHeadpatMs = NOW - MINUTE,
                    coffeeInvite1UsedMs = NOW - MINUTE,
                    coffeeInvite2UsedMs = NOW - MINUTE,
                    craft =
                        BaCraftState()
                            .withSlotAt(
                                BaCraftFunction.Generate,
                                0,
                                BaCraftSlot(startedAtMs = NOW, grades = listOf(BaCraftGrade.High)),
                            )
                            .withSlotAt(
                                BaCraftFunction.Generate,
                                1,
                                BaCraftSlot(startedAtMs = NOW, grades = listOf(BaCraftGrade.High)),
                            ),
                ),
                nowMs = NOW,
            )
        assertFalse(plan.outcome.changedAnything)
    }

    @Test
    fun `the headpat rule follows the account server`() {
        // Same timestamp, different server: the cafe refresh boundary differs, so readiness can differ.
        val headpatMs = NOW - 4L * HOUR
        val cn = planBaDailyDone(snapshot(coffeeHeadpatMs = headpatMs, serverIndex = 0), nowMs = NOW)
        val jp = planBaDailyDone(snapshot(coffeeHeadpatMs = headpatMs, serverIndex = 2), nowMs = NOW)
        // Both are past the 3h cooldown, so both restart — the point is that serverIndex reaches the rule
        // at all rather than being silently dropped.
        assertEquals(NOW, cn.coffeeHeadpatMs)
        assertEquals(NOW, jp.coffeeHeadpatMs)
    }
}
