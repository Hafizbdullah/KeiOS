package os.kei.ui.page.main.debug

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertEquals
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

    @Test
    fun sheetDetentButtonsExposeOneCompactRadioGroup() {
        val source = sourceFile(DEBUG_LIQUID_SHEET_CARD_SOURCE)
        val detentSection =
            source
                .substringAfter("LiquidSheetInitialDetent.entries.forEach")
                .substringBefore("\n        AppLiquidTextButton(")

        assertEquals(1, source.occurrencesOf(".selectableGroup()"))
        assertEquals(1, detentSection.occurrencesOf("role = Role.RadioButton"))
        assertEquals(1, detentSection.occurrencesOf("selected = selected"))
        assertTrue("val selected = detent == initialDetent" in detentSection)
        assertTrue("variant = if (selected) GlassVariant.SheetPrimaryAction else GlassVariant.Compact" in detentSection)
        assertTrue("minHeight = 42.dp" in detentSection)
        assertTrue("horizontalPadding = 4.dp" in detentSection)
    }
}

private fun String.occurrencesOf(value: String): Int = windowed(value.length).count { it == value }

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
