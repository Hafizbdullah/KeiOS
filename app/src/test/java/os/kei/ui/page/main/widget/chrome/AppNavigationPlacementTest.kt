package os.kei.ui.page.main.widget.chrome

import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AppNavigationPlacementTest {
    /** A phone keeps the bottom bar whatever the user once picked on a tablet. */
    @Test
    fun `a compact window is always a bottom bar`() {
        listOf(360.dp, 411.dp, 426.dp, 440.dp, 599.dp).forEach { width ->
            assertEquals(
                AppNavigationPlacement.Bottom,
                appNavigationPlacementFor(width, sidebarPreferred = false),
                "width=$width",
            )
            assertEquals(
                AppNavigationPlacement.Bottom,
                appNavigationPlacementFor(width, sidebarPreferred = true),
                "width=$width, preference must not reach a phone",
            )
        }
    }

    /**
     * "Consider using a tab bar first" — so a tablet with no stated preference gets the tab bar, at the top.
     */
    @Test
    fun `a regular window defaults to the top tab bar`() {
        listOf(600.dp, 775.dp, 800.dp, 930.dp, 1280.dp).forEach { width ->
            assertEquals(
                AppNavigationPlacement.Top,
                appNavigationPlacementFor(width, sidebarPreferred = false),
                "width=$width",
            )
        }
    }

    @Test
    fun `asking for the sidebar gets it wherever it fits`() {
        listOf(775.dp, 800.dp, 930.dp, 1280.dp).forEach { width ->
            assertEquals(
                AppNavigationPlacement.Sidebar,
                appNavigationPlacementFor(width, sidebarPreferred = true),
                "width=$width",
            )
        }
    }

    /**
     * The band where the window is a tablet but too narrow to spend 280dp on navigation.
     *
     * The preference is kept, not discarded — the same call with a wider window returns Sidebar again — which
     * is what makes rotating a small tablet a shape change rather than a lost setting.
     */
    @Test
    fun `a sidebar falls back to the top bar when it would starve the content`() {
        assertEquals(660.dp, AppSidebarMinWindowWidth)
        assertEquals(AppNavigationPlacement.Top, appNavigationPlacementFor(600.dp, sidebarPreferred = true))
        assertEquals(AppNavigationPlacement.Top, appNavigationPlacementFor(659.dp, sidebarPreferred = true))
        assertEquals(AppNavigationPlacement.Sidebar, appNavigationPlacementFor(660.dp, sidebarPreferred = true))

        assertFalse(appSidebarAvailableAt(640.dp))
        assertTrue(appSidebarAvailableAt(800.dp))
    }

    /** Whatever the sidebar leaves behind is never below the floor that justified allowing it. */
    @Test
    fun `the content column always survives the sidebar`() {
        listOf(660.dp, 775.dp, 800.dp, 930.dp, 1280.dp).forEach { width ->
            val content = appNavigationContentWidthFor(width, AppNavigationPlacement.Sidebar)
            assertTrue(content >= AppPaneMinWidth, "width=$width left $content")
        }
    }

    /** A floating bar takes no width: the HIG asks for content to run underneath it, not beside it. */
    @Test
    fun `a floating bar does not narrow the content`() {
        assertEquals(1280.dp, appNavigationContentWidthFor(1280.dp, AppNavigationPlacement.Top))
        assertEquals(426.dp, appNavigationContentWidthFor(426.dp, AppNavigationPlacement.Bottom))
    }
}
