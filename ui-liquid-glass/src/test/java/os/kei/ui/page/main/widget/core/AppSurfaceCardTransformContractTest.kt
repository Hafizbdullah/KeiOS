package os.kei.ui.page.main.widget.core

import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSurfaceCardTransformContractTest {
    @Test
    fun interactiveTransformStaysInsideLiquidSurfaceBackdropLayer() {
        val cardSource = sourceFile(APP_FEATURE_CARDS_SOURCE)
        val liquidSurfaceSource = sourceFile(LIQUID_SURFACES_SOURCE)

        assertFalse("pressedScale" in cardSource)
        assertFalse("app_surface_card_press_scale" in cardSource)
        assertFalse(".graphicsLayer" in cardSource)
        assertTrue("isInteractive = showIndication && clickable" in cardSource)
        assertTrue("interactionSource = interactionSource" in cardSource)

        assertTrue("{ applyLiquidSurfaceInteractiveTransform(interactiveHighlight) }" in liquidSurfaceSource)
        assertTrue("layerBlock = interactiveLayerBlock" in liquidSurfaceSource)
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

private const val APP_FEATURE_CARDS_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/core/AppFeatureCards.kt"

private const val LIQUID_SURFACES_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/LiquidSurfaces.kt"
