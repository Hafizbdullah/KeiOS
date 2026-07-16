package os.kei.ui.page.main.student.catalog.component

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaGuideBgmLibraryHeaderReuseTest {
    @Test
    fun libraryHeaderReusesSharedCardHeaderWithoutChangingItsCompactVisualRhythm() {
        val source = sourceFile(BA_GUIDE_BGM_LIBRARY_CONTROLS_SOURCE)
        val header = source.libraryHeaderSource()
        val sharedHeader = header.functionCallBlock("AppCardHeader")

        assertTrue("import os.kei.ui.page.main.widget.core.AppCardHeader" in source)
        assertEquals(1, header.occurrencesOf("AppCardHeader("))
        assertTrue("title = stringResource(R.string.ba_catalog_bgm_library_title)" in sharedHeader)
        assertTrue("subtitle = summary" in sharedHeader)
        assertTrue("subtitleColor = MiuixTheme.colorScheme.onBackgroundVariant" in sharedHeader)
        assertTrue("titleTypography = AppTypographyTokens.BodyEmphasis" in sharedHeader)
        assertTrue("subtitleTypography = AppTypographyTokens.Supporting" in sharedHeader)
        assertTrue("textVerticalSpacing = 1.dp" in sharedHeader)
        assertTrue("horizontalSpacing = 10.dp" in sharedHeader)
        assertTrue("contentPadding = PaddingValues(0.dp)" in sharedHeader)
        assertTrue("minHeight = 50.dp" in sharedHeader)
        assertTrue("titleMaxLines = 1" in sharedHeader)
        assertTrue("subtitleMaxLines = 1" in sharedHeader)
        assertTrue("expandTint = MiuixTheme.colorScheme.onBackgroundVariant" in sharedHeader)
        assertTrue(".padding(horizontal = 12.dp, vertical = 10.dp)" in header)
        assertFalse("appLucideChevronDownIcon" in source)
        assertFalse("appLucideChevronUpIcon" in source)
    }

    @Test
    fun queueStatusAndExpansionBehaviorStayInTheHeaderWhileSortControlsStayVisible() {
        val header = sourceFile(BA_GUIDE_BGM_LIBRARY_CONTROLS_SOURCE).libraryHeaderSource()
        val sharedHeader = header.functionCallBlock("AppCardHeader")

        assertTrue("endActions = {" in sharedHeader)
        assertEquals(1, sharedHeader.occurrencesOf("StatusPill("))
        assertTrue("R.string.ba_catalog_bgm_library_queue_summary" in sharedHeader)
        assertTrue("displayedCount.coerceAtLeast(0)" in sharedHeader)
        assertTrue("color = accent" in sharedHeader)
        assertTrue("size = AppStatusPillSize.Compact" in sharedHeader)
        assertTrue("expandable = true" in sharedHeader)
        assertTrue("expanded = toolsExpanded" in sharedHeader)
        assertTrue("onClick = { toolsExpanded = !toolsExpanded }" in sharedHeader)

        val sortGroupIndex = header.indexOf("BaGuideBgmSortGroupDropdownRow(")
        val toolsVisibilityIndex = header.indexOf("AnimatedVisibility(visible = toolsExpanded)")
        assertTrue(sortGroupIndex >= 0)
        assertTrue(toolsVisibilityIndex >= 0)
        assertTrue(sortGroupIndex < toolsVisibilityIndex)
    }
}

private fun String.libraryHeaderSource(): String =
    substringAfter("internal fun BaGuideBgmLibraryHeader(")
        .substringBefore("\n@Composable\nprivate fun BaGuideBgmLibraryToolsContent(")

private fun String.functionCallBlock(name: String): String {
    val start = indexOf("$name(")
    require(start >= 0) { "Unable to locate $name call" }

    var depth = 0
    var insideString = false
    var escaped = false
    for (index in start until length) {
        val character = this[index]
        when {
            escaped -> {
                escaped = false
            }

            character == '\\' && insideString -> {
                escaped = true
            }

            character == '"' -> {
                insideString = !insideString
            }

            insideString -> {
                continue
            }

            character == '(' -> {
                depth += 1
            }

            character == ')' -> {
                depth -= 1
                if (depth == 0) return substring(start, index + 1)
            }
        }
    }
    error("Unbalanced $name call")
}

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { candidate -> candidate == needle }

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

private const val BA_GUIDE_BGM_LIBRARY_CONTROLS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/component/BaGuideBgmLibraryControls.kt"
