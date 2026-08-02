package os.kei.ui.page.main.widget.sheet

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class SheetDesignRefreshContractTest {
    @Test
    fun sharedSectionHeaderKeepsTitleAndSummaryOnTheSheetLayer() {
        val source = sourceFile(SHEET_STYLES_SOURCE)
        val header = source.substringAfter("fun SheetSectionHeader(").substringBefore("\n@Composable")

        assertTrue("summary: String? = null" in header)
        assertTrue("SheetSectionTitle(" in header)
        assertTrue("AppTypographyTokens.Supporting" in header)
        assertFalse("SheetDescriptionText(" in header)
        assertFalse("SheetSectionCard(" in header)
    }

    @Test
    fun representativeSheetsUseFocusedSectionHierarchy() {
        REPRESENTATIVE_SHEET_SOURCES.forEach { path ->
            val source = sourceFile(path)
            assertTrue(
                "SheetSectionHeader(" in source,
                "$path should use the shared focused section hierarchy",
            )
            assertFalse(
                "SheetSectionTitle(" in source,
                "$path should not assemble legacy section titles directly",
            )
        }
    }

    @Test
    fun legacyDescriptionBlocksWereRemovedFromHighTrafficSheets() {
        LEGACY_DESCRIPTION_FREE_SOURCES.forEach { path ->
            val source = sourceFile(path)
            assertFalse(
                "SheetDescriptionText(" in source,
                "$path should place its overview copy in SheetSectionHeader",
            )
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

private const val SHEET_STYLES_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/sheet/SheetStyles.kt"

private val REPRESENTATIVE_SHEET_SOURCES =
    listOf(
        "app/src/main/java/os/kei/ui/page/main/ba/BaApLimitToolsSheet.kt",
        "app/src/main/java/os/kei/ui/page/main/ba/BaCafeApToolsSheet.kt",
        "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubDebugSheet.kt",
        "app/src/main/java/os/kei/ui/page/main/mcp/sheet/McpEditSheet.kt",
        "app/src/main/java/os/kei/ui/page/main/os/shell/OsShellRunnerSheets.kt",
        "app/src/main/java/os/kei/ui/page/main/student/page/BaStudentGuideCacheStatusSheet.kt",
    )

private val LEGACY_DESCRIPTION_FREE_SOURCES =
    listOf(
        "app/src/main/java/os/kei/ui/page/main/ba/BaApLimitToolsSheet.kt",
        "app/src/main/java/os/kei/ui/page/main/ba/BaCafeApToolsSheet.kt",
        "app/src/main/java/os/kei/ui/page/main/github/share/GitHubShareImportPendingSheet.kt",
        "app/src/main/java/os/kei/ui/page/main/github/share/GitHubShareImportAttachConfirmSheet.kt",
        "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubDebugSheet.kt",
    )
