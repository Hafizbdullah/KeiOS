package os.kei.ui.page.main.widget.core

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import os.kei.ui.page.main.widget.support.LocalTextCopyExpandedOverride
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@RunWith(AndroidJUnit4::class)
@Config(
    application = AppInfoRowTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class AppInfoRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun optionalLeadingContentIsRenderedInsideTheLabel() {
        setInfoRow {
            AppInfoRow(
                label = "Runtime",
                value = "Android",
                labelLeadingContent = {
                    Box(
                        modifier =
                            Modifier
                                .size(16.dp)
                                .testTag(LEADING_TAG),
                    )
                },
            )
        }

        val leadingBounds =
            composeRule
                .onNodeWithTag(LEADING_TAG, useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot

        assertTrue(leadingBounds.width > 0f && leadingBounds.height > 0f)
        composeRule.onNodeWithText("Runtime", useUnmergedTree = true).fetchSemanticsNode()
        composeRule.onNodeWithText("Android", useUnmergedTree = true).fetchSemanticsNode()
    }

    @Test
    fun defaultLabelWidthRemainsContentDrivenWhenMaximumIsProvided() {
        setInfoRow {
            Box(modifier = Modifier.width(280.dp)) {
                AppInfoRow(
                    label = "ID",
                    value = "150113",
                    labelMinWidth = 70.dp,
                    labelMaxWidth = 123.dp,
                )
            }
        }

        val labelBounds =
            composeRule
                .onNodeWithText("ID", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        val valueBounds =
            composeRule
                .onNodeWithText("150113", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        val maximumExpectedWidth = with(composeRule.density) { 71.dp.toPx() }
        val maximumExpectedValueStart = with(composeRule.density) { 81.dp.toPx() }

        assertTrue(
            labelBounds.width <= maximumExpectedWidth,
            "Short labels must keep the 70dp content-driven minimum instead of expanding to the maximum: $labelBounds",
        )
        assertTrue(
            valueBounds.left <= maximumExpectedValueStart,
            "The value must start after the 70dp label and 10dp gap instead of a fixed maximum-width label: $valueBounds",
        )
    }

    @Test
    fun defaultCopyActionExposesLongClickSemantics() {
        setInfoRow {
            AppInfoRow(label = "Build", value = "debug")
        }

        val semantics = composeRule.onNodeWithText("Build").fetchSemanticsNode().config

        assertTrue(semantics.contains(SemanticsActions.OnLongClick))
    }

    @Test
    fun disabledDefaultCopyActionRemovesLongClickSemantics() {
        setInfoRow {
            AppInfoRow(
                label = "Build",
                value = "debug",
                enableLongPressCopy = false,
            )
        }

        val semantics = composeRule.onNodeWithText("Build").fetchSemanticsNode().config

        assertFalse(semantics.contains(SemanticsActions.OnLongClick))
    }

    private fun setInfoRow(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            CompositionLocalProvider(LocalTextCopyExpandedOverride provides false) {
                MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                    content()
                }
            }
        }
    }
}

class AppInfoRowTestApp : Application()

private const val LEADING_TAG = "app-info-row-leading"
