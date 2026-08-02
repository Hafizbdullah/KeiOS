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

        assertTrue("filtersExpanded" in source)
        assertTrue("AppLiquidExpandableSection(" in source)
        assertFalse(
            "MiuixInfoItem(" in source,
            "The result count belongs in the picker hierarchy instead of another nested info row",
        )
    }

    @Test
    fun appPickerUsesIconOnlyRefreshAndShortCollapseAction() {
        val source = sourceFile(TRACK_APP_PICKER_SOURCE)
        val actionRow =
            source
                .substringAfter("val refreshContentDescription =")
                .substringBefore("AppLiquidExpandableSection(")

        assertTrue("AppLiquidIconButton(" in actionRow)
        assertTrue("icon = appLucideRefreshIcon()" in actionRow)
        assertTrue("contentDescription = refreshContentDescription" in actionRow)
        assertFalse("leadingIcon = appLucideRefreshIcon()" in actionRow)
        assertTrue("text = stringResource(R.string.github_track_sheet_btn_collapse)" in actionRow)

        val defaultStrings = sourceFile(GITHUB_STRINGS_SOURCE)
        val simplifiedCollapseLabel =
            "<string name=\"github_track_sheet_btn_collapse\">收起</string>"
        assertTrue(simplifiedCollapseLabel in defaultStrings)
    }

    @Test
    fun appTypeFiltersWrapWithoutTruncatingTheirLabels() {
        val source = sourceFile(TRACK_APP_PICKER_SOURCE)
        val filterCallSite =
            source.substring(
                startIndex = source.indexOf("GitHubTrackAppPickerButtonRow("),
                endIndex = source.indexOf("GitHubTrackAppPickerSortRow("),
            )
        val checkbox =
            source.substring(
                startIndex = source.indexOf("internal fun GitHubTrackAppTypeCheckbox("),
                endIndex = source.indexOf("internal fun GitHubTrackAppPickerButtonRow("),
            )

        assertTrue("FlowRow(" in source)
        assertFalse("Modifier.weight(" in filterCallSite)
        assertTrue("AppInteractiveTokens.compactControlRowMinHeight" in checkbox)
        assertTrue(".widthIn(min =" in checkbox)
        assertFalse("TextOverflow.Ellipsis" in checkbox)
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
private const val GITHUB_STRINGS_SOURCE =
    "app/src/main/res/values/strings_github.xml"
