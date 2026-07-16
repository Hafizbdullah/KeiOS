package os.kei.ui.page.main.widget.glass

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLiquidFloatingSurfaceTransformContractTest {
    @Test
    fun pressTransformKeepsBackdropSamplingStableAndVisualLayersSynchronized() {
        val source = sourceFile(LIQUID_SURFACES_SOURCE)
        val floatingSurfaceSource =
            source
                .substringAfter("fun AppLiquidFloatingSurface(")
                .substringBeforeLast("\n}")
        val interactionRoot =
            floatingSurfaceSource
                .substringAfter("    Box(\n        modifier =")
                .substringBefore("        contentAlignment = Alignment.Center,")

        assertTrue("val pressVisualTransform: GraphicsLayerScope.() -> Unit" in floatingSurfaceSource)
        assertFalse(".graphicsLayer" in interactionRoot)
        assertTrue("layerBlock = pressVisualTransform" in floatingSurfaceSource)
        assertTrue("translationY = snapChromeTranslationPx(-pressLiftPx * pressProgress)" in floatingSurfaceSource)
        assertTrue("scaleX = lerp(1f, 1.010f, pressProgress)" in floatingSurfaceSource)
        assertTrue("scaleY = lerp(1f, 0.992f, pressProgress)" in floatingSurfaceSource)
        assertEquals(
            3,
            floatingSurfaceSource.windowed(".graphicsLayer(block = pressVisualTransform)".length)
                .count { it == ".graphicsLayer(block = pressVisualTransform)" },
        )
        assertTrue("exportedBackdrop = exportedBackdrop" in floatingSurfaceSource)
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

private const val LIQUID_SURFACES_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/LiquidSurfaces.kt"
