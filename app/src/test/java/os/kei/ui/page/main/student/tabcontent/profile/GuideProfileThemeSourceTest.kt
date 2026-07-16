package os.kei.ui.page.main.student.tabcontent.profile

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test

class GuideProfileThemeSourceTest {
    @Test
    fun profileCapsulesAndGiftSurfacesFollowTheKeiOSAppTheme() {
        val source = sourceFile(GUIDE_PROFILE_UI_SOURCE)

        assertFalse("isSystemInDarkTheme" in source)
        assertEquals(2, source.occurrencesOf("isAppInDarkTheme()"))
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

private const val GUIDE_PROFILE_UI_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/tabcontent/profile/GuideProfileUi.kt"
