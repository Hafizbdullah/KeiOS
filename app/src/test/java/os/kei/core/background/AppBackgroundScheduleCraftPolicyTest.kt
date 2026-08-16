package os.kei.core.background

import org.junit.Test
import os.kei.ui.page.main.ba.support.BaCraftFunction
import os.kei.ui.page.main.ba.support.BaCraftGrade
import os.kei.ui.page.main.ba.support.BaCraftNotifiedMarkers
import os.kei.ui.page.main.ba.support.BaCraftSlot
import os.kei.ui.page.main.ba.support.BaCraftState
import os.kei.ui.page.main.ba.support.BaPageSnapshot
import os.kei.ui.page.main.ba.support.withMarkerAt
import os.kei.ui.page.main.ba.support.withSlotAt
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val MINUTE = 60L * 1000L
private const val HOUR = 60L * MINUTE
private const val START = 1_700_000_000_000L

class AppBackgroundScheduleCraftPolicyTest {
    private fun craftSnapshot(
        craft: BaCraftState,
        notified: BaCraftNotifiedMarkers = BaCraftNotifiedMarkers(),
        enabled: Boolean = true,
    ): BaPageSnapshot =
        BaPageSnapshot(
            craft = craft,
            craftNotified = notified,
            craftNotifyEnabled = enabled,
        )

    private fun loaded(
        function: BaCraftFunction,
        index: Int,
        grade: BaCraftGrade,
        startedAtMs: Long = START,
    ): BaCraftState =
        BaCraftState().withSlotAt(
            function,
            index,
            BaCraftSlot(startedAtMs = startedAtMs, grades = listOf(grade)),
        )

    @Test
    fun `a craft reminder alone enables BA scheduling`() {
        assertTrue(
            AppBackgroundSchedulePolicy.hasEnabledBaReminder(
                craftSnapshot(loaded(BaCraftFunction.Generate, 0, BaCraftGrade.Low)),
            ),
        )
        assertFalse(
            AppBackgroundSchedulePolicy.hasEnabledBaReminder(
                craftSnapshot(
                    craft = loaded(BaCraftFunction.Generate, 0, BaCraftGrade.Low),
                    enabled = false,
                ),
            ),
        )
    }

    @Test
    fun `a pending craft schedules at the slot end with prompt precision`() {
        val schedule =
            AppBackgroundSchedulePolicy.nextBaReminderSchedule(
                snapshot = craftSnapshot(loaded(BaCraftFunction.Generate, 1, BaCraftGrade.High)),
                nowMs = START + MINUTE,
            )
        val plan = assertNotNull(schedule)
        assertEquals(START + 3L * HOUR, plan.triggerAtMillis)
        // Windowed would slip 10-30 minutes on Android 17, which a 30-minute craft cannot absorb.
        assertEquals(BackgroundAlarmPrecision.Prompt, plan.precision)
        assertEquals(BackgroundAlarmWorkload.UserReminder, plan.workload)
    }

    @Test
    fun `the earliest slot across both functions wins`() {
        val craft =
            loaded(BaCraftFunction.Generate, 0, BaCraftGrade.Highest)
                .withSlotAt(
                    BaCraftFunction.Fusion,
                    2,
                    BaCraftSlot(startedAtMs = START, grades = listOf(BaCraftGrade.Normal)),
                )
        val schedule =
            AppBackgroundSchedulePolicy.nextBaReminderSchedule(
                snapshot = craftSnapshot(craft),
                nowMs = START + MINUTE,
            )
        assertEquals(START + 90L * MINUTE, assertNotNull(schedule).triggerAtMillis)
    }

    @Test
    fun `an already elapsed slot asks to fire now rather than later`() {
        val schedule =
            AppBackgroundSchedulePolicy.nextBaReminderSchedule(
                snapshot = craftSnapshot(loaded(BaCraftFunction.Fusion, 0, BaCraftGrade.Low)),
                nowMs = START + 45L * MINUTE,
            )
        val plan = assertNotNull(schedule)
        assertEquals(START + 45L * MINUTE, plan.triggerAtMillis)
        assertEquals(BackgroundAlarmPrecision.Prompt, plan.precision)
    }

    @Test
    fun `an elapsed and already announced slot schedules nothing`() {
        val craft = loaded(BaCraftFunction.Fusion, 0, BaCraftGrade.Low)
        val notified =
            BaCraftNotifiedMarkers().withMarkerAt(
                function = BaCraftFunction.Fusion,
                index = 0,
                endAtMs = START + 30L * MINUTE,
            )
        assertNull(
            AppBackgroundSchedulePolicy.nextBaReminderSchedule(
                snapshot = craftSnapshot(craft, notified),
                nowMs = START + 45L * MINUTE,
            ),
        )
    }

    @Test
    fun `no loaded slot schedules nothing`() {
        assertNull(
            AppBackgroundSchedulePolicy.nextBaReminderSchedule(
                snapshot = craftSnapshot(BaCraftState()),
                nowMs = START,
            ),
        )
    }

    @Test
    fun `a craft competes with the other BA reminders on trigger time`() {
        // Cafe visit seeds a baseline one minute out; a craft ending sooner must still win.
        val craft = loaded(BaCraftFunction.Generate, 0, BaCraftGrade.Low, startedAtMs = START)
        val schedule =
            AppBackgroundSchedulePolicy.nextBaReminderSchedule(
                snapshot =
                    craftSnapshot(craft).copy(
                        arenaRefreshNotifyEnabled = true,
                        arenaRefreshLastNotifiedSlotMs = START,
                    ),
                nowMs = START + MINUTE,
            )
        assertEquals(START + 30L * MINUTE, assertNotNull(schedule).triggerAtMillis)
    }
}
