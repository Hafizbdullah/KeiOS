package os.kei.ui.page.main.widget.glass

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The slider thumb's colour controls, which replaced a fixed `vibrancy()`.
 *
 * These exist because the interesting half cannot be caught on a device. The resting appearance is only
 * correct if it reproduces `vibrancy()` exactly — the Backdrop docs define that as
 * `colorControls(saturation = 1.5f)` — and nothing about a screenshot would reveal a drift from 1.5 to,
 * say, 1.4; the thumb would simply be slightly duller than every other glass surface in the app forever.
 *
 * The active half is not reachable by **synthetic** input on the API 37 AVD: every `adb shell input`
 * horizontal drag on a settings slider is claimed by the settings pager, so `pressProgress` never leaves
 * zero. It *is* reachable by a real mouse drag on the emulator window, and that is how the pressed
 * saturation was caught — see `grabbingTheThumbAddsLightWithoutAddingChroma`.
 */
class LiquidSliderThumbColorControlsTest {
    @Test
    fun restingSaturationIsExactlyWhatVibrancyMeans() {
        // Backdrop: `vibrancy()` multiplies saturation by 1.5. Drift here silently desaturates the
        // resting thumb relative to every surface that still calls vibrancy().
        assertEquals(1.5f, SliderThumbVibrancySaturation, 0f)
    }

    /**
     * The invariant a screenshot of a resting slider can never guard, and the one a real drag caught.
     *
     * `colorControls` is the **colour filter** in Backdrop's `color filter ⇒ blur ⇒ lens` order, so it
     * saturates the backdrop before this lens — which runs `chromaticAberration = true` — splits it into
     * channels. Any pressed saturation above resting therefore multiplies the chroma of the aberration
     * fringes, and it does so at exactly the moment `blur` lerps to zero and the lens amount grows, so
     * there is nothing left to soften them.
     *
     * Measured off a 120fps recording of a real mouse drag, over a 46px band of the thumb: max HLS
     * saturation went 0.31 at rest to **1.00** mid-drag, with a violet fringe above and an orange one
     * below — complementary, which is channel separation rather than light.
     */
    @Test
    fun grabbingTheThumbAddsLightWithoutAddingChroma() {
        assertEquals(
            "Pressed saturation must equal resting: this lens has chromatic aberration on, and the " +
                "colour filter runs before it, so a chroma lift becomes a rainbow fringe",
            SliderThumbVibrancySaturation,
            SliderThumbPressedSaturation,
            0f,
        )
        assertTrue(
            "Brightness is the whole of the press emphasis now, so it must be positive and non-trivial",
            SliderThumbPressedBrightness >= 0.08f,
        )
    }

    @Test
    fun theLiftStaysGentleEnoughToReadAsLight() {
        // A 20dp capsule of glass over arbitrary content, including photos. Past roughly this bound a
        // luminance lift stops looking like light on glass and starts washing the refraction out.
        assertTrue(
            "Pressed brightness $SliderThumbPressedBrightness is beyond a plausible lift",
            SliderThumbPressedBrightness <= 0.18f,
        )
    }

    @Test
    fun theThumbNoLongerCallsTheFixedVibrancyHelper() {
        // The point of the change: the effect stack is progress-driven. A `vibrancy()` call coming back
        // would pin the thumb to one appearance whether it is idle or grabbed.
        //
        // Asserted on the imports, not on the body: the KDoc here and in the source both discuss
        // `vibrancy()` by name to explain what the resting values reproduce, and a naive text search for
        // the call finds that prose. An import cannot be prose.
        val source = sourceFile(LIQUID_SLIDER_SOURCE).readText()

        assertTrue("The thumb should drive colorControls", "colorControls(" in source)
        assertTrue(
            "colorControls must be imported for the progress-driven form",
            "import com.kyant.backdrop.effects.colorControls" in source,
        )
        assertFalse(
            "vibrancy is fixed; importing it again means something reverted to one appearance",
            "import com.kyant.backdrop.effects.vibrancy" in source,
        )
    }
}

private fun sourceFile(relativePath: String): File {
    val candidates =
        generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
    return requireNotNull(
        candidates
            .map { File(it, relativePath) }
            .firstOrNull(File::isFile),
    ) {
        "Unable to locate $relativePath from ${System.getProperty("user.dir")}"
    }
}

private const val LIQUID_SLIDER_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/LiquidSliderVariants.kt"
