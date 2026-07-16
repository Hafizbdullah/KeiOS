package os.kei.ui.page.main.ba

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test

class BaCalendarPoolThemeSourceTest {
    @Test
    fun standalonePageGradientsFollowTheKeiOSAppTheme() {
        listOf(
            sourceFile(BA_ACTIVITY_CALENDAR_SOURCE),
            sourceFile(BA_POOL_SOURCE),
        ).forEach { source ->
            assertFalse("isSystemInDarkTheme" in source)
            assertEquals(1, source.occurrencesOf("isAppInDarkTheme()"))
        }
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

private const val BA_ACTIVITY_CALENDAR_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaActivityCalendarActivity.kt"
private const val BA_POOL_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaPoolActivity.kt"
