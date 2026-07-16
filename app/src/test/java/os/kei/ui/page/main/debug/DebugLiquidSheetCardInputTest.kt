package os.kei.ui.page.main.debug

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class DebugLiquidSheetCardInputTest {
    @Test
    fun sheetLabUsesTheProductionLiquidInputField() {
        val source = sourceFile(DEBUG_LIQUID_SHEET_CARD_SOURCE)
        val inputSection =
            source
                .substringAfter("val fieldDescription =")
                .substringBefore("SheetDescriptionText(")

        assertTrue("AppLiquidInputField(" in inputSection)
        assertTrue("backdrop = LocalLiquidParentBackdrop.current" in inputSection)
        assertTrue("variant = GlassVariant.SheetInput" in inputSection)
        assertTrue("minHeight = 48.dp" in inputSection)
        assertTrue("cornerRadius = 14.dp" in inputSection)
        assertTrue("horizontalPadding = 14.dp" in inputSection)
        assertTrue("verticalPadding = 14.dp" in inputSection)
        assertFalse("BasicTextField(" in source)
        assertFalse("RoundedCornerShape(" in source)
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

private const val DEBUG_LIQUID_SHEET_CARD_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/debug/DebugLiquidSheetCard.kt"
