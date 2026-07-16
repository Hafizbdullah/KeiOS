package os.kei.ui.page.main.os.shell

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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
