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

    /**
     * The shell runner used to be an activity that re-provided these three locals itself, because
     * nothing above it did. It is a nav route now, so the host provides them once for every route —
     * and the assertion is worth more there: it covers the shell and every sibling page at once.
     */
    @Test
    fun theNavHostProvidesTheChromePreferencesEveryRouteReads() {
        val hostSource = sourceFile(MAIN_SCREEN_NAV_HOST_SOURCE)
        val providerBlock = hostSource.functionCallBlock("CompositionLocalProvider")

        assertTrue(
            "LocalTransitionAnimationsEnabled provides prefsState.transitionAnimationsEnabled" in providerBlock,
        )
        assertTrue(
            "LocalPredictiveBackAnimationsEnabled provides predictiveBackPolicy.localPredictiveBackEnabled" in
                providerBlock,
        )
        assertTrue("LocalLiquidSheetEnabled provides prefsState.liquidSheetEnabled" in providerBlock)
    }

    /**
     * The activity gave the shell a FocusedTask background and exported the material to descendant
     * Liquid surfaces. Both had to survive the move, or the panels lose the backdrop they sample.
     */
    @Test
    fun theShellRouteKeepsItsFocusedTaskMaterial() {
        val hostSource = sourceFile(MAIN_SCREEN_NAV_HOST_SOURCE)
        val entryBlock = hostSource.trailingLambdaBlock("entry<KeiosRoute.OsShellRunner>")

        assertTrue(
            "style = AppManagedBackgroundStyles.FocusedTask," in entryBlock,
            "The shell route must keep the focused-task background the activity gave it",
        )
        assertTrue(
            "exportBackdropToContent = true," in entryBlock,
            "The shell route must expose its managed background to descendant Liquid surfaces",
        )
    }

    /**
     * PrivilegedShell holds exactly one status callback, so a second attach would displace
     * MainActivity's and stop the rest of the app hearing about privilege changes.
     */
    @Test
    fun theShellRouteDoesNotAttachASecondPrivilegeCallback() {
        val hostSource = sourceFile(MAIN_SCREEN_NAV_HOST_SOURCE)

        assertEquals(
            0,
            hostSource.occurrencesOf("privilegedShell.attach"),
            "The shell route must read the shared privilege status, not attach its own callback",
        )
        assertEquals(
            0,
            hostSource.occurrencesOf("PrivilegedShell()"),
            "The shell route must use the shared PrivilegedShell, not construct another",
        )
    }
}

/** The body of `marker { … }`, matched by brace depth rather than parentheses. */
private fun String.trailingLambdaBlock(marker: String): String {
    val declaration = indexOf(marker)
    require(declaration >= 0) { "Unable to locate $marker" }
    val start = indexOf('{', startIndex = declaration)
    require(start >= 0) { "Unable to locate the opening brace for $marker" }

    var depth = 1
    var index = start + 1
    while (index < length && depth > 0) {
        when (this[index]) {
            '{' -> depth += 1
            '}' -> depth -= 1
        }
        index += 1
    }
    require(depth == 0) { "Unable to locate the closing brace for $marker" }
    return substring(start, index)
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
private const val MAIN_SCREEN_NAV_HOST_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/host/main/MainScreenNavHost.kt"
