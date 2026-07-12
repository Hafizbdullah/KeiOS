package os.kei.ui.page.main.widget.dialog

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
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
    fun scrimStaysOutOfAccessibilityTraversalAndTitleIsAHeading() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                LiquidGlassDialog(
                    show = true,
                    title = "Confirm action",
                    summary = "Review this operation before continuing.",
                )
            }
        }

        composeRule.onNode(hasText("Confirm action") and isHeading()).assertIsDisplayed()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
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
