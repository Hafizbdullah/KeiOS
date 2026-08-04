package os.kei.ui.page.main.host.pager

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainLoadedPagerStateTest {
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
