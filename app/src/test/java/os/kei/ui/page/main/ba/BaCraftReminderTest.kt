package os.kei.ui.page.main.ba

import org.junit.Test
import os.kei.ui.page.main.ba.support.BA_CRAFT_SLOT_COUNT
import os.kei.ui.page.main.ba.support.BaAccountId
import os.kei.ui.page.main.ba.support.BaCraftFunction
import os.kei.ui.page.main.ba.support.BaCraftGrade
import os.kei.ui.page.main.ba.support.BaCraftNotifiedMarkers
import os.kei.ui.page.main.ba.support.BaCraftSlot
import os.kei.ui.page.main.ba.support.BaCraftState
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import os.kei.ui.page.main.ba.support.withMarkerAt
import os.kei.ui.page.main.ba.support.withSlotAt
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val MINUTE = 60L * 1000L
private const val START = 1_700_000_000_000L

class BaCraftReminderTest {
    private fun snapshot(
        craft: BaCraftState,
        notified: BaCraftNotifiedMarkers = BaCraftNotifiedMarkers(),
        enabled: Boolean = true,
    ): BaPageSnapshot =
        BaPageSnapshot(
            craft = craft,
            craftNotified = notified,
            craftNotifyEnabled = enabled,
        )

    private fun oneLowCraft(function: BaCraftFunction = BaCraftFunction.Generate): BaCraftState =
        BaCraftState().withSlotAt(
            function,
            0,
            BaCraftSlot(startedAtMs = START, grades = listOf(BaCraftGrade.Low)),
        )

    @Test
    fun `disabled reminder reports nothing even when a slot elapsed`() {
        val plan =
            BaReminderCoordinator.evaluateCraft(
                snapshot = snapshot(craft = oneLowCraft(), enabled = false),
                nowMs = START + 45L * MINUTE,
            )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `an elapsed slot is reported once and then stays quiet`() {
        val craft = oneLowCraft()
        val nowMs = START + 45L * MINUTE

        val first = BaReminderCoordinator.evaluateCraft(snapshot(craft), nowMs)
        assertEquals(1, first.size)
        val completion = first.single()
        assertEquals(BaCraftFunction.Generate, completion.function)
        assertEquals(0, completion.index)
        assertEquals(START + 30L * MINUTE, completion.endAtMs)

        val marked =
            BaCraftNotifiedMarkers().withMarkerAt(
                function = completion.function,
                index = completion.index,
                endAtMs = completion.endAtMs,
            )
        assertTrue(BaReminderCoordinator.evaluateCraft(snapshot(craft, marked), nowMs).isEmpty())
    }

    @Test
    fun `reloading the same slot re-arms the reminder without an explicit reset`() {
        val nowMs = START + 45L * MINUTE
        val marked =
            BaCraftNotifiedMarkers().withMarkerAt(
                function = BaCraftFunction.Generate,
                index = 0,
                endAtMs = START + 30L * MINUTE,
            )
        // Same slot, loaded again later: a different end instant, so the marker no longer matches.
        val reloaded =
            BaCraftState().withSlotAt(
                BaCraftFunction.Generate,
                0,
                BaCraftSlot(
                    startedAtMs = START + 40L * MINUTE,
                    grades = listOf(BaCraftGrade.Low),
                ),
            )
        val plan =
            BaReminderCoordinator.evaluateCraft(
                snapshot = snapshot(reloaded, marked),
                nowMs = nowMs + 30L * MINUTE,
            )
        assertEquals(1, plan.size)
        assertNotEquals(START + 30L * MINUTE, plan.single().endAtMs)
    }

    @Test
    fun `both functions report independently`() {
        val craft =
            oneLowCraft(BaCraftFunction.Generate)
                .withSlotAt(
                    BaCraftFunction.Fusion,
                    2,
                    BaCraftSlot(startedAtMs = START, grades = listOf(BaCraftGrade.Low)),
                )
        val plan = BaReminderCoordinator.evaluateCraft(snapshot(craft), START + 45L * MINUTE)
        assertEquals(2, plan.size)
        assertEquals(
            setOf(BaCraftFunction.Generate to 0, BaCraftFunction.Fusion to 2),
            plan.map { it.function to it.index }.toSet(),
        )
    }

    @Test
    fun `craft notification ids clear the account range`() {
        // BaAccountNotificationIds spans 43_000 .. 43_000 + 99_999 * 4 + 3.
        val accountRangeEnd = 43_000 + 99_999 * 4 + 3
        val id =
            BaCraftNotificationIds.notificationId(
                accountId = BaAccountId("main"),
                function = BaCraftFunction.Generate,
                slotIndex = 0,
            )
        assertTrue(id > accountRangeEnd, "craft id $id collides with the account id range")
    }

    @Test
    fun `craft notification ids are distinct per function and slot and stable per account`() {
        val accountId = BaAccountId("main")
        val ids =
            BaCraftFunction.entries.flatMap { function ->
                (0 until BA_CRAFT_SLOT_COUNT).map { slot ->
                    BaCraftNotificationIds.notificationId(accountId, function, slot)
                }
            }
        assertEquals(2 * BA_CRAFT_SLOT_COUNT, ids.distinct().size)
        assertEquals(
            ids,
            BaCraftFunction.entries.flatMap { function ->
                (0 until BA_CRAFT_SLOT_COUNT).map { slot ->
                    BaCraftNotificationIds.notificationId(accountId, function, slot)
                }
            },
        )
    }

    @Test
    fun `craft notification ids differ between accounts`() {
        val first = BaCraftNotificationIds.notificationId(BaAccountId("a"), BaCraftFunction.Generate, 0)
        val second = BaCraftNotificationIds.notificationId(BaAccountId("b"), BaCraftFunction.Generate, 0)
        assertNotEquals(first, second)
    }

    @Test
    fun `an out of range slot index cannot escape its account bucket`() {
        val accountId = BaAccountId("main")
        val last =
            BaCraftNotificationIds.notificationId(accountId, BaCraftFunction.Fusion, BA_CRAFT_SLOT_COUNT - 1)
        assertEquals(
            last,
            BaCraftNotificationIds.notificationId(accountId, BaCraftFunction.Fusion, 99),
        )
    }
}
