package os.kei.ui.page.main.ba

import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaNotificationSliderBackdropContractTest {
    @Test
    fun thresholdSliderInheritsTheSheetCardMaterial() {
        val slider = sourceFile(BA_NOTIFICATION_SETTINGS_SHEET_SOURCE)
            .composableFunctionBlock("BaApThresholdQuickSlider")

        assertTrue(
            "val sliderBackdrop = LocalLiquidParentBackdrop.current" in slider,
            "The threshold slider must inherit the material exported by SheetSectionCard",
        )
        assertTrue(
            "backdrop = sliderBackdrop," in slider,
            "The inherited card material must reach LiquidKeyPointSlider",
        )
        assertFalse(
            "rememberLayerBackdrop" in slider || ".layerBackdrop(" in slider,
            "An empty local producer cannot provide a visible sampling source",
        )
    }

    @Test
    fun thresholdSliderKeepsItsAccessibleCompactGeometry() {
        val slider = sourceFile(BA_NOTIFICATION_SETTINGS_SHEET_SOURCE)
            .composableFunctionBlock("BaApThresholdQuickSlider")

        assertTrue(
            ".height(48.dp)" in slider,
            "The threshold slider must retain its 48dp interaction height",
        )
        assertTrue(
            ".padding(horizontal = 4.dp)" in slider,
            "The threshold slider must retain its compact horizontal inset",
        )
    }
}

private fun String.composableFunctionBlock(functionName: String): String {
    val marker = "fun $functionName("
    val start = indexOf(marker)
    require(start >= 0) { "Unable to locate $marker" }
    val end = indexOf("\nprivate fun ", startIndex = start + marker.length)
        .takeIf { it >= 0 }
        ?: length
    return substring(start, end)
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

private const val BA_NOTIFICATION_SETTINGS_SHEET_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaNotificationSettingsSheet.kt"
