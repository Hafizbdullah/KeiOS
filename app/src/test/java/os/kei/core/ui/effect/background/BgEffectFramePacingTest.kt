package os.kei.core.ui.effect.background

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class BgEffectFramePacingTest {
    /**
     * The loop must keep waking on every VSYNC so 120/90/60 Hz and thermal transitions share one
     * phase, while capping how often it actually invalidates: on a 120 Hz panel an untouched Home
     * otherwise redraws the whole Liquid Glass page at 120 fps for a slow drift.
     */
    @Test
    fun `dynamic background stays vsync phased but caps invalidation`() {
        val modifierSource = sourceFile(BG_EFFECT_MODIFIER_SOURCE)
        val backgroundSource = sourceFile(BG_EFFECT_BACKGROUND_SOURCE)
        val animationSource =
            modifierSource
                .substringAfter("private fun startAnimation()", missingDelimiterValue = "")
                .substringBefore("private fun stopAnimation()")

        assertTrue(animationSource.isNotBlank())
        // Still driven by Choreographer, so the phase survives ARR/LTPO and thermal changes.
        assertTrue("val now = withFrameNanos { it }" in animationSource)
        assertTrue("animTime =" in animationSource)
        assertTrue("invalidateDraw()" in animationSource)
        assertTrue(animationSource.indexOf("animTime =") < animationSource.indexOf("invalidateDraw()"))
        // The shader caps itself at BG_EFFECT_HIGH_FPS, so it votes that rate rather than
        // FrameRateCategory.High: the category resolved to 90Hz on 5eea1f50 and pinned the panel
        // there for as long as the background was visible. Votes aggregate as a max, so declaring
        // the real cadence lets pager motion still pull the display up to its peak.
        assertFalse("preferredFrameRate(FrameRateCategory" in backgroundSource)
        assertTrue(".preferredFrameRate(BG_EFFECT_VOTE_HZ)" in backgroundSource)
        assertTrue("private const val BG_EFFECT_VOTE_HZ = 60f" in backgroundSource)

        // The cap skips the invalidate, never the VSYNC wake-up.
        assertTrue("minDeltaNanos" in animationSource)
        assertTrue(animationSource.indexOf("withFrameNanos") < animationSource.indexOf("minDeltaNanos"))
        assertTrue("continue" in animationSource)
        assertTrue("BG_EFFECT_HIGH_FPS" in modifierSource)
        assertTrue("BG_EFFECT_LOW_FPS" in modifierSource)
        assertFalse("framePacer" in modifierSource)
    }

    /** animTime comes from real elapsed time, so capping changes sampling rate, not drift speed. */
    @Test
    fun `animation speed is independent of the invalidation cap`() {
        val animationSource =
            sourceFile(BG_EFFECT_MODIFIER_SOURCE)
                .substringAfter("private fun startAnimation()", missingDelimiterValue = "")
                .substringBefore("private fun stopAnimation()")

        assertTrue("(now - origin)" in animationSource)
        assertFalse("lastEmit)" in animationSource.substringAfter("animTime ="))
    }
}

private fun sourceFile(relativePath: String): String {
    val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    val sourceFile =
        generateSequence(workingDirectory) { directory -> directory.parentFile }
            .map { directory -> File(directory, relativePath) }
            .firstOrNull(File::isFile)
    return requireNotNull(sourceFile) {
        "Unable to locate $relativePath from $workingDirectory"
    }.readText()
}

private const val BG_EFFECT_MODIFIER_SOURCE =
    "app/src/main/java/os/kei/core/ui/effect/background/BgEffectModifier.kt"
private const val BG_EFFECT_BACKGROUND_SOURCE =
    "app/src/main/java/os/kei/core/ui/effect/background/BgEffectBackground.kt"
