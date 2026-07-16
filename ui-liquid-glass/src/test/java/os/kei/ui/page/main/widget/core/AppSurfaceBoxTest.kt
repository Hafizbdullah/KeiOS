package os.kei.ui.page.main.widget.core

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdropOverridesFallback
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = AppSurfaceBoxTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class AppSurfaceBoxTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun boxScopeSurfaceExportsActiveMaterialAndProvidesContentColor() {
        var parentBackdrop: Backdrop? = null
        var contentBackdrop: Backdrop? = null
        var overridesFallback = false
        var observedContentColor = Color.Unspecified
        val expectedContentColor = Color(0xFF2468AC)

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                parentBackdrop = backdrop
                Box(modifier = Modifier.size(220.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .background(Color.White)
                                .layerBackdrop(backdrop),
                    )
                    CompositionLocalProvider(LocalLiquidParentBackdrop provides backdrop) {
                        AppSurfaceBox(
                            contentColor = expectedContentColor,
                            exportBackdropToContent = true,
                        ) {
                            contentBackdrop = LocalLiquidParentBackdrop.current
                            overridesFallback = LocalLiquidParentBackdropOverridesFallback.current
                            observedContentColor = LocalContentColor.current
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp)
                                        .testTag("surface-box-content"),
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag("surface-box-content").assertExists()
        composeRule.runOnIdle {
            assertNotNull(parentBackdrop)
            assertNotNull(contentBackdrop)
            assertNotSame(parentBackdrop, contentBackdrop)
            assertTrue(overridesFallback)
            assertEquals(expectedContentColor, observedContentColor)
        }
    }

    @Test
    fun boxSurfacePreservesClickLongClickAndStateSemantics() {
        var clickCount = 0
        var longClickCount = 0

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AppSurfaceBox(
                    pressSafePadding = 0.dp,
                    onClick = { clickCount++ },
                    onLongClick = { longClickCount++ },
                    stateDescription = "Available",
                ) {
                    BasicText("Surface action")
                }
            }
        }

        composeRule
            .onNodeWithText("Surface action")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Available"))
            .performClick()
            .performTouchInput { longClick() }
        composeRule.runOnIdle {
            assertEquals(1, clickCount)
            assertEquals(1, longClickCount)
        }
    }

    @Test
    fun boxSurfaceForwardsRadioButtonRoleAndSelectionState() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AppSurfaceBox(
                    pressSafePadding = 0.dp,
                    onClick = {},
                    role = Role.RadioButton,
                    selected = true,
                ) {
                    BasicText("Selected surface")
                }
            }
        }

        composeRule
            .onNodeWithText("Selected surface")
            .assertHasClickAction()
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }
}

class AppSurfaceBoxTestApp : Application()
