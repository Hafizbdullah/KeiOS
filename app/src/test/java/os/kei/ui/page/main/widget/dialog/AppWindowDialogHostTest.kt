package os.kei.ui.page.main.widget.dialog

import android.app.Application
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.ui.page.main.widget.glass.LocalLiquidParentBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = AppWindowDialogHostTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class AppWindowDialogHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fullscreenPresentationUsesWindowHeightAndDispatchesDismissFinishedOnce() {
        val show = mutableStateOf(true)
        var dismissFinishedCount = 0
        var contentObserved = false
        var observedParentBackdrop: Backdrop? = null
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val pageBackdrop = rememberLayerBackdrop()
                CompositionLocalProvider(LocalLiquidParentBackdrop provides pageBackdrop) {
                    AppWindowDialogHost(
                        show = show.value,
                        presentation = AppWindowDialogPresentation.Fullscreen,
                        onDismissFinished = { dismissFinishedCount++ },
                    ) {
                        val parentBackdrop = LocalLiquidParentBackdrop.current
                        SideEffect {
                            contentObserved = true
                            observedParentBackdrop = parentBackdrop
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .testTag("fullscreen-dialog-content"),
                        )
                    }
                }
            }
        }

        composeRule
            .onNodeWithTag("fullscreen-dialog-content")
            .assertExists()
            .assertHeightIsAtLeast(850.dp)
        composeRule.runOnIdle {
            assertTrue(contentObserved)
            assertNull(observedParentBackdrop)
        }

        composeRule.runOnIdle { show.value = false }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("fullscreen-dialog-content").assertDoesNotExist()
        assertEquals(1, dismissFinishedCount)

        composeRule.runOnIdle { show.value = false }
        composeRule.waitForIdle()
        assertEquals(1, dismissFinishedCount)
    }

    @Test
    fun fullscreenContentBackHandlerUsesDialogDispatcherBeforeHostFallback() {
        var contentBackCount = 0
        var hostBackCount = 0
        lateinit var dialogBackDispatcher: OnBackPressedDispatcher
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AppWindowDialogHost(
                    show = true,
                    presentation = AppWindowDialogPresentation.Fullscreen,
                    onDismissRequest = { hostBackCount++ },
                ) {
                    val owner = checkNotNull(LocalOnBackPressedDispatcherOwner.current)
                    SideEffect { dialogBackDispatcher = owner.onBackPressedDispatcher }
                    BackHandler { contentBackCount++ }
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        composeRule.runOnIdle { dialogBackDispatcher.onBackPressed() }

        assertEquals(1, contentBackCount)
        assertEquals(0, hostBackCount)
    }

    @Test
    fun cardBranchesForwardMetadataAndEveryWindowInstallsABackdropBoundary() {
        val source = dialogHostSource(APP_WINDOW_DIALOG_HOST_SOURCE)
        val liquidDialogCall = source.functionCallBlock("LiquidGlassDialog")
        val legacyDialogCall = source.functionCallBlock("WindowDialog")

        listOf(
            "modifier = modifier",
            "title = title",
            "summary = summary",
            "onDismissFinished = onDismissFinished",
            "maxWidth = maxWidth",
            "content = content",
        ).forEach { forwarding ->
            assertTrue(forwarding in liquidDialogCall, "Liquid branch must forward $forwarding")
            assertTrue(forwarding in legacyDialogCall, "Legacy branch must forward $forwarding")
        }
        assertTrue("dismissible = dismissible" in liquidDialogCall)
        assertTrue("onDismissRequest = onDismissRequest.takeIf { dismissible }" in legacyDialogCall)
        assertTrue("maxWidth: Dp = DialogDefaults.MaxWidth" in source)

        val legacyBoundary = source.indexOf("AppLiquidWindowBoundary {").dialogMarkerFound()
        val legacyDialog = source.indexOf("WindowDialog(", legacyBoundary).dialogMarkerFound()
        val fullscreenBoundary =
            source.indexOf("AppLiquidWindowBoundary {", legacyBoundary + 1).dialogMarkerFound()
        val fullscreenDialog = source.indexOf("\n        Dialog(", fullscreenBoundary).dialogMarkerFound()
        assertTrue(legacyBoundary < legacyDialog)
        assertTrue(legacyDialog < fullscreenBoundary)
        assertTrue(fullscreenBoundary < fullscreenDialog)
    }
}

class AppWindowDialogHostTestApp : Application()

private fun String.functionCallBlock(functionName: String): String {
    val marker = "$functionName("
    val start = indexOf(marker).dialogMarkerFound()
    var depth = 1
    var index = start + marker.length
    while (index < length && depth > 0) {
        when (this[index]) {
            '(' -> depth += 1
            ')' -> depth -= 1
        }
        index += 1
    }
    require(depth == 0) { "Unable to locate the closing parenthesis for $marker" }
    return substring(start, index)
}

private fun Int.dialogMarkerFound(): Int {
    require(this >= 0) { "Expected dialog host source marker was not found" }
    return this
}

private fun dialogHostSource(relativePath: String): String {
    val roots = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
    val source = roots.map { File(it, relativePath) }.firstOrNull(File::isFile)
    return requireNotNull(source) {
        "Unable to locate $relativePath from ${System.getProperty("user.dir")}"
    }.readText()
}

private const val APP_WINDOW_DIALOG_HOST_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/widget/dialog/AppWindowDialogHosts.kt"
