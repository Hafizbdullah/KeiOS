package os.kei.ui.page.main.os.shell

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OsShellLiquidBackdropContractTest {
    @Test
    fun runnerCardsExportTheirRealMaterialToNestedControls() {
        val source = sourceFile(OS_SHELL_RUNNER_CARDS_SOURCE)

        assertEquals(
            2,
            source.occurrencesOf("exportBackdropToContent = true,"),
            "Both input and output cards must export their rendered material",
        )
    }

    @Test
    fun nestedPanelsInheritTheExportedCardMaterial() {
        val source = sourceFile(SHELL_LIQUID_PANEL_SURFACE_SOURCE)

        assertTrue(
            "val panelBackdrop = LocalLiquidParentBackdrop.current" in source,
            "Shell panels must inherit the active material exported by their parent card",
        )
        assertTrue(
            "backdrop = panelBackdrop," in source,
            "The inherited card material must reach the panel LiquidSurface",
        )
        assertFalse(
            "rememberLayerBackdrop" in source || ".layerBackdrop(" in source,
            "Shell panels must not replace real card material with an empty producer",
        )
    }

    @Test
    fun shellInputReusesTheBareContentLayerWithoutChangingItsMaterialContract() {
        val shellInputSource = sourceFile(SHELL_COMMAND_INPUT_FIELD_SOURCE)
        val shellSurfaceSource = sourceFile(SHELL_LIQUID_PANEL_SURFACE_SOURCE)
        val sharedInputSource = sourceFile(APP_TEXT_INPUT_CONTENT_SOURCE)
        val liquidInputSource = sourceFile(APP_LIQUID_INPUT_FIELD_SOURCE)

        assertTrue("AppTextInputContent(" in shellInputSource)
        assertFalse("BasicTextField(" in shellInputSource)
        assertTrue("ShellLiquidPanelSurface(" in shellInputSource)
        assertTrue("fieldModifier = Modifier.heightIn(min = minHeight - 24.dp)" in shellInputSource)
        assertTrue("singleLine = false" in shellInputSource)
        assertTrue("keyboardOptions = KeyboardOptions.Default" in shellInputSource)
        assertTrue("focusRequester = focusRequester" in shellInputSource)
        assertTrue("text = \"$\"" in shellInputSource)

        assertTrue("AppTextInputContent(" in liquidInputSource)
        assertFalse("BasicTextField(" in liquidInputSource)
        assertTrue("BasicTextField(" in sharedInputSource)
        assertFalse(
            "Backdrop" in sharedInputSource ||
                "drawBackdrop" in sharedInputSource ||
                "layerBackdrop" in sharedInputSource,
            "The shared text-input atom must stay a plain foreground content layer",
        )

        assertTrue("chromaticAberration = true" in shellSurfaceSource)
        assertTrue("depthEffect = true" in shellSurfaceSource)
        assertTrue("blurRadius = 8.dp" in shellSurfaceSource)
        assertTrue("lensRadius = 24.dp" in shellSurfaceSource)
    }

    @Test
    fun scrollingTopBarKeepsItsIndependentRealProducer() {
        val pageSource = sourceFile(OS_SHELL_RUNNER_PAGE_SOURCE)
        val contentSource = sourceFile(OS_SHELL_RUNNER_CONTENT_SOURCE)

        assertTrue("val topBarBackdrop =" in pageSource)
        assertTrue("rememberLayerBackdrop {" in pageSource)
        assertTrue(".layerBackdrop(topBarBackdrop)" in contentSource)
    }
}

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { it == needle }

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

private const val OS_SHELL_RUNNER_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/shell/component/OsShellRunnerCards.kt"
private const val SHELL_LIQUID_PANEL_SURFACE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/shell/ShellLiquidPanelSurface.kt"
private const val SHELL_COMMAND_INPUT_FIELD_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/shell/ShellCommandInputField.kt"
private const val OS_SHELL_RUNNER_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/shell/page/OsShellRunnerPage.kt"
private const val OS_SHELL_RUNNER_CONTENT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/shell/page/OsShellRunnerContent.kt"
private const val APP_TEXT_INPUT_CONTENT_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/AppTextInputContent.kt"
private const val APP_LIQUID_INPUT_FIELD_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/AppLiquidSearchField.kt"
