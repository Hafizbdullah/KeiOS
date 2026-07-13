package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFloatingLiquidDockMaterialTest {
    @Test
    fun lightMaterialUsesBackdropDocumentationGeometry() {
        val material = floatingLiquidDockMaterial(isDark = false)

        assertEquals(4.dp, material.blur)
        assertEquals(16.dp, material.lensHeight)
        assertEquals(32.dp, material.lensAmount)
        assertTrue(material.surfaceAlpha < 0.30f)
        assertTrue(material.highlightAlpha < 0.70f)
    }

    @Test
    fun darkMaterialKeepsChromeQuiet() {
        val material = floatingLiquidDockMaterial(isDark = true)

        assertEquals(4.dp, material.blur)
        assertTrue(material.surfaceAlpha < 0.25f)
        assertTrue(material.highlightAlpha < 0.50f)
        assertTrue(material.lensAmount <= 32.dp)
    }
}
