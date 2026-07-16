package os.kei.ui.page.main.widget.chrome

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.core.prefs.NonHomeBackgroundContentScale
import os.kei.ui.page.main.settings.support.SettingsGroupCard
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdropOverridesFallback
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = AppManagedBackgroundLiquidBackdropTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class AppManagedBackgroundLiquidBackdropTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inactiveBackgroundOptInProvidesPageMaterialWithoutForcingChildOverride() {
        var pageBackdrop: Backdrop? = null
        var overridesFallback = true

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                testManagedBackgroundHost(exportBackdropToContent = true) {
                    val observedBackdrop = LocalLiquidParentBackdrop.current
                    val observedOverride = LocalLiquidParentBackdropOverridesFallback.current
                    SideEffect {
                        pageBackdrop = observedBackdrop
                        overridesFallback = observedOverride
                    }
                }
            }
        }

        composeRule.runOnIdle {
            assertNotNull(pageBackdrop)
            assertFalse(overridesFallback)
        }
    }

    @Test
    fun optOutPreservesInheritedMaterialContract() {
        var inheritedBackdrop: Backdrop? = null
        var observedBackdrop: Backdrop? = null
        var observedOverride = false

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                inheritedBackdrop = backdrop
                CompositionLocalProvider(
                    LocalLiquidParentBackdrop provides backdrop,
                    LocalLiquidParentBackdropOverridesFallback provides true,
                ) {
                    testManagedBackgroundHost(exportBackdropToContent = false) {
                        val contentBackdrop = LocalLiquidParentBackdrop.current
                        val contentOverride = LocalLiquidParentBackdropOverridesFallback.current
                        SideEffect {
                            observedBackdrop = contentBackdrop
                            observedOverride = contentOverride
                        }
                    }
                }
            }
        }

        composeRule.runOnIdle {
            assertNotNull(inheritedBackdrop)
            assertSame(inheritedBackdrop, observedBackdrop)
            assertTrue(observedOverride)
        }
    }

    @Test
    fun settingsCardConsumesPageMaterialAndExportsIndependentChildMaterial() {
        var pageBackdrop: Backdrop? = null
        var cardBackdrop: Backdrop? = null
        var cardOverridesFallback = false

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                testManagedBackgroundHost(exportBackdropToContent = true) {
                    val observedPageBackdrop = LocalLiquidParentBackdrop.current
                    SideEffect { pageBackdrop = observedPageBackdrop }
                    SettingsGroupCard(
                        header = "Settings",
                        title = "Controls",
                        containerColor = Color.White.copy(alpha = 0.64f),
                        exportBackdropToContent = true,
                    ) {
                        val observedCardBackdrop = LocalLiquidParentBackdrop.current
                        val observedOverride = LocalLiquidParentBackdropOverridesFallback.current
                        SideEffect {
                            cardBackdrop = observedCardBackdrop
                            cardOverridesFallback = observedOverride
                        }
                        Box(Modifier.size(24.dp))
                    }
                }
            }
        }

        composeRule.runOnIdle {
            assertNotNull(pageBackdrop)
            assertNotNull(cardBackdrop)
            assertNotSame(pageBackdrop, cardBackdrop)
            assertTrue(cardOverridesFallback)
        }
    }
}

@Composable
private fun testManagedBackgroundHost(
    exportBackdropToContent: Boolean,
    content: @Composable () -> Unit,
) {
    AppManagedBackgroundHost(
        enabled = false,
        imageUri = "",
        opacity = 1f,
        contentScale = NonHomeBackgroundContentScale.Crop,
        scrim = 0f,
        exportBackdropToContent = exportBackdropToContent,
        content = content,
    )
}

class AppManagedBackgroundLiquidBackdropTestApp : Application()
