package os.kei.ui.page.main.widget.sheet

import java.io.File
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiquidSheetPreferenceProviderContractTest {
    @Test
    fun `main root seeds repository from stored liquid sheet preference`() {
        val source = sourceFile(MAIN_PREFS_VIEW_MODEL_SOURCE)

        assertEquals(1, source.occurrencesOf("UiPrefs.isLiquidSheetEnabled()"))
        assertTrue("initialSnapshot =" in source)
        assertTrue("liquidSheetEnabled = UiPrefs.isLiquidSheetEnabled()," in source)
    }

    @Test
    fun `github share root provides stored liquid sheet preference`() {
        val source = sourceFile(GITHUB_SHARE_IMPORT_ACTIVITY_SOURCE)
        val providerBlock = source.functionCallBlock("CompositionLocalProvider")

        assertEquals(1, source.occurrencesOf("UiPrefs.isLiquidSheetEnabled()"))
        assertTrue("val liquidSheetEnabled = UiPrefs.isLiquidSheetEnabled()" in source)
        assertTrue("LocalLiquidSheetEnabled provides liquidSheetEnabled" in providerBlock)
    }

    @Test
    fun `component lab keeps its explicit liquid sheet showcase`() {
        val source = sourceFile(DEBUG_LIQUID_SHEET_CARD_SOURCE)

        assertTrue("useLiquidGlassSheet = true," in source)
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

private const val MAIN_PREFS_VIEW_MODEL_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/host/main/MainScreenPrefsViewModel.kt"
private const val GITHUB_SHARE_IMPORT_ACTIVITY_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/share/GitHubShareImportActivity.kt"
private const val DEBUG_LIQUID_SHEET_CARD_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugLiquidSheetCard.kt"
