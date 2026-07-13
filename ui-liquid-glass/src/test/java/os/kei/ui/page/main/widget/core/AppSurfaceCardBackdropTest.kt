package os.kei.ui.page.main.widget.core

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import os.kei.ui.page.main.widget.glass.AppStandaloneBackdropHost
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = AppSurfaceCardBackdropTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class AppSurfaceCardBackdropTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun surfaceCardExportsIndependentBackdropToContent() {
        var sceneBackdrop: Backdrop? = null
        var contentBackdrop: Backdrop? = null

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                sceneBackdrop = backdrop
                Box(modifier = Modifier.size(220.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .background(Color.White)
                                .layerBackdrop(backdrop),
                    )
                    AppSurfaceCard(
                        backdrop = backdrop,
                        exportBackdropToContent = true,
                    ) {
                        contentBackdrop = LocalLiquidParentBackdrop.current
                        Box(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .testTag("surface-card-content"),
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("surface-card-content").assertExists()
        composeRule.runOnIdle {
            assertNotNull(sceneBackdrop)
            assertNotNull(contentBackdrop)
            assertNotSame(sceneBackdrop, contentBackdrop)
        }
    }

    @Test
    fun surfaceCardWithoutParentStillExportsBackdropToContent() {
        var contentBackdrop: Backdrop? = null

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AppSurfaceCard(exportBackdropToContent = true) {
                    contentBackdrop = LocalLiquidParentBackdrop.current
                    Box(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .testTag("fallback-export-content"),
                    )
                }
            }
        }

        composeRule.onNodeWithTag("fallback-export-content").assertExists()
        composeRule.runOnIdle { assertNotNull(contentBackdrop) }
    }

    @Test
    fun featureCardForwardsExportedBackdropToBody() {
        var sceneBackdrop: Backdrop? = null
        var contentBackdrop: Backdrop? = null

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                sceneBackdrop = backdrop
                Box(modifier = Modifier.size(260.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .background(Color.White)
                                .layerBackdrop(backdrop),
                    )
                    AppFeatureCard(
                        title = "Backdrop",
                        subtitle = "Export",
                        backdrop = backdrop,
                        exportBackdropToContent = true,
                    ) {
                        contentBackdrop = LocalLiquidParentBackdrop.current
                        Box(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .testTag("feature-card-content"),
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("feature-card-content").assertExists()
        composeRule.runOnIdle {
            assertNotNull(sceneBackdrop)
            assertNotNull(contentBackdrop)
            assertNotSame(sceneBackdrop, contentBackdrop)
        }
    }

    @Test
    fun exportDisabledKeepsInheritedBackdropVisibleToContent() {
        var expectedBackdrop: Backdrop? = null
        var observedBackdrop: Backdrop? = null

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                expectedBackdrop = backdrop
                Box(modifier = Modifier.size(220.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .background(Color.White)
                                .layerBackdrop(backdrop),
                    )
                    CompositionLocalProvider(LocalLiquidParentBackdrop provides backdrop) {
                        AppSurfaceCard(exportBackdropToContent = false) {
                            observedBackdrop = LocalLiquidParentBackdrop.current
                            Box(
                                modifier =
                                    Modifier
                                        .size(24.dp)
                                        .testTag("inherited-card-content"),
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag("inherited-card-content").assertExists()
        composeRule.runOnIdle {
            assertNotNull(expectedBackdrop)
            assertSame(expectedBackdrop, observedBackdrop)
        }
    }

    @Test
    fun disabledLiquidEffectsKeepDescendantControlsOnFallback() {
        var observedBackdrop: Backdrop? = null

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalLiquidControlsEnabled provides false) {
                    AppSurfaceCard(exportBackdropToContent = true) {
                        AppStandaloneBackdropHost(modifier = Modifier) { activeBackdrop ->
                            observedBackdrop = activeBackdrop
                            Box(modifier = Modifier.testTag("disabled-effects-content"))
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag("disabled-effects-content").assertExists()
        composeRule.runOnIdle { assertNull(observedBackdrop) }
    }

    @Test
    fun nestedCardsExportIndependentBackdrops() {
        var sceneBackdrop: Backdrop? = null
        var outerBackdrop: Backdrop? = null
        var innerBackdrop: Backdrop? = null

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                sceneBackdrop = backdrop
                Box(modifier = Modifier.size(260.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .background(Color.White)
                                .layerBackdrop(backdrop),
                    )
                    AppSurfaceCard(
                        backdrop = backdrop,
                        exportBackdropToContent = true,
                    ) {
                        outerBackdrop = LocalLiquidParentBackdrop.current
                        AppSurfaceCard(exportBackdropToContent = true) {
                            innerBackdrop = LocalLiquidParentBackdrop.current
                            Box(
                                modifier =
                                    Modifier
                                        .size(24.dp)
                                        .testTag("nested-card-content"),
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithTag("nested-card-content").assertExists()
        composeRule.runOnIdle {
            assertNotNull(sceneBackdrop)
            assertNotNull(outerBackdrop)
            assertNotNull(innerBackdrop)
            assertNotSame(sceneBackdrop, outerBackdrop)
            assertNotSame(outerBackdrop, innerBackdrop)
        }
    }
}

class AppSurfaceCardBackdropTestApp : Application()
