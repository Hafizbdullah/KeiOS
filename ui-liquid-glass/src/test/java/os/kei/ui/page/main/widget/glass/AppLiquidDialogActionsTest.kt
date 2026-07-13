package os.kei.ui.page.main.widget.glass

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = AppLiquidDialogActionsTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class AppLiquidDialogActionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun standaloneBackdropHostCreatesLocalBackdropWithoutParentBackdrop() {
        var observedBackdrop: Backdrop? = null

        composeRule.setContent {
            AppStandaloneBackdropHost(modifier = Modifier) { backdrop ->
                observedBackdrop = backdrop
                Box(modifier = Modifier.testTag("standalone-content"))
            }
        }

        composeRule.onNodeWithTag("standalone-content").assertExists()
        composeRule.runOnIdle { assertNotNull(observedBackdrop) }
    }

    @Test
    fun standaloneBackdropHostPassesNullWhenLiquidEffectsAreDisabled() {
        var observedBackdrop: Backdrop? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalLiquidControlsEnabled provides false) {
                AppStandaloneBackdropHost(modifier = Modifier) { backdrop ->
                    observedBackdrop = backdrop
                    Box(modifier = Modifier.testTag("standalone-fallback-content"))
                }
            }
        }

        composeRule.onNodeWithTag("standalone-fallback-content").assertExists()
        composeRule.runOnIdle { assertNull(observedBackdrop) }
    }

    @Test
    fun fallbackAndBackdropActionsApplyTagToClickableButton() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                Column(
                    modifier =
                        Modifier
                            .background(Color.White)
                            .layerBackdrop(backdrop),
                ) {
                    AppLiquidDialogActionButton(
                        text = "Fallback action",
                        onClick = {},
                        modifier = Modifier.testTag("fallback-action"),
                    )
                    CompositionLocalProvider(LocalLiquidDialogBackdrop provides backdrop) {
                        AppLiquidDialogActionButton(
                            text = "Backdrop action",
                            onClick = {},
                            modifier = Modifier.testTag("backdrop-action"),
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("fallback-action").assertHasClickAction()
        composeRule.onNodeWithTag("backdrop-action").assertHasClickAction()
    }
}

class AppLiquidDialogActionsTestApp : Application()
