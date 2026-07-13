package os.kei.ui.page.main.widget.sheet

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetCardOpticsTest {
    @Test
    fun liquidStaticCardsRestoreAiryTagOptics() {
        val optics =
            sheetCardOptics(
                visualMode = SheetVisualMode.Liquid,
                interactive = false,
            )

        assertTrue(optics.depthEffect)
        assertEquals(0.82f, optics.highlightAlpha)
        assertEquals(1.dp, optics.borderWidth)
    }

    @Test
    fun liquidInteractiveCardsUseFullHighlight() {
        val optics =
            sheetCardOptics(
                visualMode = SheetVisualMode.Liquid,
                interactive = true,
            )

        assertTrue(optics.depthEffect)
        assertEquals(1f, optics.highlightAlpha)
        assertEquals(1.dp, optics.borderWidth)
    }

    @Test
    fun miuixCardsKeepStandardOpticsAndBorder() {
        val optics =
            sheetCardOptics(
                visualMode = SheetVisualMode.Miuix,
                interactive = true,
            )

        assertFalse(optics.depthEffect)
        assertNull(optics.highlightAlpha)
        assertEquals(1.dp, optics.borderWidth)
    }
}
