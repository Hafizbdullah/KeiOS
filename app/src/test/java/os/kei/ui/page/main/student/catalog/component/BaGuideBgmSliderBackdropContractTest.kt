package os.kei.ui.page.main.student.catalog.component

import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaGuideBgmSliderBackdropContractTest {
    @Test
    fun favoritePlaybackSlidersInheritTheirGuideCardMaterial() {
        val source = sourceFile(BA_GUIDE_BGM_FAVORITE_CARDS_SOURCE)

        listOf(
            "BaGuideBgmPlaybackSeekBar" to "horizontal = 4.dp",
            "BaGuideBgmVolumeSlider" to "horizontal = 3.dp",
        ).forEach { (functionName, horizontalPadding) ->
            val slider = source.composableFunctionBlock(functionName)

            assertTrue(
                "val sliderBackdrop = LocalLiquidParentBackdrop.current" in slider,
                "$functionName must inherit the material exported by GuideLiquidCard",
            )
            assertTrue(
                "backdrop = sliderBackdrop," in slider,
                "$functionName must pass the inherited card material to its Liquid slider",
            )
            assertFalse(
                "rememberLayerBackdrop" in slider || ".layerBackdrop(" in slider,
                "$functionName must avoid an empty local producer",
            )
            assertTrue(
                ".height(48.dp)" in slider,
                "$functionName must retain its 48dp interaction height",
            )
            assertTrue(
                ".padding($horizontalPadding)" in slider,
                "$functionName must retain its compact horizontal inset",
            )
        }
    }
}

private fun String.composableFunctionBlock(functionName: String): String {
    val marker = "fun $functionName("
    val start = indexOf(marker)
    require(start >= 0) { "Unable to locate $marker" }
    val end = indexOf("\n@Composable", startIndex = start + marker.length)
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

private const val BA_GUIDE_BGM_FAVORITE_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/BaGuideBgmFavoriteCards.kt"
