package os.kei.ui.page.main.widget.status

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.core.AppSupportingBlock
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = StatusPrimitiveBackdropTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class StatusPrimitiveBackdropTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun standaloneNullParentKeepsRenderingAndClickSemantics() {
        var supportingBlockClicks = 0

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(
                    LocalLiquidControlsEnabled provides true,
                    LocalLiquidParentBackdrop provides null,
                ) {
                    Column {
                        StatusPill(
                            label = "Ready",
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.testTag("standalone-status-pill"),
                        )
                        AppSupportingBlock(
                            text = "Open supporting details",
                            modifier = Modifier.testTag("standalone-supporting-block"),
                            onClick = { supportingBlockClicks++ },
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("standalone-status-pill").assertExists().assertHeightIsAtLeast(20.dp)
        composeRule.onNodeWithText("Ready").assertExists()
        composeRule
            .onNodeWithTag("standalone-supporting-block")
            .assertExists()
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText("Open supporting details").assertExists()
        composeRule.runOnIdle {
            assertTrue(supportingBlockClicks == 1)
        }
    }

    @Test
    fun inheritedParentBackdropKeepsBothPrimitivesRendered() {
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val parentBackdrop = rememberLayerBackdrop()
                CompositionLocalProvider(
                    LocalLiquidControlsEnabled provides true,
                    LocalLiquidParentBackdrop provides parentBackdrop,
                ) {
                    Column {
                        StatusPill(
                            label = "Inherited",
                            color = Color(0xFF22C55E),
                            modifier = Modifier.testTag("inherited-status-pill"),
                        )
                        AppSupportingBlock(
                            text = "Inherited supporting material",
                            modifier = Modifier.testTag("inherited-supporting-block"),
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("inherited-status-pill").assertExists()
        composeRule.onNodeWithText("Inherited").assertExists()
        composeRule.onNodeWithTag("inherited-supporting-block").assertExists()
        composeRule.onNodeWithText("Inherited supporting material").assertExists()
    }

    @Test
    fun primitivesContainNoStandaloneLayerBackdropProducer() {
        val statusPillSource = sourceFile(STATUS_PILL_SOURCE)
        val supportingBlockSource = sourceFile(APP_STATUS_PRIMITIVES_SOURCE)

        listOf(statusPillSource, supportingBlockSource).forEach { source ->
            assertFalse("rememberLayerBackdrop" in source)
            assertFalse(".layerBackdrop(" in source)
        }
        assertTrue("activeGlassBackdrop(backdrop ?: parentBackdrop)" in statusPillSource)
        assertTrue("if (activeBackdrop != null)" in statusPillSource)
        assertTrue(".drawAppSquircleBackground(cornerRadius)" in statusPillSource)
        assertTrue("statusPillFallbackOptics(" in statusPillSource)
        assertTrue(".appSquircleClip(cornerRadius)" in statusPillSource)
        assertTrue("fallbackOptics.veilTop" in statusPillSource)
        assertTrue("fallbackOptics.innerShadeBottom" in statusPillSource)
        assertTrue("fallbackOptics.rimColor" in statusPillSource)
        assertTrue("StatusPillLiquid(" in statusPillSource)
        assertTrue("surfaceColor = resolvedColor.copy(alpha = backgroundAlpha)" in statusPillSource)
        assertTrue("contentColorOverride: Color? = null" in statusPillSource)
        assertTrue("contentColorOverride ?:" in statusPillSource)
        assertTrue("typographyOverride: TextStyle? = null" in statusPillSource)
        assertTrue("blurRadiusOverride: Dp? = null" in statusPillSource)
        assertTrue("lensRadiusOverride: Dp? = null" in statusPillSource)
        assertTrue("blurRadius = blurRadiusOverride ?: UiPerformanceBudget.backdropBlur" in statusPillSource)
        assertTrue("lensRadius = lensRadiusOverride ?: UiPerformanceBudget.backdropLens" in statusPillSource)
        assertTrue(
            "activeGlassBackdrop(LocalLiquidParentBackdrop.current)" in supportingBlockSource,
        )
        assertTrue("if (activeBackdrop != null)" in supportingBlockSource)
        assertTrue("Modifier.appSquircleBackground(backgroundColor, 12.dp)" in supportingBlockSource)
    }
}

class StatusPrimitiveBackdropTestApp : Application()

private fun sourceFile(relativePath: String): String {
    val workingDirectory = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
    val sourceFile =
        generateSequence(workingDirectory) { directory -> directory.parentFile }
            .map { directory -> File(directory, relativePath) }
            .firstOrNull(File::isFile)
    return requireNotNull(sourceFile) {
        "Unable to locate $relativePath from $workingDirectory"
    }.readText()
}

private const val STATUS_PILL_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/status/StatusPill.kt"

private const val APP_STATUS_PRIMITIVES_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/core/AppStatusPrimitives.kt"
