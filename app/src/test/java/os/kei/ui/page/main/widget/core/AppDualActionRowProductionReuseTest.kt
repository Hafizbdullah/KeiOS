package os.kei.ui.page.main.widget.core

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AppDualActionRowProductionReuseTest {
    @Test
    fun fourProductionActionPairsReuseTheSharedEqualWeightRow() {
        val contracts =
            listOf(
                ProductionContract(
                    path = BA_GUIDE_TRANSFER_GROUPS_SOURCE,
                    buttonConstructor = "AppStandaloneLiquidTextButton(",
                    retainedMarkers =
                        listOf(
                            "onClick = onExport",
                            "onClick = onImport",
                            "leadingIcon = exportIcon",
                            "leadingIcon = importIcon",
                        ),
                ),
                ProductionContract(
                    path = BA_STUDENT_CACHE_STATUS_SOURCE,
                    buttonConstructor = "AppLiquidTextButton(",
                    retainedMarkers =
                        listOf(
                            "onClick = onRefreshCurrentStudent",
                            "onClick = onClearCurrentStudentCache",
                            "enabled = hasStatus",
                        ),
                ),
                ProductionContract(
                    path = BA_CAFE_AP_TOOLS_SOURCE,
                    buttonConstructor = "AppLiquidTextButton(",
                    retainedMarkers =
                        listOf(
                            "onClearCafeStoredAp()",
                            "onFillCafeStoredAp()",
                            "onDismissRequest()",
                        ),
                ),
                ProductionContract(
                    path = BA_CAFE_COOLDOWN_EDIT_SOURCE,
                    buttonConstructor = "AppLiquidTextButton(",
                    retainedMarkers =
                        listOf(
                            "onSaveRemaining(0L)",
                            "onSaveRemaining(parseCooldownInputMs(hoursInput, minutesInput, secondsInput))",
                        ),
                ),
            )

        contracts.forEach { contract ->
            val source = sourceFile(contract.path)

            assertTrue("import os.kei.ui.page.main.widget.core.AppDualActionRow" in source, contract.path)
            assertEquals(1, source.occurrencesOf("AppDualActionRow("), contract.path)
            assertTrue("spacing = 8.dp" in source, contract.path)
            assertEquals(1, source.occurrencesOf("first = { modifier ->"), contract.path)
            assertEquals(1, source.occurrencesOf("second = { modifier ->"), contract.path)
            assertTrue(source.occurrencesOf("modifier = modifier") >= 2, contract.path)
            assertTrue(source.occurrencesOf(contract.buttonConstructor) >= 2, contract.path)
            contract.retainedMarkers.forEach { marker ->
                assertTrue(marker in source, "$marker missing from ${contract.path}")
            }
        }
    }

    @Test
    fun sharedRowOwnsFillWidthEqualWeightsAndCallerSelectedSpacing() {
        val source = sourceFile(APP_CONTROL_ROWS_SOURCE)
        val dualActionRow =
            source
                .substringAfter("fun AppDualActionRow(")
                .substringBefore("\n}")

        assertTrue("modifier = modifier.fillMaxWidth()" in dualActionRow)
        assertTrue("horizontalArrangement = Arrangement.spacedBy(spacing)" in dualActionRow)
        assertTrue("verticalAlignment = Alignment.CenterVertically" in dualActionRow)
        assertTrue("first(Modifier.weight(1f))" in dualActionRow)
        assertTrue("second(Modifier.weight(1f))" in dualActionRow)
        assertFalse("clickable(" in dualActionRow)
        assertFalse("semantics" in dualActionRow)
    }
}

private data class ProductionContract(
    val path: String,
    val buttonConstructor: String,
    val retainedMarkers: List<String>,
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

private fun String.occurrencesOf(needle: String): Int =
    windowed(needle.length).count { candidate -> candidate == needle }

private const val APP_CONTROL_ROWS_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/core/AppControlRows.kt"
private const val BA_GUIDE_TRANSFER_GROUPS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/catalog/page/BaGuideCatalogTransferGroups.kt"
private const val BA_STUDENT_CACHE_STATUS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/student/page/BaStudentGuideCacheStatusSheet.kt"
private const val BA_CAFE_AP_TOOLS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaCafeApToolsSheet.kt"
private const val BA_CAFE_COOLDOWN_EDIT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaCafeCooldownEditSheet.kt"
