package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.Modifier
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
