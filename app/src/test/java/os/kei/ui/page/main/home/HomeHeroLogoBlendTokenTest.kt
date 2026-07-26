package os.kei.ui.page.main.home

import androidx.compose.ui.graphics.Color
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import os.kei.core.ui.effect.background.blend.ColorBlendToken
import top.yukonga.miuix.kmp.blur.BlurBlendMode

class HomeHeroLogoBlendTokenTest {
    @Test
    fun heroLogoTokensKeepTheOriginalInlineBlendValues() {
        val light = ColorBlendToken.HomeHeroLogo_Light
        assertEquals(3, light.size)
        assertEquals(Color(0xCC4A4A4A), light[0].color)
        assertEquals(BlurBlendMode.ColorBurn, light[0].mode)
        assertEquals(Color(0xFF4F4F4F), light[1].color)
        assertEquals(BlurBlendMode.LinearLight, light[1].mode)
        assertEquals(Color(0xFFFF5C96), light[2].color)
        assertEquals(BlurBlendMode.Lab, light[2].mode)

        val dark = ColorBlendToken.HomeHeroLogo_Dark
        assertEquals(3, dark.size)
        assertEquals(Color(0xE6A1A1A1), dark[0].color)
        assertEquals(BlurBlendMode.ColorDodge, dark[0].mode)
        assertEquals(Color(0x4DE6E6E6), dark[1].color)
        assertEquals(BlurBlendMode.LinearLight, dark[1].mode)
        assertEquals(Color(0xFFFF73AD), dark[2].color)
        assertEquals(BlurBlendMode.Lab, dark[2].mode)
    }

    @Test
    fun heroForegroundBlurReusesNamedTokensInsteadOfInlineEntries() {
        val source = sourceFile(HOME_PAGE_SECTIONS_SOURCE)
        val blur =
            source.substringAfter("internal fun Modifier.homeHeroForegroundBlur(")
                .substringBefore("\n@Composable\ninternal fun HomeInfoCard(")

        assertTrue("ColorBlendToken.HomeHeroLogo_Dark" in blur)
        assertTrue("ColorBlendToken.HomeHeroLogo_Light" in blur)
        assertFalse("BlendColorEntry(" in blur)
        assertFalse("BlendColorEntry(" in source)
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

private const val HOME_PAGE_SECTIONS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/home/HomePageSections.kt"
