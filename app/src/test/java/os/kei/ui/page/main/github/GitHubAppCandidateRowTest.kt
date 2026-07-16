package os.kei.ui.page.main.github

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.feature.github.model.InstalledAppItem
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = GitHubAppCandidateRowTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class GitHubAppCandidateRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun candidateRowKeepsCompactHeightAndOneRadioAction() {
        var clickCount = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                GitHubAppCandidateRow(
                    app = candidate,
                    selected = true,
                    showInstallSource = false,
                    onClick = { clickCount++ },
                    modifier = Modifier.testTag("candidate-row"),
                )
            }
        }

        composeRule
            .onNodeWithTag("candidate-row")
            .assertHeightIsEqualTo(72.dp)

        val radioButton =
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.RadioButton,
            )
        composeRule.onAllNodes(radioButton).assertCountEquals(1)
        composeRule.onAllNodes(radioButton, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onNode(radioButton).performClick()
        composeRule.runOnIdle { assertEquals(1, clickCount) }
    }

    @Test
    fun longCandidateContentAtLargeFontKeepsTextAndInstallSourceSeparated() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density = density.density, fontScale = 1.5f)
                ) {
                    GitHubAppCandidateRow(
                        app = longCandidate,
                        selected = false,
                        showInstallSource = true,
                        onClick = {},
                        modifier =
                            Modifier
                                .width(360.dp)
                                .testTag("large-font-candidate-row"),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("large-font-candidate-row")
            .assertWidthIsEqualTo(360.dp)
            .assertHeightIsAtLeast(83.dp)

        val titleBounds =
            composeRule
                .onNodeWithText(longCandidate.label, useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        val packageBounds =
            composeRule
                .onNodeWithText(longCandidate.packageName, useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        val installSourceBounds =
            composeRule
                .onNodeWithText(longCandidate.installSourceLabel, useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot

        assertTrue(titleBounds.bottom <= packageBounds.top)
        assertTrue(titleBounds.right <= installSourceBounds.left)
        assertTrue(packageBounds.right <= installSourceBounds.left)
        with(composeRule.density) {
            assertTrue(titleBounds.height.toDp() <= 30.dp)
            assertTrue(packageBounds.height.toDp() <= 27.dp)
        }
    }

    @Test
    fun candidateColorsRetainLightAndDarkSelectedAndUnselectedRoles() {
        lateinit var lightCapture: CandidateThemeCapture
        lateinit var darkCapture: CandidateThemeCapture

        composeRule.setContent {
            Column {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    val capture =
                        CandidateThemeCapture(
                            surfaceContainer = MiuixTheme.colorScheme.surfaceContainer,
                            onBackgroundVariant = MiuixTheme.colorScheme.onBackgroundVariant,
                            primary = MiuixTheme.colorScheme.primary,
                            selected = gitHubAppCandidateColors(selected = true, isDark = false),
                            unselected = gitHubAppCandidateColors(selected = false, isDark = false),
                        )
                    SideEffect { lightCapture = capture }
                }
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Dark)) {
                    val capture =
                        CandidateThemeCapture(
                            surfaceContainer = MiuixTheme.colorScheme.surfaceContainer,
                            onBackgroundVariant = MiuixTheme.colorScheme.onBackgroundVariant,
                            primary = MiuixTheme.colorScheme.primary,
                            selected = gitHubAppCandidateColors(selected = true, isDark = true),
                            unselected = gitHubAppCandidateColors(selected = false, isDark = true),
                        )
                    SideEffect { darkCapture = capture }
                }
            }
        }

        composeRule.runOnIdle {
            assertEquals(
                GitHubStatusPalette.Update.copy(alpha = 0.11f),
                lightCapture.selected.containerColor,
            )
            assertEquals(
                GitHubStatusPalette.Update.copy(alpha = 0.20f),
                darkCapture.selected.containerColor,
            )
            listOf(lightCapture, darkCapture).forEach { capture ->
                assertEquals(
                    GitHubStatusPalette.Update.copy(alpha = 0.3f),
                    capture.selected.borderColor,
                )
                assertEquals(GitHubStatusPalette.Update, capture.selected.titleColor)
                assertEquals(
                    capture.surfaceContainer.copy(alpha = 0.64f),
                    capture.unselected.containerColor,
                )
                assertEquals(
                    capture.onBackgroundVariant.copy(alpha = 0.12f),
                    capture.unselected.borderColor,
                )
                assertEquals(capture.primary, capture.unselected.titleColor)
            }
        }
    }

    @Test
    fun candidateListDeclaresASelectableGroup() {
        val source = sourceFile(GITHUB_TRACK_APP_PICKER_CONTENT_SOURCE)

        assertTrue(".selectableGroup()" in source)
    }

    private companion object {
        val candidate =
            InstalledAppItem(
                label = "KeiOS",
                packageName = "os.kei",
            )
        val longCandidate =
            InstalledAppItem(
                label = "A deliberately long localized application name for compact layout",
                packageName = "os.kei.a.deliberately.long.application.package.name",
                installSourceLabel = "A deliberately long application store source",
            )
    }
}

private data class CandidateThemeCapture(
    val surfaceContainer: Color,
    val onBackgroundVariant: Color,
    val primary: Color,
    val selected: GitHubAppCandidateColors,
    val unselected: GitHubAppCandidateColors,
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

private const val GITHUB_TRACK_APP_PICKER_CONTENT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubTrackAppPickerContent.kt"

class GitHubAppCandidateRowTestApp : Application()
