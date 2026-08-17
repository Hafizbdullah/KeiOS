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
 * The active half was not reachable on the API 37 AVD at all: every synthetic horizontal drag on a
 * settings slider is claimed by the settings pager, so `pressProgress` never leaves zero. A real finger
 * is still owed here.
 */
class LiquidSliderThumbColorControlsTest {
    @Test
    fun restingSaturationIsExactlyWhatVibrancyMeans() {
        // Backdrop: `vibrancy()` multiplies saturation by 1.5. Drift here silently desaturates the
        // resting thumb relative to every surface that still calls vibrancy().
        assertEquals(1.5f, SliderThumbVibrancySaturation, 0f)
    }

    @Test
    fun grabbingTheThumbAddsLightRatherThanRemovingIt() {
        assertTrue(
            "Pressed saturation must exceed resting, or the press reads as no change",
            SliderThumbPressedSaturation > SliderThumbVibrancySaturation,
        )
        assertTrue(
            "Pressed brightness must be positive; a negative lift would darken on touch",
            SliderThumbPressedBrightness > 0f,
        )
    }

    @Test
    fun theLiftStaysGentleEnoughToReadAsLight() {
        // A 20dp capsule of glass over arbitrary content, including photos. Past roughly these bounds the
        // effect stops looking like light on glass and starts looking like a colour bug.
        assertTrue(
            "Pressed saturation $SliderThumbPressedSaturation is beyond a plausible lift",
            SliderThumbPressedSaturation <= 2.2f,
        )
        assertTrue(
            "Pressed brightness $SliderThumbPressedBrightness is beyond a plausible lift",
            SliderThumbPressedBrightness <= 0.15f,
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
