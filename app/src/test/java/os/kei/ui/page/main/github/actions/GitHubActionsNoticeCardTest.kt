package os.kei.ui.page.main.github.actions

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.github.GitHubStatusPalette
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = GitHubActionsNoticeCardTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class GitHubActionsNoticeCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bodyTypographyPaddingAndLongTextStayComplete() {
        val longText =
            "This complete GitHub Actions status message wraps across several lines without truncation or ellipsis."

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                Column {
                    GitHubActionsNoticeCard(
                        text = "Single line notice",
                        accent = Color.Gray,
                        isDark = false,
                        modifier = Modifier.width(180.dp).testTag("single-line-notice"),
                    )
                    GitHubActionsNoticeCard(
                        text = longText,
                        accent = Color.Gray,
                        isDark = false,
                        modifier = Modifier.width(180.dp).testTag("long-notice"),
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag("single-line-notice")
            .assertWidthIsEqualTo(180.dp)
            .assertHeightIsEqualTo(40.33.dp)
        composeRule.onNodeWithText(longText).assertExists()
        val longHeight =
            with(composeRule.density) {
                composeRule.onNodeWithTag("long-notice").fetchSemanticsNode().boundsInRoot.height.toDp()
            }
        assertTrue(longHeight > 40.33.dp)
    }

    @Test
    fun parentBackdropAndStandaloneFallbackKeepTheSameNoticeGeometry() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                Column {
                    CompositionLocalProvider(LocalLiquidParentBackdrop provides null) {
                        GitHubActionsNoticeCard(
                            text = "Fallback notice",
                            accent = Color.Gray,
                            isDark = false,
                            modifier = Modifier.width(180.dp).testTag("fallback-notice"),
                        )
                    }
                    val backdrop = rememberLayerBackdrop()
                    Box(modifier = Modifier.width(180.dp)) {
                        Box(
                            modifier =
                                Modifier
                                    .matchParentSize()
                                    .background(Color.White)
                                    .layerBackdrop(backdrop),
                        )
                        CompositionLocalProvider(LocalLiquidParentBackdrop provides backdrop) {
                            GitHubActionsNoticeCard(
                                text = "Parent material notice",
                                accent = Color.Gray,
                                isDark = false,
                                modifier = Modifier.testTag("parent-material-notice"),
                            )
                        }
                    }
                }
            }
        }

        composeRule
            .onNodeWithTag("fallback-notice")
            .assertWidthIsEqualTo(180.dp)
            .assertHeightIsEqualTo(40.33.dp)
        composeRule
            .onNodeWithTag("parent-material-notice")
            .assertWidthIsEqualTo(180.dp)
            .assertHeightIsEqualTo(40.33.dp)
        composeRule.onNodeWithText("Fallback notice").assertExists()
        composeRule.onNodeWithText("Parent material notice").assertExists()
    }

    @Test
    fun lightAndDarkNoticeColorsKeepNeutralAndErrorReadability() {
        lateinit var lightNeutral: NoticeThemeCapture
        lateinit var lightError: GitHubActionsNoticeColors
        lateinit var darkNeutral: NoticeThemeCapture
        lateinit var darkError: GitHubActionsNoticeColors

        composeRule.setContent {
            Column {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    val neutral = githubActionsNoticeColors(accent = Color.Gray, isDark = false)
                    val error = githubActionsNoticeColors(accent = GitHubStatusPalette.Error, isDark = false)
                    val capture =
                        NoticeThemeCapture(
                            colors = neutral,
                            onBackground = MiuixTheme.colorScheme.onBackground,
                            onBackgroundVariant = MiuixTheme.colorScheme.onBackgroundVariant,
                        )
                    SideEffect {
                        lightNeutral = capture
                        lightError = error
                    }
                    Box(modifier = Modifier.size(1.dp))
                }
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Dark)) {
                    val neutral = githubActionsNoticeColors(accent = Color.Gray, isDark = true)
                    val error = githubActionsNoticeColors(accent = GitHubStatusPalette.Error, isDark = true)
                    val capture =
                        NoticeThemeCapture(
                            colors = neutral,
                            onBackground = MiuixTheme.colorScheme.onBackground,
                            onBackgroundVariant = MiuixTheme.colorScheme.onBackgroundVariant,
                        )
                    SideEffect {
                        darkNeutral = capture
                        darkError = error
                    }
                    Box(modifier = Modifier.size(1.dp))
                }
            }
        }

        composeRule.runOnIdle {
            assertEquals(lightNeutral.onBackgroundVariant, lightNeutral.colors.accentColor)
            assertEquals(lightNeutral.onBackgroundVariant.copy(alpha = 0.085f), lightNeutral.colors.containerColor)
            assertEquals(lightNeutral.onBackgroundVariant.copy(alpha = 0.14f), lightNeutral.colors.borderColor)
            assertEquals(lightNeutral.onBackground.copy(alpha = 0.70f), lightNeutral.colors.contentColor)

            assertEquals(darkNeutral.onBackgroundVariant, darkNeutral.colors.accentColor)
            assertEquals(darkNeutral.onBackgroundVariant.copy(alpha = 0.15f), darkNeutral.colors.containerColor)
            assertEquals(darkNeutral.onBackgroundVariant.copy(alpha = 0.20f), darkNeutral.colors.borderColor)
            assertEquals(darkNeutral.onBackground.copy(alpha = 0.80f), darkNeutral.colors.contentColor)

            assertEquals(GitHubStatusPalette.Error, lightError.accentColor)
            assertEquals(GitHubStatusPalette.Error.copy(alpha = 0.09f), lightError.containerColor)
            assertEquals(GitHubStatusPalette.Error.copy(alpha = 0.16f), lightError.borderColor)
            assertEquals(GitHubStatusPalette.Error, lightError.contentColor)

            assertEquals(GitHubStatusPalette.Error, darkError.accentColor)
            assertEquals(GitHubStatusPalette.Error.copy(alpha = 0.16f), darkError.containerColor)
            assertEquals(GitHubStatusPalette.Error.copy(alpha = 0.24f), darkError.borderColor)
            assertEquals(GitHubStatusPalette.Error, darkError.contentColor)
        }
    }
}

private data class NoticeThemeCapture(
    val colors: GitHubActionsNoticeColors,
    val onBackground: Color,
    val onBackgroundVariant: Color,
)

class GitHubActionsNoticeCardTestApp : Application()
