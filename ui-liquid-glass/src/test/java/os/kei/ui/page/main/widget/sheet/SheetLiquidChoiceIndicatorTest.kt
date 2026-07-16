package os.kei.ui.page.main.widget.sheet

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = SheetLiquidChoiceIndicatorTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class SheetLiquidChoiceIndicatorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun indicatorKeepsCompactVisualInsideMinimumTouchTarget() {
        var clickCount = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                SheetLiquidChoiceIndicator(
                    selected = true,
                    onSelect = { clickCount++ },
                    modifier = Modifier.testTag("choice-indicator"),
                )
            }
        }

        composeRule
            .onNodeWithTag("choice-indicator")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .performClick()
        composeRule.runOnIdle { assertEquals(1, clickCount) }
    }

    @Test
    fun disabledIndicatorExposesDisabledSelectionSemantics() {
        var clickCount = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                SheetLiquidChoiceIndicator(
                    selected = true,
                    enabled = false,
                    onSelect = { clickCount++ },
                    modifier = Modifier.testTag("disabled-choice-indicator"),
                )
            }
        }

        composeRule
            .onNodeWithTag("disabled-choice-indicator")
            .assertIsSelected()
            .assertIsNotEnabled()
            .performClick()
        composeRule.runOnIdle { assertEquals(0, clickCount) }
    }

    @Test
    fun choiceCardUsesOneRadioButtonAndInvokesSelectionOnce() {
        var clickCount = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                SheetChoiceCard(
                    title = "Local network",
                    summary = "Allow devices on the same network",
                    selected = true,
                    onSelect = { clickCount++ },
                )
            }
        }

        val radioButton =
            SemanticsMatcher.expectValue(
                SemanticsProperties.Role,
                Role.RadioButton,
            )
        composeRule.onAllNodes(radioButton).assertCountEquals(1)
        composeRule.onAllNodes(radioButton, useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        composeRule
            .onNode(radioButton)
            .assertIsSelected()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, clickCount) }
    }

    @Test
    fun indicatorUsesOnlyAnEnabledParentBackdrop() {
        var expectedBackdrop: Backdrop? = null
        var resolvedBackdrop: Backdrop? = null
        var disabledBackdrop: Backdrop? = null
        var standaloneBackdrop: Backdrop? = null

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val parentBackdrop = rememberLayerBackdrop()
                expectedBackdrop = parentBackdrop
                CompositionLocalProvider(LocalLiquidParentBackdrop provides parentBackdrop) {
                    resolvedBackdrop = resolvedSheetChoiceIndicatorBackdrop()
                    CompositionLocalProvider(LocalLiquidControlsEnabled provides false) {
                        disabledBackdrop = resolvedSheetChoiceIndicatorBackdrop()
                    }
                }
                standaloneBackdrop = resolvedSheetChoiceIndicatorBackdrop()
            }
        }

        composeRule.runOnIdle {
            assertSame(expectedBackdrop, resolvedBackdrop)
            assertNull(disabledBackdrop)
            assertNull(standaloneBackdrop)
        }
    }
}

class SheetLiquidChoiceIndicatorTestApp : Application()
