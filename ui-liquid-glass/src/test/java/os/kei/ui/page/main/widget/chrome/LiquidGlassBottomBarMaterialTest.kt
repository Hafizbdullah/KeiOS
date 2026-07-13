package os.kei.ui.page.main.widget.chrome

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassBottomBarMaterialTest {
    @Test
    fun lightMaterialPreservesReferenceRefraction() {
        val material = liquidBottomBarMaterial(isLight = true)

        assertEquals(0.40f, material.surfaceAlpha)
        assertEquals(1f, material.highlightAlpha)
        assertEquals(24.dp, material.lensHeight)
        assertEquals(24.dp, material.lensAmount)
    }

    @Test
    fun darkMaterialUsesQuieterRefraction() {
        val material = liquidBottomBarMaterial(isLight = false)

        assertTrue(material.surfaceAlpha <= 0.20f)
        assertTrue(material.highlightAlpha < 0.55f)
        assertTrue(material.lensAmount <= 32.dp)
    }

    @Test
    fun lightSelectionIndicatorKeepsAVisibleNeutralFilm() {
        val indicator = liquidBottomBarSelectionIndicatorColor(isLight = true)

        assertTrue(indicator.alpha in 0.09f..0.11f)
        assertEquals(Color.Black.red, indicator.red)
        assertEquals(Color.Black.green, indicator.green)
        assertEquals(Color.Black.blue, indicator.blue)
    }
}
