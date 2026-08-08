package os.kei.ui.page.main.widget.sheet

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Miuix variant is gone along with the preference that selected it, so the only thing left to
 * vary is whether the card is interactive.
 */
class SheetCardOpticsTest {
    @Test
    fun staticCardsRestoreAiryTagOptics() {
        val optics = sheetCardOptics(interactive = false)

        assertTrue(optics.depthEffect)
        assertEquals(0.82f, optics.highlightAlpha)
        assertEquals(1.dp, optics.borderWidth)
    }

    @Test
    fun interactiveCardsUseFullHighlight() {
        val optics = sheetCardOptics(interactive = true)

        assertTrue(optics.depthEffect)
        assertEquals(1f, optics.highlightAlpha)
        assertEquals(1.dp, optics.borderWidth)
    }
}
