package os.kei.ui.page.main.os.shell

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OsShellLiquidThemeSourceTest {
    @Test
    fun shellLiquidVisualsFollowTheSelectedKeiOSTheme() {
        mapOf(
            "runner page" to OS_SHELL_RUNNER_PAGE_SOURCE,
            "runner cards" to OS_SHELL_RUNNER_CARDS_SOURCE,
            "output renderer" to OS_SHELL_OUTPUT_RENDERER_SOURCE,
            "panel surface" to SHELL_LIQUID_PANEL_SURFACE_SOURCE,
        ).forEach { (name, sourcePath) ->
            val source = sourceFile(sourcePath)

            assertEquals(
                1,
                source.occurrencesOf("isAppInDarkTheme()"),
                "$name must read the selected KeiOS theme exactly once",
            )
            assertFalse(
                "isSystemInDarkTheme" in source,
                "$name must remain aligned with the surrounding MiuixTheme",
            )
        }
    }

    @Test
    fun shellActivityProvidesTheStoredLiquidSheetPreference() {
        val activitySource = sourceFile(OS_SHELL_RUNNER_ACTIVITY_SOURCE)
        val providerBlock = activitySource.functionCallBlock("CompositionLocalProvider")

        assertEquals(
            1,
            activitySource.occurrencesOf("UiPrefs.isLiquidSheetEnabled()"),
            "The shell activity must read the persisted Liquid Sheet preference once",
        )
        assertTrue("val liquidSheetEnabled = UiPrefs.isLiquidSheetEnabled()" in activitySource)
        assertTrue(
            "LocalTransitionAnimationsEnabled provides chromePrefs.transitionAnimationsEnabled" in providerBlock,
        )
        assertTrue(
            "LocalPredictiveBackAnimationsEnabled provides predictiveBackPolicy.localPredictiveBackEnabled" in
                providerBlock,
        )
        assertTrue("LocalLiquidSheetEnabled provides liquidSheetEnabled" in providerBlock)
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

private const val OS_SHELL_RUNNER_PAGE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/shell/page/OsShellRunnerPage.kt"
private const val OS_SHELL_RUNNER_CARDS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/shell/component/OsShellRunnerCards.kt"
private const val OS_SHELL_OUTPUT_RENDERER_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/shell/OsShellOutputRenderer.kt"
private const val SHELL_LIQUID_PANEL_SURFACE_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/shell/ShellLiquidPanelSurface.kt"
private const val OS_SHELL_RUNNER_ACTIVITY_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/shell/OsShellRunnerActivity.kt"
