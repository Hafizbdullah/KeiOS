package os.kei.ui.page.main.os.state

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class OsPageUiContextThemeSourceTest {
    @Test
    fun sharedPageVisualsFollowTheSelectedKeiOSTheme() {
        val source = sourceFile(OS_PAGE_UI_CONTEXT_SOURCE)

        assertEquals(
            1,
            source.occurrencesOf("isAppInDarkTheme()"),
            "The shared OS page context must read the selected KeiOS theme exactly once",
        )
        assertFalse(
            "isSystemInDarkTheme" in source,
            "OS page visuals must remain aligned with the surrounding MiuixTheme",
        )
    }
}

private fun String.occurrencesOf(needle: String): Int =
    windowed(needle.length).count { it == needle }

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

private const val OS_PAGE_UI_CONTEXT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/state/OsPageUiContext.kt"
