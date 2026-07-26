package os.kei.ui.page.main.widget.glass

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class AppEdgeStackedCardsTest {
    private val stackLine = 300f
    private val height = 400f
    private val tuckRise = 54f

    private fun transformAt(itemTop: Float): AppEdgeStackTransform =
        computeAppEdgeStackTransform(
            itemTopInContainer = itemTop,
            itemHeightPx = height,
            stackLinePx = stackLine,
            tuckRisePx = tuckRise,
        )

    @Test
    fun `cards resting at or below the stack line render untouched`() {
        assertEquals(AppEdgeStackTransform.Identity, transformAt(stackLine))
        assertEquals(AppEdgeStackTransform.Identity, transformAt(stackLine + 250f))
    }

    @Test
    fun `a card crossing the line pins near it instead of scrolling away`() {
        val overshoot = 80f
        val transform = transformAt(stackLine - overshoot)
        // Pinned: translation returns most of the overshoot, minus the tuck rise.
        assertTrue(transform.translationY > overshoot - tuckRise)
        assertTrue(transform.translationY < overshoot)
        assertTrue(transform.scale < 1f)
        assertEquals(1f, transform.alpha)
    }

    @Test
    fun `depth grows monotonically as the card is scrolled further past the line`() {
        val shallow = transformAt(stackLine - 50f)
        val deep = transformAt(stackLine - 400f)
        assertTrue(deep.scale < shallow.scale)
        assertTrue(deep.scale >= APP_EDGE_STACK_MIN_SCALE)
        // Deeper cards sit visually higher (larger share of overshoot returned as rise).
        val shallowVisualTop = stackLine - 50f + shallow.translationY
        val deepVisualTop = stackLine - 400f + deep.translationY
        assertTrue(deepVisualTop < shallowVisualTop)
    }

    @Test
    fun `alpha reaches zero before the lazy layout disposes the item`() {
        // Disposal happens when layout bottom crosses the viewport top: itemTop == -height.
        val disposalTop = -height
        assertEquals(0f, transformAt(disposalTop).alpha)
        // The fade must already be complete slightly before disposal so removal never pops.
        val fadeEndOvershoot = (stackLine + height) / 2f * APP_EDGE_STACK_FADE_END
        assertEquals(0f, transformAt(stackLine - fadeEndOvershoot).alpha)
        assertTrue(fadeEndOvershoot < stackLine + height)
    }

    @Test
    fun `zero height items never transform`() {
        val transform = computeAppEdgeStackTransform(
            itemTopInContainer = 0f,
            itemHeightPx = 0f,
            stackLinePx = stackLine,
            tuckRisePx = tuckRise,
        )
        assertEquals(AppEdgeStackTransform.Identity, transform)
    }
}
