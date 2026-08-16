package os.kei.ui.page.main.widget

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A component must ask the *app* for the theme, not the system.
 *
 * `isAppInDarkTheme()` folds the app's own Light/Dark override over `isSystemInDarkTheme()`. Calling the
 * system directly skips that override, so the component renders for whichever theme the *device* is in —
 * permanently wrong whenever the two disagree, which a restart does not fix and which looks exactly like
 * "this component did not update when I switched the theme".
 *
 * Found in `SettingsBackgroundSection`, where it drove `onApplyNonHomeBackgroundReadableSuggestion`, so an
 * app forced to Light on a Dark device applied the dark readable preset.
 *
 * Two call sites are legitimate and are listed by name: the helper itself, and `MainActivity`, which has
 * to resolve `FOLLOW_SYSTEM` to a concrete appearance for the system bars.
 */
class AppThemeSourceContractTest {
    @Test
    fun onlyTheHelperAndTheActivityReadTheSystemThemeDirectly() {
        val offenders =
            sourceFiles()
                .filter { (_, source) -> "isSystemInDarkTheme()" in source }
                .map { (path, _) -> path }
                .filterNot { path -> path in ALLOWED }

        assertTrue(
            offenders.isEmpty(),
            "These read the system theme instead of the app theme, so an in-app Light/Dark choice does " +
                "not reach them: $offenders. Use isAppInDarkTheme().",
        )
    }

    @Test
    fun theAllowedCallSitesStillExistSoThisTestCannotPassVacuously() {
        val present =
            sourceFiles()
                .filter { (path, source) -> path in ALLOWED && "isSystemInDarkTheme()" in source }
                .map { (path, _) -> path }
                .toSet()

        assertEquals(ALLOWED, present, "The allowed list has drifted from the code")
    }

    private fun sourceFiles(): List<Pair<String, String>> =
        MODULES
            .map { module -> File(repoRoot(), module) }
            .filter(File::isDirectory)
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .map { file -> file.relativeTo(repoRoot()).path to file.readText().withoutComments() }

    /**
     * Comments are stripped before scanning, because a doc line *naming* the API is not a call to it —
     * the comment explaining this very rule tripped the first version of this test.
     */
    private fun String.withoutComments(): String =
        COMMENT.replace(this) { match -> match.value.filter { it == '\n' } }

    private companion object {
        val MODULES =
            listOf(
                "app/src/main",
                "ui-liquid-glass/src/main",
            )

        /** Block and line comments, newlines preserved so reported positions stay meaningful. */
        val COMMENT = Regex("""/\*.*?\*/|//[^\n]*""", RegexOption.DOT_MATCHES_ALL)

        val ALLOWED =
            setOf(
                "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/AppThemeAppearance.kt",
                "app/src/main/java/os/kei/MainActivity.kt",
            )

        fun repoRoot(): File {
            val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
            return requireNotNull(
                generateSequence(workingDirectory) { it.parentFile }
                    .firstOrNull { File(it, "settings.gradle.kts").isFile },
            ) {
                "Unable to locate the repository root from $workingDirectory"
            }
        }
    }
}
