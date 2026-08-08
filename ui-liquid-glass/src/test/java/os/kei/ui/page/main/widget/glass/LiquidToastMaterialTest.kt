package os.kei.ui.page.main.widget.glass

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The toast's material and motion.
 *
 * Both were the toast's real defects rather than matters of taste:
 *
 * - It filled with `Color.White.copy(alpha = 0.5f)` in *both* themes. That is the identical mistake the
 *   pre-rewrite dialog made — a pale pill on a dark app, carrying `onBackground` text that is itself
 *   near-white in dark mode.
 * - Its scale ran through `AnimatedVisibility(scaleIn/scaleOut)`, an ancestor `graphicsLayer` around a
 *   `drawBackdrop` element, which scales the sampled backdrop along with the pill. The scale now goes
 *   through `drawBackdrop`'s own `layerBlock`, which is where the library says to put it.
 */
class LiquidToastMaterialTest {
    @Test
    fun theFillDiffersByThemeInsteadOfBeingWhiteInBoth() {
        val dark = liquidToastGlassFill(isDark = true)
        val light = liquidToastGlassFill(isDark = false)

        assertNotEquals(dark, light, "one fill for both themes is the bug this replaces")
        assertTrue(
            dark.red < 0.5f && dark.green < 0.5f && dark.blue < 0.5f,
            "the dark fill must actually be dark, not white at half alpha: $dark",
        )
        assertTrue(
            light.red > 0.5f && light.green > 0.5f && light.blue > 0.5f,
            "the light fill must read as light: $light",
        )
    }

    @Test
    fun theGlassFillStaysOpaqueEnoughToReadAgainstABusyPage() {
        // A toast is small and gets no scrim, so the fill is the only contrast it has. Anything much
        // below this and body text behind the pill shows through it.
        assertTrue(liquidToastGlassFill(isDark = true).alpha >= 0.80f)
        assertTrue(liquidToastGlassFill(isDark = false).alpha >= 0.80f)
    }

    @Test
    fun theFallbackFillIsHeavierThanTheGlassOne() {
        // The fallback runs when there is no usable backdrop, so it has nothing blurred underneath to
        // hide behind and must stand on its own.
        listOf(true, false).forEach { isDark ->
            assertTrue(
                liquidToastFallbackFill(isDark).alpha > liquidToastGlassFill(isDark).alpha,
                "fallback must be more opaque than glass (isDark=$isDark)",
            )
        }
    }

    @Test
    fun theFallbackSharesTheGlassFillsHueSoTheThemesDoNotJump() {
        listOf(true, false).forEach { isDark ->
            val glass = liquidToastGlassFill(isDark)
            val fallback = liquidToastFallbackFill(isDark)
            assertEquals(glass.copy(alpha = 1f), fallback.copy(alpha = 1f))
        }
    }

    @Test
    fun motionRunsFromASmallerPillToFullSize() {
        assertEquals(1f, liquidToastScale(1f), 0.0001f)
        assertTrue(liquidToastScale(0f) < 1f, "the pill has to grow into place")
        assertTrue(liquidToastScale(0f) > 0.5f, "but not from nothing — that reads as a pop, not a rise")
    }

    @Test
    fun alphaReachesFullBeforeTheScaleDoes() {
        assertEquals(0f, liquidToastAlpha(0f), 0.0001f)
        assertEquals(1f, liquidToastAlpha(1f), 0.0001f)
        assertTrue(
            liquidToastAlpha(0.7f) >= 1f,
            "the pill must be solid before it stops growing, or the fade reads as a second animation",
        )
    }

    @Test
    fun motionIsMonotonicSoTheExitNeverBacktracks() {
        // One driver runs both directions, so a non-monotonic curve would make the exit visibly
        // stutter on the way out. This is the property that makes the flinch impossible.
        var previousScale = liquidToastScale(0f)
        var previousAlpha = liquidToastAlpha(0f)
        (1..20).forEach { step ->
            val progress = step / 20f
            val scale = liquidToastScale(progress)
            val alpha = liquidToastAlpha(progress)
            assertTrue(scale >= previousScale, "scale regressed at $progress")
            assertTrue(alpha >= previousAlpha, "alpha regressed at $progress")
            previousScale = scale
            previousAlpha = alpha
        }
    }

    @Test
    fun outOfRangeProgressIsClamped() {
        assertEquals(liquidToastScale(1f), liquidToastScale(1.6f), 0.0001f)
        assertEquals(liquidToastScale(0f), liquidToastScale(-0.4f), 0.0001f)
        assertEquals(1f, liquidToastAlpha(2f), 0.0001f)
        assertEquals(0f, liquidToastAlpha(-1f), 0.0001f)
    }
}
