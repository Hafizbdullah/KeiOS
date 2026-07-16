package os.kei.ui.page.main.ba

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdropOverridesFallback
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.activeGlassBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = BaLiquidSurfacesBackdropTestApp::class,
    sdk = [35],
)
class BaLiquidSurfacesBackdropTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inheritedPageBackdropExportsCardMaterialToDescendants() {
        var pageBackdrop: Backdrop? = null
        var descendantBackdrop: Backdrop? = null
        var descendantOverridesFallback = false

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                pageBackdrop = backdrop
                BaLiquidCard(backdrop = backdrop) {
                    val observedBackdrop = LocalLiquidParentBackdrop.current
                    val observedOverride = LocalLiquidParentBackdropOverridesFallback.current
                    SideEffect {
                        descendantBackdrop = observedBackdrop
                        descendantOverridesFallback = observedOverride
                    }
                }
            }
        }

        composeRule.runOnIdle {
            assertNotNull(pageBackdrop)
            assertNotNull(descendantBackdrop)
            assertNotSame(pageBackdrop, descendantBackdrop)
            assertTrue(descendantOverridesFallback)
        }
    }

    @Test
    fun nestedPanelConsumesAndReExportsParentCardMaterial() {
        var pageBackdrop: Backdrop? = null
        var cardBackdrop: Backdrop? = null
        var panelBackdrop: Backdrop? = null

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                pageBackdrop = backdrop
                BaLiquidCard(backdrop = backdrop) {
                    val observedCardBackdrop = LocalLiquidParentBackdrop.current
                    SideEffect { cardBackdrop = observedCardBackdrop }
                    BaLiquidPanel(backdrop = backdrop) {
                        val observedPanelBackdrop = LocalLiquidParentBackdrop.current
                        SideEffect { panelBackdrop = observedPanelBackdrop }
                    }
                }
            }
        }

        composeRule.runOnIdle {
            assertNotNull(pageBackdrop)
            assertNotNull(cardBackdrop)
            assertNotNull(panelBackdrop)
            assertNotSame(pageBackdrop, cardBackdrop)
            assertNotSame(cardBackdrop, panelBackdrop)
        }
    }

    @Test
    fun standaloneAndDisabledCardsKeepDescendantsOnTheirOwnFallbacks() {
        var standaloneBackdrop: Backdrop? = null
        var standaloneOverride = true
        var disabledBackdrop: Backdrop? = null
        var disabledOverride = true

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                BaLiquidCard(backdrop = null) {
                    val observedBackdrop = LocalLiquidParentBackdrop.current
                    val observedOverride = LocalLiquidParentBackdropOverridesFallback.current
                    SideEffect {
                        standaloneBackdrop = observedBackdrop
                        standaloneOverride = observedOverride
                    }
                }
                BaLiquidCard(
                    backdrop = rememberLayerBackdrop(),
                    effectsEnabled = false,
                ) {
                    val observedBackdrop = LocalLiquidParentBackdrop.current
                    val observedOverride = LocalLiquidParentBackdropOverridesFallback.current
                    SideEffect {
                        disabledBackdrop = observedBackdrop
                        disabledOverride = observedOverride
                    }
                }
            }
        }

        composeRule.runOnIdle {
            assertNull(standaloneBackdrop)
            assertFalse(standaloneOverride)
            assertNull(disabledBackdrop)
            assertFalse(disabledOverride)
        }
    }

    @Test
    fun runtimeDisabledCardDoesNotPublishAnEmptyMaterial() {
        var parentBackdrop: Backdrop? = null
        var descendantBackdrop: Backdrop? = null
        var descendantActiveBackdrop: Backdrop? = null
        var descendantOverridesFallback = true

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val backdrop = rememberLayerBackdrop()
                parentBackdrop = backdrop
                CompositionLocalProvider(
                    LocalLiquidParentBackdrop provides backdrop,
                    LocalLiquidControlsEnabled provides false,
                ) {
                    BaLiquidCard(backdrop = null) {
                        val observedBackdrop = LocalLiquidParentBackdrop.current
                        val observedActiveBackdrop = activeGlassBackdrop(observedBackdrop)
                        val observedOverride = LocalLiquidParentBackdropOverridesFallback.current
                        SideEffect {
                            descendantBackdrop = observedBackdrop
                            descendantActiveBackdrop = observedActiveBackdrop
                            descendantOverridesFallback = observedOverride
                        }
                    }
                }
            }
        }

        composeRule.runOnIdle {
            assertNotNull(parentBackdrop)
            assertSame(parentBackdrop, descendantBackdrop)
            assertNull(descendantActiveBackdrop)
            assertFalse(descendantOverridesFallback)
        }
    }

    @Test
    fun surfaceMaterialsFollowTheAppTheme() {
        val source = baLiquidSurfacesSource()

        assertFalse("isSystemInDarkTheme" in source)
        assertEquals(1, source.occurrencesOf("isAppInDarkTheme()"))
        assertEquals(1, source.occurrencesOf("AppSurfaceBox("))
        assertTrue("activeGlassBackdrop(inheritedBackdrop)" in source)
        assertFalse("LiquidSurface(" in source)
        assertFalse("rememberLayerBackdrop" in source)
        assertFalse("CompositionLocalProvider(" in source)
        assertFalse(".layerBackdrop(" in source)
        assertFalse("localBackdrop" in source)
    }
}

class BaLiquidSurfacesBackdropTestApp : Application()

private fun baLiquidSurfacesSource(): String {
    val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    val sourceFile =
        generateSequence(workingDirectory) { directory -> directory.parentFile }
            .map { directory -> File(directory, BA_LIQUID_SURFACES_SOURCE) }
            .firstOrNull(File::isFile)
    return requireNotNull(sourceFile) {
        "Unable to locate $BA_LIQUID_SURFACES_SOURCE from $workingDirectory"
    }.readText()
}

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count { it == needle }

private const val BA_LIQUID_SURFACES_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/ba/BaLiquidSurfaces.kt"
