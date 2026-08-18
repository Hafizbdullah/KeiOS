package os.kei.ui.page.main.widget.chrome

import androidx.compose.ui.unit.dp
import kotlin.test.assertEquals
import org.junit.Test

class AppPageContentWidthTest {
    /**
     * The half that must never regress: on a phone this is identically zero.
     *
     * Every widget that adds the gutter — list content padding, the top-end action overlay, the tabbed bottom
     * chrome, Home's own padding — adds it unconditionally, so if this returned anything non-zero below the
     * cap it would shift the entire phone layout at once. The widths below are the real ones: 411dp is a
     * Pixel-class portrait, 426dp is the phone AVD (1280x2856 at density 480), 440dp is the widest phone this
     * app can install on.
     */
    @Test
    fun `a phone gets no gutter at all`() {
        listOf(360.dp, 411.dp, 426.dp, 440.dp).forEach { width ->
            assertEquals(0.dp, appPageSideGutterFor(width), "width=$width")
        }
    }

    /**
     * Measured Pad AVD geometry, both orientations — 2560x1600 at density 320.
     *
     * The point of the numbers is that the *content* is 720dp in both: 800 - 2*40 and 1280 - 2*280. Rotating
     * the tablet moves the gutter, never the row.
     */
    @Test
    fun `the pad gets the same content column in both orientations`() {
        val portrait = appPageSideGutterFor(800.dp)
        val landscape = appPageSideGutterFor(1280.dp)

        assertEquals(40.dp, portrait)
        assertEquals(280.dp, landscape)
        assertEquals(AppPageContentMaxWidth, 800.dp - portrait * 2f)
        assertEquals(AppPageContentMaxWidth, 1280.dp - landscape * 2f)
    }

    /** Exactly at the cap is the boundary, and it belongs to the no-gutter side. */
    @Test
    fun `the cap itself produces no gutter`() {
        assertEquals(0.dp, appPageSideGutterFor(AppPageContentMaxWidth))
        assertEquals(0.dp, appPageSideGutterFor(AppPageContentMaxWidth - 1.dp))
        assertEquals(0.5.dp, appPageSideGutterFor(AppPageContentMaxWidth + 1.dp))
    }

    /**
     * A narrowed window is the case that would break if this read the display instead of the window.
     *
     * Split-screen on a tablet hands the app something phone-shaped. The gutter has to vanish then, or the
     * content column would be centred against a screen the app does not have and the visible half would be
     * padded off-centre.
     */
    @Test
    fun `a split-screen window falls back to no gutter`() {
        assertEquals(0.dp, appPageSideGutterFor(640.dp))
        assertEquals(0.dp, appPageSideGutterFor(400.dp))
    }
}
