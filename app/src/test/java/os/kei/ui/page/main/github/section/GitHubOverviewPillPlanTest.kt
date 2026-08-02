package os.kei.ui.page.main.github.section

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test

class GitHubOverviewPillPlanTest {
    @Test
    fun overviewUsesFixedCompactMetricPills() {
        val plan = buildGitHubOverviewExpandedPillPlan()

        assertEquals(
            listOf(
                GitHubOverviewExpandedPillKind.Stable,
                GitHubOverviewExpandedPillKind.PreRelease,
                GitHubOverviewExpandedPillKind.CheckFailed,
            ),
            plan.map { it.kind },
        )
    }

    @Test
    fun overviewCustomizationEntryPointsStayRemoved() {
        val overviewSource = sourceFile(OVERVIEW_SOURCE)
        val overviewCard =
            overviewSource
                .substringAfter("internal fun GitHubOverviewCard(")
                .substringBefore("\n@Composable\nprivate fun GitHubOverviewExpandedContent")

        assertFalse("visibleEntries" in overviewCard)
        assertFalse("onEditVisibleEntries" in overviewCard)
        assertFalse("onLongClick" in overviewCard)
        assertFalse("GitHubOverviewEntrySheet" in sourceFile(SHEET_HOST_SOURCE))
        assertFalse("overviewVisibleEntries" in sourceFile(PAGE_STATE_SOURCE))
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

private const val OVERVIEW_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/section/GitHubOverviewSection.kt"
private const val SHEET_HOST_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/page/GitHubPageSheetHost.kt"
private const val PAGE_STATE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/page/GitHubPageState.kt"
