package os.kei.core.ui.effect.background

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class BgEffectFramePacingTest {
    @Test
    fun `dynamic background follows ARR without a fixed software frame cap`() {
        val modifierSource = sourceFile(BG_EFFECT_MODIFIER_SOURCE)
        val backgroundSource = sourceFile(BG_EFFECT_BACKGROUND_SOURCE)
        val animationSource =
            modifierSource
                .substringAfter("private fun startAnimation()", missingDelimiterValue = "")
                .substringBefore("private fun stopAnimation()")

        assertTrue(animationSource.isNotBlank())
        assertTrue("val now = withFrameNanos { it }" in animationSource)
        assertTrue("animTime =" in animationSource)
        assertTrue("invalidateDraw()" in animationSource)
        assertTrue(animationSource.indexOf("animTime =") < animationSource.indexOf("invalidateDraw()"))
        assertTrue(".preferredFrameRate(FrameRateCategory.High)" in backgroundSource)
        assertFalse("BG_EFFECT_HIGH_FPS" in modifierSource)
        assertFalse("BG_EFFECT_LOW_FPS" in modifierSource)
        assertFalse("minDeltaNanos" in modifierSource)
        assertFalse("framePacer" in modifierSource)
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
