package os.kei.ui.page.main.widget.glass

import java.io.File
import kotlin.test.assertTrue
import org.junit.Test

class LiquidGlassActionMenuSubmenuContractTest {
    @Test
    fun singleAndMultipleChoiceSubmenusForwardTheirCompleteRowModels() {
        val source = sourceFile(LIQUID_GLASS_ACTION_MENU_SOURCE)
        val submenuPanel =
            source
                .substringAfter("private fun LiquidGlassActionMenuSubmenuPanel(")
                .substringBefore("private fun LiquidGlassActionMenuQuickActionsRow(")
        val singleChoiceBranch =
            submenuPanel
                .substringAfter("is LiquidGlassActionMenuSingleChoiceRow ->")
                .substringBefore("is LiquidGlassActionMenuMultipleChoiceRow ->")
        val multipleChoiceBranch =
            submenuPanel.substringAfter("is LiquidGlassActionMenuMultipleChoiceRow ->")

        listOf(
            "leadingIcon = choice.leadingIcon",
            "trailingIcon = choice.trailingIcon",
            "subtitle = choice.subtitle",
            "variant = choice.variant",
            "enabled = choice.enabled",
        ).forEach { forwardingContract ->
            assertTrue(forwardingContract in singleChoiceBranch, forwardingContract)
            assertTrue(forwardingContract in multipleChoiceBranch, forwardingContract)
        }
        assertTrue("choice.onClick()" in singleChoiceBranch)
        assertTrue("onDismissRequest()" in singleChoiceBranch)
        assertTrue("onCheckedChange = choice.onCheckedChange" in multipleChoiceBranch)
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

private const val LIQUID_GLASS_ACTION_MENU_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/LiquidGlassActionMenu.kt"
