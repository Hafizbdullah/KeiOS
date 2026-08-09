package os.kei.ui.page.main.widget.glass

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

/**
 * The pile geometry.
 *
 * Two properties here are the point of the rewrite rather than tuning, and both are stated as
 * assertions rather than left to a screenshot.
 *
 * **Depth darkens before anything turns transparent.** `dim` is the depth channel and `fade` is only
 * a retirement tail. The previous implementation had no `dim` at all and drove `alpha` from 1 to 0
 * across the whole pile, which is why receding cards read as merely translucent — and over a light
 * page they got *lighter* as they receded, the opposite of going behind something.
 *
 * **A tall card and a short row pile at comparable rates.** Depth used to run over
 * `stackLine + height` with no bound, so a 600px card needed 626px of overshoot to recede while a
 * 60px row needed 86px — a sevenfold spread, and the reason the effect looked like a different
 * feature on the OS page than on the history pages.
 */
class AppEdgeStackedCardsTest {
    private val stackLine = 300f
    private val height = 400f
    private val riseTotal = 54f
    private val step = 150f

    private fun transformAt(
        itemTop: Float,
        itemHeightPx: Float = height,
        stepPx: Float = step,
    ): AppEdgeStackTransform =
        computeAppEdgeStackTransform(
            itemTopInContainer = itemTop,
            itemHeightPx = itemHeightPx,
            stackLinePx = stackLine,
            riseTotalPx = riseTotal,
            stepPx = stepPx,
        )

    @Test
    fun `cards resting at or below the stack line render untouched`() {
        assertEquals(AppEdgeStackTransform.Identity, transformAt(stackLine))
        assertEquals(AppEdgeStackTransform.Identity, transformAt(stackLine + 250f))
    }

    @Test
    fun `zero height items never transform`() {
        assertEquals(AppEdgeStackTransform.Identity, transformAt(0f, itemHeightPx = 0f))
    }

    @Test
    fun `a card crossing the line pins near it instead of scrolling away`() {
        val overshoot = 80f
        val transform = transformAt(stackLine - overshoot)

        // Pinned: the travel is handed back, less the rise the card has earned by sinking.
        assertTrue(transform.translationY > overshoot - riseTotal)
        assertTrue(transform.translationY <= overshoot)
        assertTrue(transform.scale < 1f)
    }

    @Test
    fun `depth is expressed as dimming long before anything fades`() {
        // The regression this rewrite exists to prevent. At the depth where a card is already visibly
        // receded, it must still be fully opaque — otherwise the pile is just transparency again.
        val shallow = transformAt(stackLine - 60f)
        val middle = transformAt(stackLine - 180f)

        assertTrue(shallow.dim > 0f, "a card past the line is already dimmed")
        assertTrue(middle.dim > shallow.dim, "dimming deepens with depth")
        assertEquals(1f, shallow.fade)
        assertEquals(1f, middle.fade)
    }

    @Test
    fun `depth grows monotonically and bottoms out at the scale floor`() {
        val shallow = transformAt(stackLine - 50f)
        val deep = transformAt(stackLine - 400f)

        assertTrue(deep.scale < shallow.scale)
        assertTrue(deep.scale >= APP_EDGE_STACK_MIN_SCALE)
        assertTrue(deep.dim > shallow.dim)
        // Deeper cards sit visually higher: more of the rise has been applied.
        val shallowVisualTop = stackLine - 50f + shallow.translationY
        val deepVisualTop = stackLine - 400f + deep.translationY
        assertTrue(deepVisualTop < shallowVisualTop)
    }

    @Test
    fun `dim and scale saturate rather than running away`() {
        val atFullDepth = transformAt(stackLine - 4000f)

        assertEquals(1f, atFullDepth.dim)
        assertEquals(APP_EDGE_STACK_MIN_SCALE, atFullDepth.scale)
        assertEquals(0f, atFullDepth.fade)
    }

    @Test
    fun `retirement completes before the lazy layout disposes the card`() {
        // Disposal happens when the item's layout bottom crosses the viewport top: itemTop == -height.
        val disposalOvershoot = stackLine + height
        assertEquals(0f, transformAt(-height).fade)
        // And strictly before it, so removal never pops.
        val retireOvershoot = disposalOvershoot * APP_EDGE_STACK_RETIRE_MARGIN
        assertTrue(retireOvershoot < disposalOvershoot)
        assertEquals(0f, transformAt(stackLine - retireOvershoot).fade)
    }

    @Test
    fun `the pile is bounded to a few levels rather than the whole card`() {
        // A short row must not have to travel its own height plus the stack line to recede: with the
        // step bounded, three levels is the whole pile.
        val shortStep = 64f
        val pile = APP_EDGE_STACK_LEVELS * shortStep
        val atPileEnd = transformAt(stackLine - pile, itemHeightPx = 900f, stepPx = shortStep)

        assertEquals(1f, atPileEnd.dim)
        assertEquals(APP_EDGE_STACK_MIN_SCALE, atPileEnd.scale)
    }

    @Test
    fun `tall and short cards recede at comparable rates`() {
        // The clamped step is what bounds this. Unclamped height gave a sevenfold spread; measured at
        // the same overshoot, the two extremes of the step band must stay within a small factor.
        val overshoot = 120f
        val shortRow = transformAt(stackLine - overshoot, itemHeightPx = 64f, stepPx = 64f)
        val tallCard = transformAt(stackLine - overshoot, itemHeightPx = 900f, stepPx = 168f)

        assertTrue(shortRow.dim > tallCard.dim, "a short row is deeper into the pile at equal travel")
        assertTrue(
            shortRow.dim < tallCard.dim * 3f,
            "but not by the runaway factor the unbounded rate produced: " +
                "${shortRow.dim} vs ${tallCard.dim}",
        )
    }

    @Test
    fun `the scrim is heavier in dark mode than in light`() {
        val card = AppEdgeStackCard().apply { apply(transformAt(stackLine - 4000f)) }

        assertEquals(APP_EDGE_STACK_DIM_DARK, appEdgeStackDimAlpha(card, isDark = true))
        assertEquals(APP_EDGE_STACK_DIM_LIGHT, appEdgeStackDimAlpha(card, isDark = false))
        assertTrue(APP_EDGE_STACK_DIM_LIGHT < APP_EDGE_STACK_DIM_DARK)
    }

    @Test
    fun `a resting card draws no scrim at all`() {
        val card = AppEdgeStackCard()

        assertEquals(0f, appEdgeStackDimAlpha(card, isDark = true))
        assertEquals(0f, appEdgeStackDimAlpha(card, isDark = false))
    }
}
