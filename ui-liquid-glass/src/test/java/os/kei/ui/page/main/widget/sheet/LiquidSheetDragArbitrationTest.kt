package os.kei.ui.page.main.widget.sheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sheet's vertical drag arbitration.
 *
 * Every case here is a state the content scroll was measured dying in on the Android 17 AVD, traced
 * out of the real nested-scroll callbacks. The instrumented run that produced them looked like this,
 * for a sheet whose maximum height is 2700px:
 *
 * ```
 * preScroll DIVERT delta=-19.2  hidden=0.0 canScrollUp=false h=2157 max=2700 consumed=-19.2
 * preScroll DIVERT delta=-68.8  hidden=0.0 canScrollUp=false h=2226 max=2700 consumed=-68.8
 * preScroll DIVERT delta=-243.2 hidden=0.0 canScrollUp=false h=2700 max=2700 consumed=-198.8
 * preFling  EAT   v=0.0 sheetConsumed=true hidden=0.0
 * ```
 *
 * Two separate defects in four lines: every upward delta consumed whole by sheet growth, and then a
 * fling eaten while `hidden` was exactly zero — so the sheet claimed a fling it had nothing to do
 * with.
 */
class LiquidSheetDragArbitrationTest {
    private val maxHeight = 2700f
    private val minHeight = 900f

    @Test
    fun contentOwnsAnUpwardDragOnceTheSheetIsAtItsMaximum() {
        assertEquals(
            LiquidSheetDragOwner.Content,
            liquidSheetUpwardDragOwner(
                hidden = 0f,
                heightPx = maxHeight,
                maxHeightPx = maxHeight,
                contentCanScrollUp = false,
                contentOverflows = true,
            ),
        )
    }

    @Test
    fun aSubPixelShortfallDoesNotHandTheDragBackToTheSheet() {
        // The measured trap. A gesture that simply ran out of finger left the sheet at 2668 against a
        // 2700 maximum, and nothing ever snapped it, so `heightPx < maxHeightPx` stayed true and the
        // sheet claimed the first part of every later upward drag. The height snap is what closes the
        // 32px case; the epsilon is what closes the fractional one that a spring leaves behind.
        assertFalse(liquidSheetCanGrow(maxHeight - 0.4f, maxHeight))
        assertEquals(
            LiquidSheetDragOwner.Content,
            liquidSheetUpwardDragOwner(
                hidden = 0f,
                heightPx = maxHeight - 0.4f,
                maxHeightPx = maxHeight,
                contentCanScrollUp = false,
                contentOverflows = true,
            ),
        )
    }

    @Test
    fun aShortSheetDoesNotInflateWhenGrowingWouldRevealNothing() {
        // Measured on device twice: the sheet grew to full height on the first upward drag of a sheet
        // whose content already fitted, leaving 500-700px of empty glass below it. Growing is only
        // worth taking the drag for when there is content to reveal.
        assertEquals(
            LiquidSheetDragOwner.Content,
            liquidSheetUpwardDragOwner(
                hidden = 0f,
                heightPx = minHeight,
                maxHeightPx = maxHeight,
                contentCanScrollUp = false,
                contentOverflows = false,
            ),
        )
    }

    @Test
    fun expandToScrollStillWinsWhenThereIsContentToReveal() {
        // Apple's behaviour for a sheet below its largest detent, and the reason this branch exists at
        // all. Removing it would be the wrong fix.
        assertEquals(
            LiquidSheetDragOwner.Sheet,
            liquidSheetUpwardDragOwner(
                hidden = 0f,
                heightPx = minHeight,
                maxHeightPx = maxHeight,
                contentCanScrollUp = false,
                contentOverflows = true,
            ),
        )
    }

    @Test
    fun contentThatHasScrolledOffItsTopKeepsTheGesture() {
        // The stale-signal case. `contentCanScrollUp` used to arrive through a
        // snapshotFlow -> reporter -> state-write round trip that published *false* whenever the host
        // sheet recomposed, and never corrected itself. A false value here is what let the sheet grab
        // a drag that belonged to content sitting mid-scroll.
        assertEquals(
            LiquidSheetDragOwner.Content,
            liquidSheetUpwardDragOwner(
                hidden = 0f,
                heightPx = minHeight,
                maxHeightPx = maxHeight,
                contentCanScrollUp = true,
                contentOverflows = true,
            ),
        )
    }

    @Test
    fun aPartlyDismissedSheetIsPulledBackBeforeAnythingElse() {
        assertEquals(
            LiquidSheetDragOwner.Sheet,
            liquidSheetUpwardDragOwner(
                hidden = 0.2f,
                heightPx = maxHeight,
                maxHeightPx = maxHeight,
                contentCanScrollUp = true,
                contentOverflows = true,
            ),
        )
    }

    @Test
    fun anInvisibleDismissResidueDoesNotArmTheSheet() {
        // `settleSpec` is underdamped, so it overshoots to negative `hidden`, and `takeOverMotion`
        // stops an interrupted animation from writing its final exact zero. Either way the sheet looks
        // perfectly at rest — `liquidSheetOffsetPx` clamps at zero — while a bare `hidden != 0f` test
        // stays armed indefinitely and keeps diverting the content's drags.
        assertFalse(liquidSheetIsOffRest(hidden = -0.004f, heightPx = maxHeight))
        assertFalse(liquidSheetIsOffRest(hidden = 0.0002f, heightPx = maxHeight))
        assertTrue(liquidSheetIsOffRest(hidden = 0.02f, heightPx = maxHeight))
        assertEquals(
            LiquidSheetDragOwner.Content,
            liquidSheetUpwardDragOwner(
                hidden = -0.004f,
                heightPx = maxHeight,
                maxHeightPx = maxHeight,
                contentCanScrollUp = true,
                contentOverflows = true,
            ),
        )
    }

    @Test
    fun theContentKeepsItsFlingWhenTheSheetIsAtRest() {
        // The `preFling EAT ... hidden=0.0` line from the trace. The flag it turns on used to latch for
        // the whole gesture, so *drag up -> sheet grows -> content scrolls -> flick* ended with the
        // sheet swallowing the entire fling. The content stopped dead the instant the finger lifted, on
        // every scroll.
        assertFalse(
            liquidSheetShouldClaimFling(
                sheetOwnsGesture = false,
                hidden = 0f,
                heightPx = maxHeight,
            ),
        )
    }

    @Test
    fun theSheetKeepsTheFlingItWasActuallyDriving() {
        assertTrue(
            liquidSheetShouldClaimFling(
                sheetOwnsGesture = true,
                hidden = 0f,
                heightPx = maxHeight,
            ),
        )
        assertTrue(
            liquidSheetShouldClaimFling(
                sheetOwnsGesture = false,
                hidden = 0.3f,
                heightPx = maxHeight,
            ),
        )
    }

    @Test
    fun aSheetLeftBelowItsMaximumStillHandsTheRemainderOfTheDragToTheContent() {
        // Why a resize is allowed to rest at an arbitrary height, rather than being snapped to a
        // detent: the grabber is a continuous control by design and three tests in
        // `LiquidGlassBottomSheetTest` pin that. The cost is bounded — the sheet takes only the gap it
        // can actually use, and `liquidSheetResolveDrag` reports exactly that, so the rest reaches the
        // content inside the same event.
        val result =
            liquidSheetResolveDrag(
                deltaPx = -243f,
                heightPx = maxHeight - 32f,
                hidden = 0f,
                minVisibleHeightPx = minHeight,
                maxVisibleHeightPx = maxHeight,
                resistance = 1f,
            )
        assertEquals(maxHeight, result.heightPx)
        assertEquals(-32f, result.consumedPx)
    }
}
