package os.kei.ui.page.main.widget.dialog

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = LiquidGlassDialogTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class LiquidGlassDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun customWidthKeepsAccessibleTitleSummaryAndScrimTraversal() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTransitionAnimationsEnabled provides false) {
                    LiquidGlassDialog(
                        show = true,
                        modifier = Modifier.testTag("liquid-dialog-card"),
                        title = "Confirm action",
                        summary = "Review this operation before continuing.",
                        maxWidth = 280.dp,
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp))
                    }
                }
            }
        }

        composeRule.onNode(hasText("Confirm action") and isHeading()).assertIsDisplayed()
        composeRule.onNode(hasText("Review this operation before continuing.")).assertIsDisplayed()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
        val dialogWidth =
            with(composeRule.density) {
                composeRule
                    .onNodeWithTag("liquid-dialog-card")
                    .fetchSemanticsNode()
                    .boundsInRoot.width
                    .toDp()
            }
        assertTrue(dialogWidth <= 280.dp, "Custom maxWidth must constrain the Liquid dialog card")
    }

    @Test
    fun dialogWindowClearsInheritedParentBackdrop() {
        var contentObserved = false
        var observedParentBackdrop: Backdrop? = null
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val pageBackdrop = rememberLayerBackdrop()
                CompositionLocalProvider(
                    LocalLiquidParentBackdrop provides pageBackdrop,
                    LocalTransitionAnimationsEnabled provides false,
                ) {
                    LiquidGlassDialog(
                        show = true,
                        title = "Window boundary",
                    ) {
                        val parentBackdrop = LocalLiquidParentBackdrop.current
                        SideEffect {
                            contentObserved = true
                            observedParentBackdrop = parentBackdrop
                        }
                    }
                }
            }
        }

        composeRule.onNode(hasText("Window boundary")).assertIsDisplayed()
        composeRule.runOnIdle {
            assertTrue(contentObserved)
            assertNull(observedParentBackdrop)
        }
    }

    @Test
    fun disabledTransitionAnimationsRemoveDialogWithoutAnimationDuration() {
        val show = mutableStateOf(true)
        var dismissFinishedCount = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTransitionAnimationsEnabled provides false) {
                    LiquidGlassDialog(
                        show = show.value,
                        title = "Confirm action",
                        onDismissFinished = { dismissFinishedCount++ },
                    )
                }
            }
        }

        composeRule.onNode(hasText("Confirm action")).assertIsDisplayed()
        val clockBeforeDismiss = composeRule.mainClock.currentTime
        composeRule.runOnIdle { show.value = false }
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasText("Confirm action")).assertCountEquals(0)
        assertEquals(1, dismissFinishedCount)
        assertTrue(
            composeRule.mainClock.currentTime - clockBeforeDismiss < 100,
            "Reduced-motion dismissal should finish without consuming the 220ms exit animation",
        )
    }
}

class LiquidGlassDialogTestApp : Application()
