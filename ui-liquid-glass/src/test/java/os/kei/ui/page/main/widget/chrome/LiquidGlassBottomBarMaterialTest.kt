package os.kei.ui.page.main.widget.chrome

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassBottomBarMaterialTest {
    @Test
    fun lightMaterialKeepsBackdropVisible() {
        val material = liquidBottomBarMaterial(isLight = true)

        assertTrue(material.surfaceAlpha < 0.35f)
        assertTrue(material.highlightAlpha < 0.75f)
        assertEquals(16.dp, material.lensHeight)
        assertEquals(32.dp, material.lensAmount)
    }

    @Test
    fun darkMaterialUsesQuieterRefraction() {
        val material = liquidBottomBarMaterial(isLight = false)

        assertTrue(material.surfaceAlpha <= 0.20f)
        assertTrue(material.highlightAlpha < 0.55f)
        assertTrue(material.lensAmount <= 32.dp)
    }
}
