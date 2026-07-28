package os.kei.ui.page.main.github.page

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class GitHubTrackAppPickerSynchronizationTest {
    @Test
    fun trackedPackageFilterObservesSnapshotListContents() {
        val source = sourceFile(GITHUB_PAGE_SHEET_HOST_SOURCE)

        assertTrue(
            "derivedStateOf {" in source,
            "Tracked package candidates must derive from observable snapshot contents",
        )
        assertTrue(
            "state.trackedItems.map { item -> item.packageName }.toSet()" in source,
            "The app picker must derive its exclusion set from current tracked items",
        )
        assertFalse(
            "remember(state.trackedItems)" in source,
            "SnapshotStateList identity cannot be used as the tracked-package cache key",
        )
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

private const val GITHUB_PAGE_SHEET_HOST_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/page/GitHubPageSheetHost.kt"
