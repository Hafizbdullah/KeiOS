package os.kei.ui.page.main.widget.chrome

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidActionBarMaterialTest {
    @Test
    fun lightMaterialTracksAiryBackdropGeometry() {
        val material = liquidActionBarMaterial(isLight = true)

        assertEquals(4.dp, material.blur)
        assertEquals(16.dp, material.lensHeight)
        assertEquals(32.dp, material.lensAmount)
        assertTrue(material.surfaceAlpha <= 0.30f)
        assertTrue(material.highlightAlpha < 0.70f)
    }

    @Test
    fun darkMaterialKeepsChromeQuiet() {
        val material = liquidActionBarMaterial(isLight = false)

        assertEquals(4.dp, material.blur)
        assertTrue(material.surfaceAlpha <= 0.22f)
        assertTrue(material.highlightAlpha < 0.50f)
        assertTrue(material.lensAmount <= 32.dp)
    }

    @Test
    fun lightSelectionIndicatorUsesAWhiteTintedFilm() {
        val indicator =
            liquidChromeSelectionIndicatorColor(
                isLight = true,
                accentColor = Color.Blue,
            )

        assertTrue(indicator.alpha in 0.25f..0.27f)
        assertTrue(indicator.red > 0.80f)
        assertTrue(indicator.green > 0.80f)
        assertTrue(indicator.blue >= indicator.red)
    }

    @Test
    fun darkSelectionIndicatorStaysSubtle() {
        val indicator =
            liquidChromeSelectionIndicatorColor(
                isLight = false,
                accentColor = Color.Blue,
            )

        assertTrue(indicator.alpha <= 0.12f)
    }

    @Test
    fun threeActionsKeepTheirCompactProductionWidth() {
        assertEquals(
            160.dp,
            liquidActionBarWidth(
                itemCount = 3,
                minimumWidth = 160.dp,
            ),
        )
    }

    @Test
    fun fourActionsExpandToPreserveTouchTargets() {
        assertEquals(
            200.dp,
            liquidActionBarWidth(
                itemCount = 4,
                minimumWidth = 160.dp,
            ),
        )
    }
}
