package os.kei.ui.page.main.github

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GitHubPopupSheetThemeSourceTest {
    @Test
    fun popupAndSheetVisualsFollowTheSelectedKeiOSTheme() {
        mapOf(
            GITHUB_ACTIONS_PRIMITIVES_SOURCE to 2,
            GITHUB_ACTIONS_SHEET_CONTENT_SOURCE to 1,
            GITHUB_ACTIONS_SHEET_SECTIONS_SOURCE to 1,
            GITHUB_REPOSITORY_SCAN_CANDIDATE_ROWS_SOURCE to 1,
            GITHUB_TRACK_EDIT_FDROID_DISCOVERY_SECTION_SOURCE to 1,
        ).forEach { (sourcePath, expectedReadCount) ->
            val source = sourceFile(sourcePath)

            assertEquals(
                expectedReadCount,
                source.occurrencesOf("isAppInDarkTheme()"),
                "$sourcePath must read the selected KeiOS theme at each visual branch",
            )
            assertFalse(
                "isSystemInDarkTheme" in source,
                "$sourcePath must remain aligned with the surrounding MiuixTheme",
            )
        }
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

private const val GITHUB_ACTIONS_PRIMITIVES_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/actions/GitHubActionsPrimitives.kt"
private const val GITHUB_ACTIONS_SHEET_CONTENT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/actions/GitHubActionsSheetContent.kt"
private const val GITHUB_ACTIONS_SHEET_SECTIONS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/actions/GitHubActionsSheetSections.kt"
private const val GITHUB_REPOSITORY_SCAN_CANDIDATE_ROWS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubRepositoryScanCandidateRows.kt"
private const val GITHUB_TRACK_EDIT_FDROID_DISCOVERY_SECTION_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubTrackEditFdroidDiscoverySection.kt"
