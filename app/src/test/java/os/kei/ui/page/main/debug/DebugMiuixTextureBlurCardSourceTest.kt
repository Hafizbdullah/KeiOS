package os.kei.ui.page.main.debug

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class DebugMiuixTextureBlurCardSourceTest {
    @Test
    fun sampleStaysOnTheMiuixBlurChannelWithoutKyantBackdropMixing() {
        val source = sourceFile(DEBUG_MIUIX_TEXTURE_BLUR_CARD_SOURCE)

        assertTrue("import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop" in source)
        assertTrue("import top.yukonga.miuix.kmp.blur.layerBackdrop" in source)
        assertTrue("import top.yukonga.miuix.kmp.blur.textureBlur" in source)
        assertTrue("import top.yukonga.miuix.kmp.blur.progressiveTextureBlur" in source)
        assertEquals(2, source.occurrencesOf("rememberLayerBackdrop()"))
        assertEquals(2, source.occurrencesOf(".layerBackdrop("))
        assertEquals(1, source.occurrencesOf(".textureBlur("))
        assertEquals(1, source.occurrencesOf(".progressiveTextureBlur("))
        assertTrue("gradient = ProgressiveBlur.Bottom" in source)

        // The stages must never sample or export the kyant glass channel.
        assertFalse("import com.kyant.backdrop" in source)
        assertFalse("LocalLiquidParentBackdrop" in source)
        assertFalse("exportBackdropToContent" in source)
    }

    @Test
    fun frostedStageReusesNamedBlendTokens() {
        val source = sourceFile(DEBUG_MIUIX_TEXTURE_BLUR_CARD_SOURCE)

        assertTrue("ColorBlendToken.Colored_Regular_Dark" in source)
        assertTrue("ColorBlendToken.Colored_Regular_Light" in source)
        assertFalse("BlendColorEntry(" in source)
    }

    @Test
    fun catalogPageRegistersTheMiuixSampleCard() {
        val page = sourceFile(DEBUG_LIQUID_CATALOG_PAGE_SOURCE)

        assertEquals(1, page.occurrencesOf("DebugMiuixTextureBlurCard("))
    }
}

private fun String.occurrencesOf(needle: String): Int =
    windowed(needle.length).count { candidate -> candidate == needle }

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

private const val DEBUG_MIUIX_TEXTURE_BLUR_CARD_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugMiuixTextureBlurCard.kt"

private const val DEBUG_LIQUID_CATALOG_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugLiquidCatalogPage.kt"
