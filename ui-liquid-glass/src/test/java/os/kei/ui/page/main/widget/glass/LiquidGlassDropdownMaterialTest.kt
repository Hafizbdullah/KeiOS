package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassDropdownMaterialTest {
    @Test
    fun productionMaterialsUseQuietBackdropOptics() {
        LiquidGlassDropdownMaterial.entries.forEach { material ->
            val metrics = liquidGlassDropdownMetrics(material)

            assertTrue(metrics.blurRadius <= 6.dp)
            assertTrue(metrics.lensStart <= 16.dp)
            assertTrue(metrics.lensEnd <= 32.dp)
            assertFalse(metrics.chromaticAberration)
            assertFalse(metrics.depthEffect)
            assertTrue(metrics.lightHighlightAlpha <= 0.64f)
        }
    }

    @Test
    fun activeBackdropSurfaceStaysTranslucent() {
        LiquidGlassDropdownMaterial.entries.forEach { material ->
            val light =
                buildLiquidGlassDropdownContainerColors(
                    isDark = false,
                    accentColor = Color.Blue,
                    material = material,
                    surfaceContainer = Color.White,
                )
            val dark =
                buildLiquidGlassDropdownContainerColors(
                    isDark = true,
                    accentColor = Color.Blue,
                    material = material,
                    surfaceContainer = Color(0xFF202124),
                )

            assertTrue(light.surfaceColor.alpha <= 0.53f)
            assertTrue(dark.surfaceColor.alpha <= 0.39f)
            assertTrue(light.fallbackBaseColor.alpha <= 0.73f)
            assertTrue(dark.fallbackBaseColor.alpha <= 0.79f)
        }
    }

    @Test
    fun actionMenuTracksBackdropDocumentationGeometry() {
        val metrics = liquidGlassDropdownMetrics(LiquidGlassDropdownMaterial.ActionMenu)

        assertEquals(16.dp, metrics.lensStart)
        assertEquals(32.dp, metrics.lensEnd)
    }
}
