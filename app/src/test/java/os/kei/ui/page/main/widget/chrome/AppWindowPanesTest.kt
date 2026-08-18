package os.kei.ui.page.main.widget.chrome

import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import org.junit.Test

class AppWindowPanesTest {
    /**
     * The devices the threshold was derived to land correctly on.
     *
     * These are the real widths, and each row is a case that was named as wanting one answer or the other: an
     * 8" tablet stays on one pane, a fold's inner screen and an 11"-class tablet get two. A borrowed 600dp
     * breakpoint fails the first row and a borrowed 840dp one fails the fold in portrait, which is why the
     * threshold is derived from [AppPaneMinWidth] instead.
     */
    @Test
    fun `each real device lands on the intended side`() {
        val cases =
            listOf(
                360.dp to AppPaneMode.Single, // narrowest phone, and a fold's outer screen
                426.dp to AppPaneMode.Single, // the phone AVD
                600.dp to AppPaneMode.Single, // 8" tablet in portrait
                720.dp to AppPaneMode.Single, // 8" tablet, the roomy end
                775.dp to AppPaneMode.Dual, // fold inner screen, portrait
                800.dp to AppPaneMode.Dual, // Pixel Tablet, portrait
                930.dp to AppPaneMode.Dual, // fold inner screen, landscape
                1280.dp to AppPaneMode.Dual, // Pixel Tablet, landscape
            )
        cases.forEach { (width, expected) ->
            assertEquals(expected, appPaneModeFor(width), "width=$width")
        }
    }

    /** No pane may ever be born narrower than the geometry the app is drawn for. */
    @Test
    fun `a pane is never below the minimum that justified the split`() {
        listOf(760.dp, 775.dp, 800.dp, 930.dp, 1280.dp, 2000.dp).forEach { width ->
            val pane = appPaneWidthFor(width)
            assert(pane >= AppPaneMinWidth) { "width=$width produced a $pane pane" }
        }
    }

    @Test
    fun `the threshold is exactly two minimum panes, and the boundary splits`() {
        assertEquals(760.dp, AppDualPaneMinWidth)
        assertEquals(AppPaneMode.Dual, appPaneModeFor(AppDualPaneMinWidth))
        assertEquals(AppPaneMode.Single, appPaneModeFor(AppDualPaneMinWidth - 1.dp))
        assertEquals(AppPaneMinWidth, appPaneWidthFor(AppDualPaneMinWidth))
    }

    /** Single pane means the window is the pane — not half of it. */
    @Test
    fun `a single-pane window is not split`() {
        assertEquals(426.dp, appPaneWidthFor(426.dp))
        assertEquals(600.dp, appPaneWidthFor(600.dp))
    }

    /**
     * The two layers compose rather than fight: the split happens first, the content cap applies inside a pane.
     *
     * On the Pad AVD in landscape each pane is 640dp, under [AppPageContentMaxWidth], so a pane takes no gutter
     * at all — the split has already done the job the gutter existed to do. The gutter only comes back on a
     * panel wide enough to give a single pane more than the cap.
     */
    @Test
    fun `the content cap applies inside a pane, not across the window`() {
        val padLandscapePane = appPaneWidthFor(1280.dp)
        assertEquals(640.dp, padLandscapePane)
        assertEquals(0.dp, appPageSideGutterFor(padLandscapePane))

        val hugePane = appPaneWidthFor(2000.dp)
        assertEquals(1000.dp, hugePane)
        assertEquals(140.dp, appPageSideGutterFor(hugePane))

        // And the single-pane Pad in portrait keeps round 1's behaviour untouched.
        assertEquals(40.dp, appPageSideGutterFor(appPaneWidthFor(800.dp) * 2f))
    }
}
