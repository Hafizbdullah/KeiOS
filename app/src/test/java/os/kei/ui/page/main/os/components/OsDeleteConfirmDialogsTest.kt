package os.kei.ui.page.main.os.components

import android.app.Application
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.R
import os.kei.ui.page.main.widget.dialog.LiquidGlassDialog
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = OsDeleteConfirmDialogsTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class OsDeleteConfirmDialogsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deleteDialogPreservesShowCopyAndDualActions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val show = mutableStateOf(false)
        var deleteCount = 0
        var dismissCount = 0
        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTransitionAnimationsEnabled provides false) {
                    LiquidGlassDialog(
                        show = show.value,
                        title = DELETE_TITLE,
                        summary = DELETE_SUMMARY,
                    ) {
                        OsDeleteConfirmDialogActions(
                            onDismissRequest = { dismissCount++ },
                            onConfirmDelete = { deleteCount++ },
                        )
                    }
                }
            }
        }

        composeRule.onAllNodes(hasText(DELETE_TITLE)).assertCountEquals(0)
        composeRule.runOnIdle { show.value = true }
        composeRule.onNode(hasText(DELETE_TITLE) and isHeading()).assertIsDisplayed()
        composeRule.onNode(hasText(DELETE_SUMMARY)).assertIsDisplayed()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)

        composeRule
            .onNode(hasText(context.getString(R.string.common_cancel)) and hasClickAction())
            .performClick()
        composeRule
            .onNode(hasText(context.getString(R.string.common_delete)) and hasClickAction())
            .performClick()
        composeRule.runOnIdle {
            assertEquals(1, dismissCount)
            assertEquals(1, deleteCount)
            show.value = false
        }
        composeRule.onAllNodes(hasText(DELETE_TITLE)).assertCountEquals(0)
    }

    @Test
    fun deleteDialogRoutesThroughTheSharedHostAndKeepsDangerStyling() {
        val source = confirmDialogSource(OS_DELETE_CONFIRM_DIALOG_SOURCE)

        assertEquals(1, source.occurrencesOf("AppWindowDialogHost("))
        assertFalse("WindowDialog(" in source)
        assertEquals(1, source.occurrencesOf("containerColor = MiuixTheme.colorScheme.error"))
        assertEquals(1, source.occurrencesOf("variant = GlassVariant.SheetDangerAction"))
        assertEquals(2, source.occurrencesOf("AppLiquidDialogActionButton("))
        assertEquals(1, source.occurrencesOf("show = show,"))
        assertEquals(1, source.occurrencesOf("title = title,"))
        assertEquals(1, source.occurrencesOf("summary = summary,"))
    }
}

class OsDeleteConfirmDialogsTestApp : Application()

private fun String.occurrencesOf(needle: String): Int =
    windowed(needle.length).count { candidate -> candidate == needle }

private fun confirmDialogSource(relativePath: String): String {
    val roots = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
    val source = roots.map { File(it, relativePath) }.firstOrNull(File::isFile)
    return requireNotNull(source) {
        "Unable to locate $relativePath from ${System.getProperty("user.dir")}"
    }.readText()
}

private const val DELETE_TITLE = "Delete saved item?"
private const val DELETE_SUMMARY = "This operation cannot be undone."
private const val OS_DELETE_CONFIRM_DIALOG_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/os/components/OsDeleteConfirmDialogs.kt"
