package os.kei.ui.page.main.os.components

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OsDualActionRowProductionReuseTest {
    @Test
    fun backupTransferActionsReuseTheSharedCompactEqualWeightRow() {
        val contracts =
            listOf(
                TransferActionContract(
                    path = OS_SHELL_COMMAND_CARD_SHEETS_SOURCE,
                    sectionStart = "text = stringResource(R.string.os_shell_sheet_transfer_title)",
                    sectionEnd = "SheetDescriptionText(text = stringResource(R.string.os_shell_sheet_transfer_desc))",
                    exportText = "R.string.os_shell_sheet_action_export_backup",
                    importText = "R.string.os_shell_sheet_action_import_backup",
                ),
                TransferActionContract(
                    path = OS_PAGE_SECTIONS_SOURCE,
                    sectionStart = "text = stringResource(R.string.os_activity_sheet_transfer_title)",
                    sectionEnd = "@Composable\nprivate fun ActivityVisibilityGroup",
                    exportText = "R.string.os_activity_sheet_action_export_backup",
                    importText = "R.string.os_activity_sheet_action_import_backup",
                ),
            )

        contracts.forEach { contract ->
            val source = sourceFile(contract.path)
            assertTrue(contract.sectionStart in source, contract.path)
            assertTrue(contract.sectionEnd in source, contract.path)
            val transferSection =
                source
                    .substringAfter(contract.sectionStart)
                    .substringBefore(contract.sectionEnd)

            assertTrue("import os.kei.ui.page.main.widget.core.AppDualActionRow" in source, contract.path)
            assertEquals(1, transferSection.occurrencesOf("AppDualActionRow("), contract.path)
            assertEquals(1, transferSection.occurrencesOf("spacing = 8.dp"), contract.path)
            assertEquals(1, transferSection.occurrencesOf("first = { modifier ->"), contract.path)
            assertEquals(1, transferSection.occurrencesOf("second = { modifier ->"), contract.path)
            assertEquals(2, transferSection.occurrencesOf("modifier = modifier"), contract.path)
            assertEquals(2, transferSection.occurrencesOf("AppLiquidTextButton("), contract.path)
            assertEquals(2, transferSection.occurrencesOf("enabled = !transferInProgress"), contract.path)
            assertEquals(2, transferSection.occurrencesOf("variant = GlassVariant.SheetAction"), contract.path)
            assertEquals(2, transferSection.occurrencesOf("pressOverlayEnabled = true"), contract.path)
            assertTrue("text = stringResource(${contract.exportText})" in transferSection, contract.path)
            assertTrue("text = stringResource(${contract.importText})" in transferSection, contract.path)
            assertTrue("onClick = onExportAllCards" in transferSection, contract.path)
            assertTrue("onClick = onImportAllCards" in transferSection, contract.path)
            assertEquals(2, transferSection.occurrencesOf("backdrop = sheetBackdrop"), contract.path)
            assertFalse("Box(modifier = Modifier.weight(1f))" in transferSection, contract.path)
        }
    }
}

private data class TransferActionContract(
    val path: String,
    val sectionStart: String,
    val sectionEnd: String,
    val exportText: String,
    val importText: String,
)

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

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { candidate -> candidate == needle }

private const val OS_SHELL_COMMAND_CARD_SHEETS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/components/OsShellCommandCardSheets.kt"
private const val OS_PAGE_SECTIONS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/components/OsPageSections.kt"
