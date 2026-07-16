package os.kei.ui.page.main.widget.sheet

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.glass.AppLiquidWindowBoundary
import os.kei.ui.page.main.widget.glass.LocalLiquidDialogBackdrop
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import os.kei.ui.page.main.widget.glass.preferredLiquidBackdrop
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WindowBackdropBoundaryContractTest {
    @Test
    fun boundaryClearsInheritedWindowBackdropsAndExplicitFallbacks() {
        val boundary = windowBoundarySource(WINDOW_BOUNDARY_SOURCE)
        val resolver = windowBoundarySource(GLASS_RUNTIME_SOURCE)

        assertTrue("LocalLiquidBackdropWindowBoundary provides true" in boundary)
        assertTrue("LocalSceneBackdrop provides emptyBackdrop()" in boundary)
        assertTrue("LocalLiquidParentBackdrop provides null" in boundary)
        assertTrue("LocalLiquidParentBackdropOverridesFallback provides false" in boundary)
        assertTrue("LocalLiquidDialogBackdrop provides null" in boundary)
        assertTrue("fun AppLiquidWindowBoundary(content: @Composable () -> Unit)" in boundary)
        assertTrue("LiquidBackdropWindowBoundary(content = content)" in boundary)
        assertTrue(
            "LocalLiquidParentBackdrop.current ?: LocalLiquidDialogBackdrop.current" in resolver,
            "A window can consume a parent or dialog backdrop created inside its own boundary",
        )
        assertTrue("LiquidBackdropWindowBoundary {\n        Dialog(" in boundary)
        assertTrue("LiquidBackdropWindowBoundary {\n        Popup(" in boundary)
        assertFalse("rememberLayerBackdrop" in boundary)
        assertFalse(".layerBackdrop(" in boundary)
        assertFalse(".drawBackdrop(" in boundary)
    }

    @Test
    fun sheetUsesFallbackSurfaceInsideDialogWindow() {
        val host = windowBoundarySource(DETENT_WINDOW_SHEET_SOURCE)
        val sheet = windowBoundarySource(LIQUID_SHEET_SOURCE)

        assertTrue("LiquidBackdropWindowDialog(" in host)
        assertTrue(host.indexOf("LiquidBackdropWindowDialog(") < host.indexOf("hostContent()"))
        assertTrue("surfaceModifier = Modifier," in sheet)
        assertTrue("?: liquidSheetSurfaceColor(" in sheet)
        assertFalse("LocalSceneBackdrop.current" in sheet)
        assertFalse("rememberLayerBackdrop" in sheet)
        assertFalse(".drawBackdrop(" in sheet)
        assertFalse("LocalLiquidParentBackdrop provides" in sheet)
    }

    @Test
    fun dialogCreatesOnlyAWindowLocalExportWithoutSelfConsumption() {
        val dialog = windowBoundarySource(LIQUID_DIALOG_SOURCE)
        val boundaryIndex = dialog.indexOf("LiquidBackdropWindowDialog(").windowMarkerFound()
        val producerIndex = dialog.indexOf("val dialogBackdrop = rememberLayerBackdrop()").windowMarkerFound()

        assertTrue(boundaryIndex < producerIndex)
        assertTrue("LocalSceneBackdrop.current" in dialog)
        assertTrue("exportedBackdrop = dialogBackdrop," in dialog)
        assertTrue("LocalLiquidDialogBackdrop provides if (liquidControlsEnabled) dialogBackdrop else null" in dialog)
        assertFalse(".layerBackdrop(dialogBackdrop)" in dialog)
    }

    @Test
    fun popupInstallsBoundaryBeforeComposingCallerContent() {
        val popup = windowBoundarySource(SNAPSHOT_POPUP_SOURCE)
        val popupIndex = popup.indexOf("        LiquidBackdropWindowPopup(").windowMarkerFound()
        val contentIndex = popup.indexOf("                content()", popupIndex).windowMarkerFound()

        assertTrue(popupIndex < contentIndex)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = WindowBackdropBoundaryContractTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class WindowBackdropBoundaryRuntimeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resolverRejectsCapturedBackdropAndAcceptsWindowLocalProviders() {
        var explicitOnly: Backdrop? = null
        var expectedParent: Backdrop? = null
        var expectedDialog: Backdrop? = null
        var resolvedParent: Backdrop? = null
        var resolvedDialog: Backdrop? = null

        composeRule.setContent {
            val capturedPageBackdrop = rememberLayerBackdrop()
            val windowParentBackdrop = rememberLayerBackdrop()
            val windowDialogBackdrop = rememberLayerBackdrop()

            AppLiquidWindowBoundary {
                val explicitResolution = preferredLiquidBackdrop(capturedPageBackdrop)
                SideEffect {
                    explicitOnly = explicitResolution
                    expectedParent = windowParentBackdrop
                    expectedDialog = windowDialogBackdrop
                }
                CompositionLocalProvider(LocalLiquidParentBackdrop provides windowParentBackdrop) {
                    val resolution = preferredLiquidBackdrop(capturedPageBackdrop)
                    SideEffect { resolvedParent = resolution }
                }
                CompositionLocalProvider(LocalLiquidDialogBackdrop provides windowDialogBackdrop) {
                    val resolution = preferredLiquidBackdrop(capturedPageBackdrop)
                    SideEffect { resolvedDialog = resolution }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertNull(explicitOnly)
            assertSame(expectedParent, resolvedParent)
            assertSame(expectedDialog, resolvedDialog)
        }
    }
}

class WindowBackdropBoundaryContractTestApp : Application()

private fun Int.windowMarkerFound(): Int {
    require(this >= 0) { "Expected source marker was not found" }
    return this
}

private fun windowBoundarySource(relativePath: String): String {
    val roots = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
    val source = roots.map { File(it, relativePath) }.firstOrNull(File::isFile)
    return requireNotNull(source) {
        "Unable to locate $relativePath from ${System.getProperty("user.dir")}"
    }.readText()
}

private const val WINDOW_BOUNDARY_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/LiquidBackdropWindowBoundary.kt"
private const val GLASS_RUNTIME_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/glass/GlassEffectRuntime.kt"
private const val DETENT_WINDOW_SHEET_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/sheet/LiquidDetentWindowBottomSheet.kt"
private const val LIQUID_SHEET_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/sheet/LiquidGlassBottomSheet.kt"
private const val LIQUID_DIALOG_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/dialog/LiquidGlassDialog.kt"
private const val SNAPSHOT_POPUP_SOURCE =
    "ui-liquid-glass/src/main/java/os/kei/ui/page/main/widget/sheet/MiuixSnapshotAdapters.kt"
