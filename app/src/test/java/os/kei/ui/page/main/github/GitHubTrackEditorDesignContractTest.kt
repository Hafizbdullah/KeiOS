package os.kei.ui.page.main.github

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class GitHubTrackEditorDesignContractTest {
    @Test
    fun editorUsesQuietHierarchyAndCollapsibleAdvancedOptions() {
        val source = sourceFile(TRACK_EDITOR_FORM_SOURCE)

        assertTrue("SheetSectionHeader(" in source)
        assertTrue("AppLiquidExpandableSection(" in source)
        assertTrue("checkOptionsExpanded" in source)
        assertFalse(
            "SheetInputTitle(repoInputLabel)" in source,
            "The repository field must not repeat the same label above and inside the input",
        )
    }

    @Test
    fun appPickerKeepsSearchPrimaryAndFiltersProgressive() {
        val source = sourceFile(TRACK_APP_PICKER_SOURCE)

        assertTrue("modifier = Modifier.weight(1f)" in source)
        assertTrue("filtersExpanded" in source)
        assertTrue("AppLiquidExpandableSection(" in source)
        assertFalse(
            "MiuixInfoItem(" in source,
            "The result count belongs in the picker hierarchy instead of another nested info row",
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

private const val TRACK_EDITOR_FORM_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubTrackEditFormContent.kt"
private const val TRACK_APP_PICKER_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubTrackAppPickerContent.kt"
