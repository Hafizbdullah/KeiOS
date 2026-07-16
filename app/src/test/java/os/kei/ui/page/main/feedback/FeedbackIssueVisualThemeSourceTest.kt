package os.kei.ui.page.main.feedback

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FeedbackIssueVisualThemeSourceTest {
    @Test
    fun liquidPresentationFollowsTheAppTheme() {
        val source = sourceFile(FEEDBACK_ISSUE_VISUALS_SOURCE)

        assertEquals(3, source.occurrencesOf("isAppInDarkTheme()"))
        assertFalse("isSystemInDarkTheme" in source)
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

private const val FEEDBACK_ISSUE_VISUALS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/feedback/FeedbackIssueVisuals.kt"
