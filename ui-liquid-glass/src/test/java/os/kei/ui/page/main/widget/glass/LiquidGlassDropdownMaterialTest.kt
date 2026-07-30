package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassDropdownMaterialTest {
    @Test
    fun productionMaterialsBalanceReadableSurfacesWithVisibleRefraction() {
        val dropdown = liquidGlassDropdownMetrics(LiquidGlassDropdownMaterial.Default)
        val actionMenu = liquidGlassDropdownMetrics(LiquidGlassDropdownMaterial.ActionMenu)

        assertTrue(dropdown.blurRadius in 10.dp..14.dp)
        assertTrue(dropdown.depthEffect)
        assertTrue(dropdown.shadowElevation >= 24.dp)
        assertTrue(actionMenu.blurRadius > dropdown.blurRadius)
        assertTrue(actionMenu.shadowElevation > dropdown.shadowElevation)
        assertTrue(actionMenu.lensEnd > dropdown.lensEnd)
        assertTrue(dropdown.lensEnd >= dropdown.blurRadius * 3f)
        assertTrue(actionMenu.lensEnd >= actionMenu.blurRadius * 3f)
        assertTrue(dropdown.lightHighlightAlpha >= 0.64f)
        assertTrue(actionMenu.lightHighlightAlpha > dropdown.lightHighlightAlpha)
        assertFalse(dropdown.chromaticAberration)
        assertFalse(actionMenu.chromaticAberration)
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

            assertTrue(light.surfaceColor.alpha in 0.58f..0.64f)
            assertTrue(dark.surfaceColor.alpha in 0.51f..0.57f)
            assertTrue(light.fallbackBaseColor.alpha >= 0.94f)
            assertTrue(dark.fallbackBaseColor.alpha >= 0.92f)
            assertTrue(light.borderColor.alpha >= 0.09f)
            assertTrue(dark.borderColor.alpha in 0.18f..0.20f)
        }
    }

    @Test
    fun actionMenuTracksBackdropDocumentationGeometry() {
        val metrics = liquidGlassDropdownMetrics(LiquidGlassDropdownMaterial.ActionMenu)

        assertEquals(32.dp, metrics.lensStart)
        assertEquals(54.dp, metrics.lensEnd)
    }

    @Test
    fun opticalFieldsResolveFromEachContainerBounds() {
        LiquidGlassDropdownMaterial.entries.forEach { material ->
            val colors =
                buildLiquidGlassDropdownContainerColors(
                    isDark = false,
                    accentColor = Color.Blue,
                    material = material,
                    surfaceContainer = Color.White,
                )

            listOf(
                colors.surfaceGradientBrush,
                colors.surfaceCausticBrush,
                colors.fallbackMiddleBrush,
                colors.fallbackSheenBrush,
            ).forEach { brush ->
                assertTrue(brush.intrinsicSize.width.isNaN())
                assertTrue(brush.intrinsicSize.height.isNaN())
            }
        }
    }

    @Test
    fun contentContrastUsesLocalThemeRelativeHalo() {
        val light = liquidGlassDropdownContentShadow(isDark = false, enabled = true)
        val dark = liquidGlassDropdownContentShadow(isDark = true, enabled = true)

        assertEquals(Color.White, light.color.copy(alpha = 1f))
        assertEquals(Color.Black, dark.color.copy(alpha = 1f))
        assertTrue(light.color.alpha in 0.38f..0.42f)
        assertTrue(dark.color.alpha in 0.42f..0.46f)
        assertEquals(2f, light.blurRadius)
        assertEquals(2f, dark.blurRadius)
    }
}
