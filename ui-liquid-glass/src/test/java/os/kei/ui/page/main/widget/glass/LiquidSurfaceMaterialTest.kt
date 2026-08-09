package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidSurfaceMaterialTest {
    @Test
    fun enabledSurfaceOmitsIdentityContentLayer() {
        assertEquals(0, liquidSurfaceContentAlphaModifier(enabled = true).elementCount())
        assertEquals(1, liquidSurfaceContentAlphaModifier(enabled = false).elementCount())
    }

    @Test
    fun passiveSurfaceOmitsZeroContributionShadowLayers() {
        assertNull(
            liquidSurfaceOuterShadowOrNull(
                enabled = false,
                alpha = 0.10f,
            ),
        )
        assertNull(
            liquidSurfaceOuterShadowOrNull(
                enabled = true,
                alpha = 0f,
            ),
        )
        assertFalse(
            liquidSurfaceNeedsInteractiveInnerShadow(
                isInteractive = false,
                enabled = true,
            ),
        )
    }

    @Test
    fun interactiveSurfaceKeepsVisibleShadowLayers() {
        assertNotNull(
            liquidSurfaceOuterShadowOrNull(
                enabled = true,
                alpha = 0.10f,
            ),
        )
        assertTrue(
            liquidSurfaceNeedsInteractiveInnerShadow(
                isInteractive = true,
                enabled = true,
            ),
        )
    }

    @Test
    fun shadowBlurIsScaledToTheSurfaceItSitsUnder() {
        // The square-cornered shadow was geometry, not clipping: `Shadow.Default` is a fixed 24dp blur
        // spreading `radius * 2` every way, so on a 22dp checkbox it drew a silhouette four times the
        // control's size — and a silhouette that large has no corner rounding left to see, which reads
        // as a right angle behind a rounded shape.
        val checkbox = liquidSurfaceShadowRadius(22f)
        val pill = liquidSurfaceShadowRadius(32f)
        val field = liquidSurfaceShadowRadius(44f)
        val card = liquidSurfaceShadowRadius(160f)

        assertTrue("a checkbox must not wear a card's shadow: $checkbox", checkbox < 12.dp)
        assertTrue(pill > checkbox)
        assertTrue(field > pill)
        // Anything card-sized keeps exactly the previous radius, so cards and sheets do not move.
        assertEquals(LiquidShadowRadiusMax, card)
    }

    @Test
    fun shadowBlurStaysInsideItsBand() {
        assertEquals(LiquidShadowRadiusMin, liquidSurfaceShadowRadius(1f))
        assertEquals(LiquidShadowRadiusMax, liquidSurfaceShadowRadius(10_000f))
        // Before the first measurement, fall back to the ceiling rather than to a hard-edged zero blur.
        assertEquals(LiquidShadowRadiusMax, liquidSurfaceShadowRadius(0f))
        assertEquals(LiquidShadowRadiusMax, liquidSurfaceShadowRadius(Float.NaN))
        assertEquals(LiquidShadowRadiusMax, liquidSurfaceShadowRadius(-5f))
    }

    @Test
    fun idleHighlightStaysQuietInBothThemes() {
        assertEquals(
            0.62f,
            liquidSurfaceHighlightAlpha(
                isDark = false,
                interactive = true,
                enabled = true,
                pressProgress = 0f,
            ),
        )
        assertEquals(
            0.42f,
            liquidSurfaceHighlightAlpha(
                isDark = true,
                interactive = false,
                enabled = true,
                pressProgress = 0f,
            ),
        )
    }

    @Test
    fun pressAddsOnlyAControlledHighlightBoost() {
        val lightPressed =
            liquidSurfaceHighlightAlpha(
                isDark = false,
                interactive = true,
                enabled = true,
                pressProgress = 1f,
            )
        val darkPressed =
            liquidSurfaceHighlightAlpha(
                isDark = true,
                interactive = true,
                enabled = true,
                pressProgress = 1f,
            )

        assertTrue(lightPressed <= 0.72f)
        assertTrue(darkPressed <= 0.52f)
    }

    @Test
    fun malformedPressProgressIsClamped() {
        assertEquals(
            0.62f,
            liquidSurfaceHighlightAlpha(
                isDark = false,
                interactive = true,
                enabled = true,
                pressProgress = Float.POSITIVE_INFINITY,
            ),
        )
        assertEquals(
            0.62f,
            liquidSurfaceHighlightAlpha(
                isDark = false,
                interactive = true,
                enabled = true,
                pressProgress = Float.NaN,
            ),
        )
    }

    @Test
    fun explicitHighlightOverrideIsThemeIndependentAndClamped() {
        assertEquals(
            0.82f,
            liquidSurfaceHighlightAlpha(
                isDark = true,
                interactive = false,
                enabled = true,
                pressProgress = 0f,
                overrideAlpha = 0.82f,
            ),
        )
        assertEquals(
            1f,
            liquidSurfaceHighlightAlpha(
                isDark = false,
                interactive = true,
                enabled = true,
                pressProgress = 1f,
                overrideAlpha = 1.2f,
            ),
        )
        assertEquals(
            0f,
            liquidSurfaceHighlightAlpha(
                isDark = false,
                interactive = true,
                enabled = true,
                pressProgress = 1f,
                overrideAlpha = -0.2f,
            ),
        )
    }

    @Test
    fun malformedHighlightOverrideFallsBackToDefaultCurve() {
        assertEquals(
            0.62f,
            liquidSurfaceHighlightAlpha(
                isDark = false,
                interactive = true,
                enabled = true,
                pressProgress = 0f,
                overrideAlpha = Float.NaN,
            ),
        )
        assertEquals(
            0.42f,
            liquidSurfaceHighlightAlpha(
                isDark = true,
                interactive = false,
                enabled = true,
                pressProgress = 0f,
                overrideAlpha = Float.POSITIVE_INFINITY,
            ),
        )
    }
}

private fun Modifier.elementCount(): Int = foldIn(0) { count, _ -> count + 1 }
