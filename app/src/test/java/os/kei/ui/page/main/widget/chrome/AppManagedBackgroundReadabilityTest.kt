package os.kei.ui.page.main.widget.chrome

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * The non-Home background must not make the page's own text unreadable.
 *
 * The image is user-supplied, so the worst case has to be assumed rather than sampled: a white image in
 * dark theme, a black one in light. Composited strength is `opacity * (1 - overlay)`, and
 * [appManagedBackgroundRender] holds that below a ceiling solved from WCAG contrast.
 *
 * Measured before the ceiling existed: at the 40% maximum in dark theme primary text fell to **4.20:1**,
 * under the 4.5:1 AA line, and nothing in the default configuration prevented it —
 * `AppManagedBackgroundStyles.Standard` has a zero flat overlay and the reading-overlay preference
 * defaults to 0, so readability depended on the user discovering the "Readable" page style.
 */
class AppManagedBackgroundReadabilityTest {
    @Test
    fun theDefaultOpacityNeedsNoProtectionSoTheDefaultLookIsUnchanged() {
        // 16% is the shipped default. Worst case there is 9.29:1 dark / 14.48:1 light, so forcing an
        // overlay would only dim the image for nothing. The ceiling must stay out of the way.
        listOf(true, false).forEach { darkBase ->
            val render = appManagedBackgroundRender(DEFAULT_OPACITY, AppManagedBackgroundStyles.Standard, darkBase)
            assertEquals(
                "the default must not be dimmed (darkBase=$darkBase)",
                0f,
                render.readabilityOverlay,
                0f,
            )
            assertEquals(DEFAULT_OPACITY, render.imageOpacity, 1e-4f)
        }
    }

    @Test
    fun primaryTextStaysAtWcagAaAcrossTheWholeOpacityRange() {
        // Every reachable slider position, both themes, against the worst image for that theme.
        var step = MIN_OPACITY
        while (step <= MAX_OPACITY + 1e-4f) {
            listOf(
                Triple(true, DARK_BASE, WHITE),
                Triple(false, LIGHT_BASE, BLACK),
            ).forEach { (darkBase, base, worstImage) ->
                val render = appManagedBackgroundRender(step, AppManagedBackgroundStyles.Standard, darkBase)
                val text = if (darkBase) WHITE else BLACK
                val ratio = contrast(text, composite(worstImage, base, render))
                assertTrue(
                    "opacity=$step darkBase=$darkBase gave $ratio:1, under WCAG AA",
                    ratio >= WCAG_AA_BODY,
                )
            }
            step += 0.01f
        }
    }

    @Test
    fun theCeilingOnlyEngagesWhereItIsActuallyNeeded() {
        // Dark theme binds, because #242424 sits far closer to white than White does to black. So the
        // overlay must appear in dark theme before it appears in light, and only near the top of the range.
        val darkCeiling = appManagedBackgroundReadableStrengthCeiling(darkBase = true)
        val lightCeiling = appManagedBackgroundReadableStrengthCeiling(darkBase = false)
        assertTrue("dark should bind: $darkCeiling vs $lightCeiling", darkCeiling < lightCeiling)

        assertEquals(
            "30% should still be untouched",
            0f,
            appManagedBackgroundRender(0.30f, AppManagedBackgroundStyles.Standard, darkBase = true).readabilityOverlay,
            0f,
        )
        assertTrue(
            "the 40% maximum is the case that used to fail and must now be protected",
            appManagedBackgroundRender(
                MAX_OPACITY,
                AppManagedBackgroundStyles.Standard,
                darkBase = true,
            ).readabilityOverlay > 0f,
        )
    }

    @Test
    fun aPageStyleThatAlreadyDimsTheImageNeedsLessProtection() {
        // `Focused` multiplies opacity down to 62%, so the composite is already inside the ceiling and the
        // extra overlay must not stack on top of the preset's own dimming.
        val focused = appManagedBackgroundRender(MAX_OPACITY, AppManagedBackgroundStyles.Focused, darkBase = true)
        val standard = appManagedBackgroundRender(MAX_OPACITY, AppManagedBackgroundStyles.Standard, darkBase = true)
        assertTrue(
            "Focused already dims, so it should need no more than Standard does",
            focused.readabilityOverlay <= standard.readabilityOverlay,
        )
    }

    @Test
    fun malformedOpacityCannotProduceAnInvalidOverlay() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 5f).forEach { opacity ->
            val render = appManagedBackgroundRender(opacity, AppManagedBackgroundStyles.Standard, darkBase = true)
            assertTrue(
                "opacity=$opacity produced overlay ${render.readabilityOverlay}",
                render.readabilityOverlay in 0f..1f,
            )
            assertTrue(
                "opacity=$opacity produced image alpha ${render.imageOpacity}",
                render.imageOpacity in 0f..1f,
            )
        }
    }

    /**
     * Secondary text is a known limitation, recorded rather than silently "fixed".
     *
     * `onBackgroundVariant` is only 3.04:1 on plain White and 3.86:1 on plain `#242424` — already at or
     * under large-text AA before any image. No overlay rescues it: even 14% only reaches ~2.2:1 / ~2.5:1.
     * Fixing it means not putting secondary text on the raw page background, which is a change across
     * many pages rather than a tuning of this one.
     */
    @Test
    fun secondaryTextIsAlreadyMarginalBeforeAnyBackgroundIsApplied() {
        assertTrue(contrast(LIGHT_VARIANT, LIGHT_BASE) < 3.2f)
        assertTrue(contrast(DARK_VARIANT, DARK_BASE) < 4f)
    }

    // ---- WCAG plumbing, kept local so the assertions are self-contained -----------------------

    private fun composite(
        image: Color,
        base: Color,
        render: AppManagedBackgroundRender,
    ): Color {
        val strength = render.imageOpacity * (1f - render.readabilityOverlay)
        fun mix(
            i: Float,
            b: Float,
        ) = i * strength + b * (1f - strength)
        return Color(mix(image.red, base.red), mix(image.green, base.green), mix(image.blue, base.blue))
    }

    private fun contrast(
        foreground: Color,
        background: Color,
    ): Float {
        val a = relativeLuminance(foreground)
        val b = relativeLuminance(background)
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }

    private fun relativeLuminance(color: Color): Float {
        fun channel(value: Float): Float =
            if (value <= 0.03928f) value / 12.92f else ((value + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        return 0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)
    }

    private companion object {
        // Mirrors core-prefs. Duplicated deliberately: if the prefs range widens, this test should fail
        // rather than quietly stop covering the new positions.
        const val DEFAULT_OPACITY = 0.16f
        const val MIN_OPACITY = 0.06f
        const val MAX_OPACITY = 0.40f
        const val WCAG_AA_BODY = 4.5f

        val WHITE = Color.White
        val BLACK = Color.Black

        /** miuix `colorScheme.background` / `onBackgroundVariant`, light then dark. */
        val LIGHT_BASE = Color.White
        val DARK_BASE = Color(0xFF242424)
        val LIGHT_VARIANT = Color(0xFF8C93B0)
        val DARK_VARIANT = Color(0xFF787E96)
    }
}
