package os.kei.ui.page.main.ba.support

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val MINUTE = 60L * 1000L
private const val HOUR = 60L * MINUTE

/** Any non-zero anchor: `startedAtMs == 0L` is the idle sentinel, so it cannot double as a start. */
private const val START = 1_700_000_000_000L

class BaCraftModelsTest {
    @Test
    fun `grade ladder matches the game`() {
        assertEquals(30L * MINUTE, BaCraftGrade.Low.durationMs)
        assertEquals(90L * MINUTE, BaCraftGrade.Normal.durationMs)
        assertEquals(3L * HOUR, BaCraftGrade.High.durationMs)
        assertEquals(6L * HOUR, BaCraftGrade.Highest.durationMs)
    }

    @Test
    fun `generate sums the grade of every opened node`() {
        val slot =
            BaCraftSlot(
                startedAtMs = 1_000L,
                grades = listOf(BaCraftGrade.High, BaCraftGrade.High, BaCraftGrade.Highest),
            )
        assertEquals(12L * HOUR, slot.computedDurationMs())
        assertEquals(1_000L + 12L * HOUR, slot.endAtMs())
    }

    @Test
    fun `three highest nodes are the longest reachable generate craft`() {
        val slot = BaCraftSlot(startedAtMs = 1L, grades = List(3) { BaCraftGrade.Highest })
        assertEquals(18L * HOUR, slot.computedDurationMs())
    }

    @Test
    fun `fusion multiplies one grade by the quantity`() {
        val slot = BaCraftSlot(startedAtMs = 1L, grades = List(5) { BaCraftGrade.Highest })
        // Five superlative copies is the documented worst case: 30 hours.
        assertEquals(30L * HOUR, slot.computedDurationMs())
    }

    @Test
    fun `generate caps at three entries and fusion at five`() {
        val overfilled = BaCraftSlot(grades = List(9) { BaCraftGrade.Low })
        assertEquals(3, overfilled.normalized(BaCraftFunction.Generate).grades.size)
        assertEquals(5, overfilled.normalized(BaCraftFunction.Fusion).grades.size)
    }

    @Test
    fun `fusion collapses a mixed list onto its first grade`() {
        val mixed =
            BaCraftSlot(
                grades = listOf(BaCraftGrade.High, BaCraftGrade.Low, BaCraftGrade.Highest),
            )
        val normalized = mixed.normalized(BaCraftFunction.Fusion)
        assertEquals(List(3) { BaCraftGrade.High }, normalized.grades)
        assertEquals(9L * HOUR, normalized.computedDurationMs())
    }

    @Test
    fun `generate keeps a mixed list intact`() {
        val mixed = listOf(BaCraftGrade.High, BaCraftGrade.Low, BaCraftGrade.Highest)
        assertEquals(mixed, BaCraftSlot(grades = mixed).normalized(BaCraftFunction.Generate).grades)
    }

    @Test
    fun `custom duration overrides the computed sum`() {
        val slot =
            BaCraftSlot(
                startedAtMs = 1_000L,
                grades = listOf(BaCraftGrade.Low),
                customDurationMs = 7L * HOUR,
            )
        assertEquals(30L * MINUTE, slot.computedDurationMs())
        assertEquals(7L * HOUR, slot.effectiveDurationMs())
        assertEquals(1_000L + 7L * HOUR, slot.endAtMs())
    }

    @Test
    fun `custom duration is bounded and a corrupt value cannot schedule months out`() {
        val slot = BaCraftSlot(customDurationMs = 400L * 24L * HOUR)
        assertEquals(BA_CRAFT_MAX_DURATION_MS, slot.normalized(BaCraftFunction.Fusion).customDurationMs)
    }

    @Test
    fun `an idle slot has no duration and no end`() {
        val noStart = BaCraftSlot(grades = listOf(BaCraftGrade.Low))
        assertFalse(noStart.isActive())
        assertEquals(0L, noStart.endAtMs())
        assertEquals(0L, noStart.remainingMs(nowMs = 5L))

        val noGrades = BaCraftSlot(startedAtMs = 1_000L)
        assertFalse(noGrades.isActive())
        assertEquals(0L, noGrades.endAtMs())
    }

    @Test
    fun `remaining clamps at zero once elapsed`() {
        val slot = BaCraftSlot(startedAtMs = START, grades = listOf(BaCraftGrade.Low))
        assertEquals(10L * MINUTE, slot.remainingMs(nowMs = START + 20L * MINUTE))
        assertEquals(0L, slot.remainingMs(nowMs = START + 31L * MINUTE))
        assertTrue(slot.isComplete(nowMs = START + 30L * MINUTE))
        assertFalse(slot.isComplete(nowMs = START + 30L * MINUTE - 1L))
    }

    @Test
    fun `state normalizes to exactly three slots per function`() {
        val state = BaCraftState(generate = listOf(BaCraftSlot()), fusion = emptyList()).normalized()
        assertEquals(BA_CRAFT_SLOT_COUNT, state.generate.size)
        assertEquals(BA_CRAFT_SLOT_COUNT, state.fusion.size)
        assertFalse(state.hasActiveSlot())
    }

    @Test
    fun `next completion is the earliest future end across both functions`() {
        val state =
            BaCraftState()
                .withSlotAt(
                    BaCraftFunction.Generate,
                    0,
                    BaCraftSlot(startedAtMs = START, grades = listOf(BaCraftGrade.Highest)),
                )
                .withSlotAt(
                    BaCraftFunction.Fusion,
                    2,
                    BaCraftSlot(startedAtMs = START, grades = listOf(BaCraftGrade.Normal)),
                )
        // Fusion slot 2 ends at 1:30, generate slot 0 at 6:00.
        assertEquals(START + 90L * MINUTE, state.nextCompletionAtMs(nowMs = START + MINUTE))
        assertTrue(state.hasActiveSlot())
    }

    @Test
    fun `an elapsed slot is a completion rather than a future alarm`() {
        val state =
            BaCraftState().withSlotAt(
                BaCraftFunction.Fusion,
                1,
                BaCraftSlot(startedAtMs = START, grades = listOf(BaCraftGrade.Low)),
            )
        val nowMs = START + 45L * MINUTE
        assertNull(state.nextCompletionAtMs(nowMs))
        val completions = state.completions(nowMs)
        assertEquals(1, completions.size)
        assertEquals(BaCraftFunction.Fusion, completions.single().function)
        assertEquals(1, completions.single().index)
        assertEquals(START + 30L * MINUTE, completions.single().endAtMs)
    }

    @Test
    fun `writing a slot out of range is ignored`() {
        val state = BaCraftState().normalized()
        val slot = BaCraftSlot(startedAtMs = 1L, grades = listOf(BaCraftGrade.Low))
        assertEquals(state, state.withSlotAt(BaCraftFunction.Generate, 3, slot))
        assertEquals(state, state.withSlotAt(BaCraftFunction.Generate, -1, slot))
    }

    @Test
    fun `writing a slot normalizes it and fills the rest`() {
        val state =
            BaCraftState().withSlotAt(
                BaCraftFunction.Fusion,
                0,
                BaCraftSlot(
                    startedAtMs = -5L,
                    grades = List(9) { BaCraftGrade.High },
                    label = "  a very long crafting note that overflows  ",
                ),
            )
        val slot = state.slotAt(BaCraftFunction.Fusion, 0)
        assertEquals(0L, slot.startedAtMs)
        assertEquals(5, slot.grades.size)
        assertEquals("a very long crafting not", slot.label)
        assertEquals(BA_CRAFT_SLOT_COUNT, state.fusion.size)
    }

    @Test
    fun `label is trimmed to twenty four characters`() {
        val slot = BaCraftSlot(label = "x".repeat(40)).normalized(BaCraftFunction.Generate)
        assertEquals(24, slot.label.length)
    }
}
