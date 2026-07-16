package os.kei.ui.page.main.ba

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaDualActionRowProductionReuseTest {
    @Test
    fun apLimitActionsReuseSharedCompactRowAndRetainBehavior() {
        val source = sourceFile(BA_AP_LIMIT_TOOLS_SOURCE)
        val actions = source.substringAfter("AppDualActionRow(")

        assertTrue("import os.kei.ui.page.main.widget.core.AppDualActionRow" in source)
        assertEquals(1, source.occurrencesOf("AppDualActionRow("))
        assertEquals(1, actions.occurrencesOf("spacing = 8.dp"))
        assertEquals(1, actions.occurrencesOf("first = { modifier ->"))
        assertEquals(1, actions.occurrencesOf("second = { modifier ->"))
        assertEquals(2, actions.occurrencesOf("modifier = modifier"))
        assertEquals(2, actions.occurrencesOf("AppLiquidTextButton("))
        assertEquals(2, actions.occurrencesOf("backdrop = backdrop"))
        assertEquals(2, actions.occurrencesOf("variant = GlassVariant.SheetAction"))
        assertEquals(2, actions.occurrencesOf("pressOverlayEnabled = true"))
        assertEquals(1, actions.occurrencesOf("R.string.ba_ap_limit_tools_set_max"))
        assertEquals(1, actions.occurrencesOf("R.string.common_save"))
        assertEquals(1, actions.occurrencesOf("onApLimitInputChange(BA_AP_LIMIT_MAX.toString())"))
        assertEquals(1, actions.occurrencesOf("onClick = onSaveApLimit"))
        assertFalse("Modifier.weight(1f)" in actions)
    }

    @Test
    fun accountDeleteActionsReuseSharedCompactRowAndRetainDangerSemantics() {
        val source = sourceFile(BA_ACCOUNT_MANAGEMENT_SOURCE)
        val deleteCard =
            source
                .substringAfter("private fun BaAccountDeleteConfirmCard(")
                .substringBefore("\n@Composable\ninternal fun BaAccountSelectableGroup")
        val actions = deleteCard.substringAfter("AppDualActionRow(")

        assertTrue("import os.kei.ui.page.main.widget.core.AppDualActionRow" in source)
        assertEquals(1, deleteCard.occurrencesOf("AppDualActionRow("))
        assertEquals(1, actions.occurrencesOf("spacing = 8.dp"))
        assertEquals(1, actions.occurrencesOf("first = { modifier ->"))
        assertEquals(1, actions.occurrencesOf("second = { modifier ->"))
        assertEquals(2, actions.occurrencesOf("modifier = modifier"))
        assertEquals(2, actions.occurrencesOf("AppLiquidTextButton("))
        assertEquals(2, actions.occurrencesOf("backdrop = backdrop"))
        assertEquals(1, actions.occurrencesOf("variant = GlassVariant.SheetAction"))
        assertEquals(1, actions.occurrencesOf("variant = GlassVariant.SheetDangerAction"))
        assertEquals(2, actions.occurrencesOf("pressOverlayEnabled = true"))
        assertEquals(1, actions.occurrencesOf("textColor = dangerColor"))
        assertEquals(1, actions.occurrencesOf("containerColor = dangerColor"))
        assertEquals(1, actions.occurrencesOf("onClick = onCancel"))
        assertEquals(1, actions.occurrencesOf("onClick = onConfirm"))
        assertFalse("Modifier.weight(1f)" in actions)
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

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { candidate -> candidate == needle }

private const val BA_AP_LIMIT_TOOLS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaApLimitToolsSheet.kt"
private const val BA_ACCOUNT_MANAGEMENT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaAccountManagementSheet.kt"
