package os.kei.ui.page.main.widget.glass

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiquidSliderVisualOffsetContractTest {
    @Test
    fun visualOffsetMovesTrackKeyPointsAndThumbWhileInputLayerStaysUnshifted() {
        val source = sourceFile(LIQUID_SLIDER_VARIANTS_SOURCE)
        val trackSectionStart = source.indexOf("        val trackLayerHeight =")
        val inputSectionStart = source.indexOf("        if (enabled) {", trackSectionStart)
        val thumbSectionStart =
            source.indexOf(
                "        Box(\n            Modifier\n                .offset(y = safeVisualVerticalOffset)",
                inputSectionStart,
            )
        require(trackSectionStart >= 0) { "Missing track layer section" }
        require(inputSectionStart > trackSectionStart) { "Missing slider input section" }
        require(thumbSectionStart > inputSectionStart) { "Missing slider thumb section" }

        val trackSection = source.substring(trackSectionStart, inputSectionStart)
        val inputSection = source.substring(inputSectionStart, thumbSectionStart)
        val thumbSection = source.substring(thumbSectionStart)

        assertTrue(".offset(y = safeVisualVerticalOffset)" in trackSection)
        assertTrue("safeKeyPoints.forEach" in trackSection)
        assertFalse(".offset(y = safeVisualVerticalOffset)" in inputSection)
        assertTrue(".height(maxHeight)" in inputSection)
        assertTrue(".offset(y = safeVisualVerticalOffset)" in thumbSection)
        assertEquals(
            2,
            VISUAL_OFFSET_PATTERN.findAll(source).count(),
            "Only the shared track/key-point layer and thumb may receive the visual offset",
        )
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

private val VISUAL_OFFSET_PATTERN =
    Regex("""\.offset\(\s*y\s*=\s*safeVisualVerticalOffset\s*\)""")

private const val LIQUID_SLIDER_VARIANTS_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/LiquidSliderVariants.kt"
