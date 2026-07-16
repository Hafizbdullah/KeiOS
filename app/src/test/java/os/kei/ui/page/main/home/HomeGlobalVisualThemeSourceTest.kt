package os.kei.ui.page.main.home

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test

class HomeGlobalVisualThemeSourceTest {
    @Test
    fun homeHdrAndManagedBackgroundFollowTheKeiOSAppTheme() {
        val homeSource = sourceFile(HOME_PAGE_SECTIONS_SOURCE)
        val backgroundSource = sourceFile(BG_EFFECT_BACKGROUND_SOURCE)

        assertEquals(1, homeSource.occurrencesOf("isAppInDarkTheme()"))
        assertEquals(1, backgroundSource.occurrencesOf("isAppInDarkTheme()"))
        assertFalse("isSystemInDarkTheme" in homeSource)
        assertFalse("isSystemInDarkTheme" in backgroundSource)
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

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { it == needle }

private const val HOME_PAGE_SECTIONS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/home/HomePageSections.kt"
private const val BG_EFFECT_BACKGROUND_SOURCE =
    "app/src/main/java/os/kei/core/ui/effect/background/BgEffectBackground.kt"
