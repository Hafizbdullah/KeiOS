package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.liquidglass.R
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = LiquidToastAccessibilityTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class LiquidToastAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setHost(state: LiquidToastState) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .layerBackdrop(backdrop),
                ) {
                    LiquidToastHost(state = state, backdrop = backdrop)
                }
            }
        }
    }

    private fun stepClock(
        totalMs: Long,
        sliceMs: Long = 100,
    ) {
        var advanced = 0L
        while (advanced < totalMs) {
            composeRule.mainClock.advanceTimeBy(sliceMs)
            composeRule.waitForIdle()
            advanced += sliceMs
        }
    }

    @Test
    fun visibleToastExposesPoliteLiveRegionPaneTitleAndOneMessageLabel() {
        val state = LiquidToastState()
        setHost(state)
        state.show("Saved")
        stepClock(600)

        val context = RuntimeEnvironment.getApplication()
        val toastNode = composeRule.onNodeWithContentDescription("Saved")
        toastNode
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    context.getString(R.string.liquid_toast_pane_title),
                ),
            ).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeRule
            .onAllNodesWithContentDescription("Saved", useUnmergedTree = true)
            .assertCountEquals(1)
        composeRule
            .onAllNodesWithText("Saved")
            .assertCountEquals(0)

        val dismissAction = toastNode.fetchSemanticsNode().config[SemanticsActions.OnClick]
        assertEquals(context.getString(R.string.liquid_toast_dismiss), dismissAction.label)
    }

    @Test
    fun dismissActionPlaysExitAndReleasesTheToastSlot() {
        val state = LiquidToastState()
        setHost(state)
        state.show("Dismiss me")
        stepClock(600)

        composeRule.onNodeWithContentDescription("Dismiss me").performClick()
        stepClock(600)

        composeRule
            .onAllNodesWithContentDescription("Dismiss me")
            .assertCountEquals(0)
        assertTrue(state.visibleSlots.isEmpty())
    }
}

class LiquidToastAccessibilityTestApp : Application()
