package os.kei.ui.page.main.widget.chrome

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class ManagedActivityLiquidBackdropContractTest {
    @Test
    fun liquidActivitiesExportTheirManagedPageMaterial() {
        listOf(
            "GitHub Star Import" to GITHUB_STAR_IMPORT_ACTIVITY_SOURCE,
            "Feedback" to FEEDBACK_ISSUE_ACTIVITY_SOURCE,
            "JSON Import" to JSON_IMPORT_ACTIVITY_SOURCE,
            "OS Shell" to OS_SHELL_RUNNER_ACTIVITY_SOURCE,
        ).forEach { (pageName, sourcePath) ->
            val hostCall = sourceFile(sourcePath).functionCallBlock("AppManagedBackgroundHost")

            assertTrue(
                "exportBackdropToContent = true," in hostCall,
                "$pageName must expose its real managed background to descendant Liquid surfaces",
            )
        }
    }

    @Test
    fun topBarSamplingKeepsAnIndependentPageBackdrop() {
        listOf(
            "GitHub Star Import" to GITHUB_STAR_IMPORT_PAGE_SOURCE,
            "Feedback" to FEEDBACK_ISSUE_PAGE_SOURCE,
            "JSON Import" to JSON_IMPORT_PAGE_SOURCE,
        ).forEach { (pageName, sourcePath) ->
            val source = sourceFile(sourcePath)

            assertTrue(
                "val pageBackdrop = rememberLayerBackdrop()" in source,
                "$pageName must keep a backdrop identity dedicated to its scrolling top bar",
            )
            assertTrue(
                "titleBackdrop = pageBackdrop," in source,
                "$pageName must keep its top bar connected to the dedicated page backdrop",
            )
        }
    }
}

private fun String.functionCallBlock(functionName: String): String {
    val marker = "$functionName("
    val start = indexOf(marker)
    require(start >= 0) { "Unable to locate $marker" }

    var depth = 1
    var index = start + marker.length
    while (index < length && depth > 0) {
        when (this[index]) {
            '(' -> depth += 1
            ')' -> depth -= 1
        }
        index += 1
    }
    require(depth == 0) { "Unable to locate the closing parenthesis for $marker" }
    return substring(start, index)
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

private const val GITHUB_STAR_IMPORT_ACTIVITY_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/importer/GitHubStarImportActivity.kt"
private const val GITHUB_STAR_IMPORT_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/importer/GitHubStarImportPage.kt"
private const val FEEDBACK_ISSUE_ACTIVITY_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/feedback/FeedbackIssueActivity.kt"
private const val FEEDBACK_ISSUE_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/feedback/FeedbackIssuePage.kt"
private const val JSON_IMPORT_ACTIVITY_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/jsonimport/KeiOSJsonImportActivity.kt"
private const val JSON_IMPORT_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/jsonimport/KeiOSJsonImportPage.kt"
private const val OS_SHELL_RUNNER_ACTIVITY_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/shell/OsShellRunnerActivity.kt"
