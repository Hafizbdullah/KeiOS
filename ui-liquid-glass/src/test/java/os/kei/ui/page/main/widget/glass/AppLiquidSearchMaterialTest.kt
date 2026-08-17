package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLiquidSearchMaterialTest {
    @Test
    fun productionSearchGeometryStaysWithinQuietLensBudget() {
        assertEquals(16.dp, SEARCH_FIELD_LENS_START)
        assertEquals(28.dp, SEARCH_FIELD_LENS_END)
        assertTrue(SEARCH_FIELD_LIGHT_HIGHLIGHT_ALPHA <= 0.62f)
        assertTrue(SEARCH_FIELD_DARK_HIGHLIGHT_ALPHA <= 0.42f)
        assertTrue(SEARCH_FIELD_LIGHT_FALLBACK_ALPHA <= 0.34f)
        assertTrue(SEARCH_FIELD_DARK_FALLBACK_ALPHA <= 0.28f)
    }

    @Test
    fun lightMaterialKeepsRimsAndGlowSubtle() {
        val colors = appLiquidSearchMaterialColors(isDark = false)

        assertTrue(colors.overlayTop.alpha <= 0.09f)
        assertTrue(colors.overlayBottom.alpha <= 0.06f)
        assertTrue(colors.centerGlow.alpha <= 0.11f)
        assertTrue(colors.bottomGlow.alpha <= 0.07f)
        assertTrue(colors.sideRim.alpha <= 0.17f)
        assertTrue(colors.innerRim.alpha <= 0.27f)
    }

    /**
     * The film must not carry a border, because both surfaces that use it already stroke
     * `glass.borderColor` at `glass.borderWidth` themselves.
     *
     * This is the regression that existed: the film had an `edge` colour that was the same 1.1dp ring
     * with, in light mode, the byte-identical `Color(0xFF86C3FF).copy(alpha = 0.32f)`. Two strokes of one
     * ring on one path composite to `0.32 + 0.32 * 0.68`, so the border rendered at roughly 0.54 —
     * two-thirds over strength — everywhere a search capsule appeared in light mode.
     */
    @Test
    fun theFilmLeavesTheBorderToTheVariantStyle() {
        val searchFieldBorder =
            listOf(true, false).map { isDark ->
                if (isDark) Color.White.copy(alpha = 0.18f) else Color(0xFF86C3FF).copy(alpha = 0.32f)
            }

        listOf(true, false).forEachIndexed { index, isDark ->
            val colors = appLiquidSearchMaterialColors(isDark = isDark)
            val border = searchFieldBorder[index]
            // The film's rim is white in both themes; the variant's border is accent-tinted in light.
            // Equal values here would mean the ring is defined twice again.
            assertTrue(
                "The film's rim must not restate the SearchField border (isDark=$isDark)",
                colors.innerRim != border,
            )
        }
    }

    /** The ramp the cached glow brushes replace, held to the values it used to draw. */
    @Test
    fun glowFractionReproducesTheOldPerFrameAlphaRamp() {
        val base = 0.100f
        val gain = APP_LIQUID_SEARCH_CENTER_GLOW_GAIN
        val peak = base + gain

        listOf(0f, 0.25f, 0.5f, 1f).forEach { progress ->
            val fraction =
                appLiquidSearchGlowAlphaFraction(
                    baseAlpha = base,
                    gain = gain,
                    materialProgress = progress,
                )
            // `base + gain * p` is what the old code baked into a fresh Brush each draw.
            assertEquals(base + gain * progress, peak * fraction, 1e-5f)
        }
    }

    /** A glow whose resting alpha is zero — the dark compact centre specular — still ramps from zero. */
    @Test
    fun aGlowThatRestsAtZeroRampsLinearly() {
        assertEquals(
            0.5f,
            appLiquidSearchGlowAlphaFraction(baseAlpha = 0f, gain = APP_LIQUID_SEARCH_CENTER_GLOW_GAIN, materialProgress = 0.5f),
            1e-5f,
        )
        assertEquals(
            0f,
            appLiquidSearchGlowAlphaFraction(baseAlpha = 0f, gain = APP_LIQUID_SEARCH_CENTER_GLOW_GAIN, materialProgress = 0f),
            1e-5f,
        )
    }

    @Test
    fun focusedHighlightHasAQuietUpperBound() {
        val alpha =
            appLiquidSearchHighlightAlpha(
                baseAlpha = SEARCH_FIELD_LIGHT_HIGHLIGHT_ALPHA,
                materialProgress = 1f,
                isDark = false,
            )

        assertTrue(alpha <= 0.68f)
    }

    @Test
    fun lightPlaceholderUsesReadableContentColor() {
        val placeholder =
            appLiquidSearchPlaceholderColor(
                contentColor = Color.Black,
                isDark = false,
            )

        assertTrue(placeholder.alpha in 0.61f..0.63f)
    }
}
