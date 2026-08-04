package os.kei.ui.page.main.host.pager

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainLoadedPagerStateTest {
    @Test
    fun `far tab jump renders the outgoing and target pages as one visual pair`() {
        assertTrue(shouldUseLoadedPagerVisualPair(startPage = 0, targetPage = 4))
        assertFalse(shouldUseLoadedPagerVisualPair(startPage = 3, targetPage = 4))
        assertTrue(
            isLoadedPagerVisualPairPage(
                pageIndex = 0,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
        assertTrue(
            isLoadedPagerVisualPairPage(
                pageIndex = 4,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
        assertFalse(
            isLoadedPagerVisualPairPage(
                pageIndex = 2,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )

        assertEquals(
            0f,
            resolveLoadedPagerVisualRelativePosition(
                pageIndex = 0,
                pagePosition = 0f,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
        assertEquals(
            0.08f,
            resolveLoadedPagerVisualRelativePosition(
                pageIndex = 4,
                pagePosition = 0f,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
        assertEquals(
            2f,
            resolveLoadedPagerVisualRelativePosition(
                pageIndex = 3,
                pagePosition = 0f,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
        assertEquals(
            -0.08f,
            resolveLoadedPagerVisualRelativePosition(
                pageIndex = 0,
                pagePosition = 2f,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
        assertEquals(
            0.08f,
            resolveLoadedPagerVisualRelativePosition(
                pageIndex = 4,
                pagePosition = 2f,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
    }

    @Test
    fun `reverse far tab jump keeps the outgoing page moving right`() {
        assertEquals(
            0.08f,
            resolveLoadedPagerVisualRelativePosition(
                pageIndex = 4,
                pagePosition = 2f,
                visualFromPage = 4,
                visualTargetPage = 0,
            ),
        )
        assertEquals(
            -0.08f,
            resolveLoadedPagerVisualRelativePosition(
                pageIndex = 0,
                pagePosition = 2f,
                visualFromPage = 4,
                visualTargetPage = 0,
            ),
        )
    }

    @Test
    fun `far tab jump draws one fading page on each side of the handoff`() {
        assertTrue(
            shouldDrawLoadedPagerVisualPairPage(
                pageIndex = 0,
                pagePosition = 1f,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
        assertFalse(
            shouldDrawLoadedPagerVisualPairPage(
                pageIndex = 4,
                pagePosition = 1f,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
        assertFalse(
            shouldDrawLoadedPagerVisualPairPage(
                pageIndex = 0,
                pagePosition = 2f,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
        assertTrue(
            shouldDrawLoadedPagerVisualPairPage(
                pageIndex = 4,
                pagePosition = 2f,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )

        assertEquals(
            0.43f,
            resolveLoadedPagerVisualPairVeilAlpha(
                pageIndex = 0,
                pagePosition = 1f,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
        assertEquals(
            0.86f,
            resolveLoadedPagerVisualPairVeilAlpha(
                pageIndex = 4,
                pagePosition = 2f,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
        assertEquals(
            0.43f,
            resolveLoadedPagerVisualPairVeilAlpha(
                pageIndex = 4,
                pagePosition = 3f,
                visualFromPage = 0,
                visualTargetPage = 4,
            ),
        )
    }

    @Test
    fun `gesture settle preserves velocity toward its target`() {
        assertEquals(
            2f,
            resolveLoadedPagerSettleInitialVelocity(
                start = 0.35f,
                target = 1f,
                gestureVelocityPagesPerSecond = 2f,
            ),
        )
        assertEquals(
            -2f,
            resolveLoadedPagerSettleInitialVelocity(
                start = 0.65f,
                target = 0f,
                gestureVelocityPagesPerSecond = -2f,
            ),
        )
    }

    @Test
    fun `gesture settle discards velocity moving away from its target`() {
        assertEquals(
            0f,
            resolveLoadedPagerSettleInitialVelocity(
                start = 0.35f,
                target = 1f,
                gestureVelocityPagesPerSecond = -2f,
            ),
        )
        assertEquals(
            0f,
            resolveLoadedPagerSettleInitialVelocity(
                start = 0.65f,
                target = 0f,
                gestureVelocityPagesPerSecond = 2f,
            ),
        )
    }

    @Test
    fun `gesture settle limits high velocity near its target`() {
        val resolvedVelocity =
            resolveLoadedPagerSettleInitialVelocity(
                start = 0.97f,
                target = 1f,
                gestureVelocityPagesPerSecond = 20f,
            )

        assertTrue(resolvedVelocity > 0f)
        assertTrue(resolvedVelocity < 2f)
    }

    @Test
    fun `initial page resolver keeps saved page identity after page list shrinks`() {
        val resolved = resolveMainLoadedPagerInitialPage(
            pageKeys = listOf("Home", "GitHub", "Ba"),
            initialPage = 3,
            savedPageKey = "Ba"
        )

        assertEquals(2, resolved)
    }

    @Test
    fun `initial page resolver falls back to requested page when saved key is missing`() {
        val resolved = resolveMainLoadedPagerInitialPage(
            pageKeys = listOf("Home", "GitHub"),
            initialPage = 1,
            savedPageKey = "Ba"
        )

        assertEquals(1, resolved)
    }

    @Test
    fun `initial page resolver handles empty page list`() {
        val resolved = resolveMainLoadedPagerInitialPage(
            pageKeys = emptyList(),
            initialPage = 4,
            savedPageKey = "GitHub"
        )

        assertEquals(0, resolved)
    }
}
