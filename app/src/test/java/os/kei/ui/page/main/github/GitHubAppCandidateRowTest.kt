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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
    qualifiers = "w360dp-h800dp-xxhdpi",
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
        val installSourceNode =
            composeRule.onNodeWithText(longCandidate.installSourceLabel, useUnmergedTree = true)
        val installSourceBounds = installSourceNode.fetchSemanticsNode().boundsInRoot

        installSourceNode
            .assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Role))
            .assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
            .assert(!SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
        val radioButton =
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.RadioButton,
            )
        composeRule.onAllNodes(radioButton).assertCountEquals(1)
        composeRule.onAllNodes(radioButton, useUnmergedTree = true).assertCountEquals(1)

        assertTrue(titleBounds.bottom <= packageBounds.top)
        assertTrue(titleBounds.right <= installSourceBounds.left)
        assertTrue(packageBounds.right <= installSourceBounds.left)
        with(composeRule.density) {
            assertTrue(titleBounds.height.toDp() <= 30.dp)
            assertTrue(packageBounds.height.toDp() <= 27.dp)
        }
    }

    @Test
    fun selectedAppCardAtLargeFontKeepsLongTextAndInstallSourceSeparated() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density = density.density, fontScale = 1.5f)
                ) {
                    Column(
                        modifier =
                            Modifier
                                .width(360.dp)
                                .testTag("large-font-selected-app-card"),
                    ) {
                        GitHubSelectedAppCard(
                            selectedApp = longCandidate,
                            showInstallSource = true,
                        )
                    }
                }
            }
        }

        val cardBounds =
            composeRule
                .onNodeWithTag("large-font-selected-app-card")
                .assertWidthIsEqualTo(360.dp)
                .fetchSemanticsNode()
                .boundsInRoot
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
        val installSourceNode =
            composeRule.onNodeWithText(longCandidate.installSourceLabel, useUnmergedTree = true)
        val installSourceBounds = installSourceNode.fetchSemanticsNode().boundsInRoot

        installSourceNode
            .assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Role))
            .assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
            .assert(!SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))

        assertTrue(titleBounds.bottom <= packageBounds.top)
        assertTrue(titleBounds.right <= installSourceBounds.left)
        assertTrue(packageBounds.right <= installSourceBounds.left)
        assertTrue(installSourceBounds.right <= cardBounds.right)
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

    @Test
    fun pickerLoadingAndEmptyStatesUseCompactLiquidInfoBlocks() {
        val source = sourceFile(GITHUB_TRACK_APP_PICKER_CONTENT_SOURCE)
        val transientStates =
            source
                .substringAfter("if (!appFilterReady)")
                .substringBefore("} else {\n                LazyColumn(")

        assertEquals(2, Regex("LiquidInfoBlock\\(").findAll(transientStates).count())
        assertEquals(
            2,
            Regex("density = LiquidInfoBlockDensity\\.Compact").findAll(transientStates).count(),
        )
        assertTrue("MiuixInfoItem(" !in transientStates)
    }

    @Test
    fun installSourcePillReusesCompactStatusMaterialFromExportedSheetCards() {
        val source = sourceFile(GITHUB_APP_SELECTION_ROWS_SOURCE)
        val consumers = source.substringBefore("@Composable\ninternal fun InstallSourcePill(")
        val pill =
            source
                .substringAfter("internal fun InstallSourcePill(")
                .substringBefore("@Composable\ninternal fun AppIconImage(")
        val sheetStyles = sourceFile(SHEET_STYLES_SOURCE)
        val surfaceCard =
            sheetStyles
                .substringAfter("fun SheetSurfaceCard(")
                .substringBefore("@Composable\nfun SheetSectionCard(")
        val choiceCard =
            sheetStyles
                .substringAfter("fun SheetChoiceCard(")
                .substringBefore("@Composable\nfun SheetLiquidChoiceIndicator(")

        assertEquals(2, consumers.occurrencesOf("InstallSourcePill("))
        assertEquals(1, pill.occurrencesOf("StatusPill("))
        assertTrue("size = AppStatusPillSize.Compact" in pill)
        assertTrue("modifier = modifier.widthIn(max = 156.dp)" in pill)
        assertTrue("maxLines = 1" in pill)
        assertTrue("overflow = TextOverflow.Ellipsis" in pill)
        assertTrue("selected: Boolean = false" in pill)
        assertTrue("GitHubStatusPalette.Update" in pill)
        assertTrue("MiuixTheme.colorScheme.primary" in pill)
        assertTrue("modifier: Modifier = Modifier" in pill)
        assertTrue("backdrop =" !in pill)
        assertTrue("Box(" !in pill)
        assertTrue("appSquircleBackground" !in pill)
        assertTrue("appSquircleBorder" !in pill)
        assertTrue("rememberAppStatusPillMetrics" !in pill)

        assertTrue("exportBackdropToContent = true" in surfaceCard)
        assertTrue("SheetSurfaceCard(" in choiceCard)
        assertTrue("trailing?.invoke(this)" in choiceCard)
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

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { it == needle }

private const val GITHUB_TRACK_APP_PICKER_CONTENT_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubTrackAppPickerContent.kt"
private const val GITHUB_APP_SELECTION_ROWS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/GitHubAppSelectionRows.kt"
private const val SHEET_STYLES_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/sheet/SheetStyles.kt"

class GitHubAppCandidateRowTestApp : Application()
