@file:Suppress("FunctionName")

package os.kei.ui.page.main.github.sheet

import android.app.Application
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import os.kei.R
import os.kei.feature.github.data.local.GitHubTrackedItemsImportPayload
import os.kei.feature.github.model.GitHubTrackedApp
import os.kei.ui.page.main.github.page.GitHubTrackImportPreview
import os.kei.ui.page.main.widget.dialog.LiquidGlassDialog
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = GitHubTrackDialogsTestApp::class,
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
)
class GitHubTrackDialogsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deleteExitKeepsTrackSummaryAndActionsUntilDismissalFinishes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val item = trackedApp()
        val pendingItem = mutableStateOf<GitHubTrackedApp?>(item)
        var cancelCount = 0
        var confirmCount = 0
        val title = context.getString(R.string.github_delete_dialog_title)
        val summary =
            context.getString(
                R.string.github_delete_dialog_summary,
                item.appLabel,
                item.owner,
                item.repo,
            )
        val cancelLabel = context.getString(R.string.common_cancel)
        val deleteLabel = context.getString(R.string.common_delete)

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTransitionAnimationsEnabled provides true) {
                    // The production composable, not a rebuilt harness: it is an action sheet now, and
                    // the exit-snapshot behaviour asserted below is a property of that composable rather
                    // than of a dialog wrapper the app no longer uses here.
                    GitHubDeleteTrackDialog(
                        pendingDeleteItem = pendingItem.value,
                        deleteInProgress = false,
                        onDismissRequest = {},
                        onCancel = { cancelCount++ },
                        onConfirmDelete = { confirmCount++ },
                    )
                }
            }
        }

        composeRule.onNode(hasText(title) and isHeading()).assertIsDisplayed()
        composeRule.onNode(hasText(summary)).assertIsDisplayed()
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
        composeRule.onNode(hasText(cancelLabel) and hasClickAction()).performClick()
        composeRule.onNode(hasText(deleteLabel) and hasClickAction()).performClick()

        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { pendingItem.value = null }
        composeRule.mainClock.advanceTimeBy(EXIT_OBSERVATION_MILLIS)

        composeRule.onNode(hasText(summary)).assertIsDisplayed()
        composeRule.onNode(hasText(deleteLabel)).assertIsNotEnabled()
        composeRule.onNode(hasText(cancelLabel)).assertIsNotEnabled()
        // The snapshot now lives inside GitHubDeleteTrackDialog, so it is observed through what it
        // keeps on screen rather than by reaching for the object.

        finishExitAnimation()

        composeRule.onAllNodes(hasText(title)).assertCountEquals(0)
        // Retention is observed through the screen above; the snapshot is internal to the sheet now.
        assertEquals(1, cancelCount)
        assertEquals(1, confirmCount)
    }

    @Test
    fun importExitKeepsPreviewAndRoutesReadyActionsUntilDismissalFinishes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preview = readyImportPreview()
        val pendingPreview = mutableStateOf<GitHubTrackImportPreview?>(preview)
        var observedSnapshot: GitHubTrackDialogExitSnapshot<GitHubTrackImportPreview>? = null
        var cancelCount = 0
        var confirmCount = 0
        val title = context.getString(R.string.github_import_dialog_title)
        val summary = context.getString(R.string.github_import_dialog_summary_ready)
        val cancelLabel = context.getString(R.string.common_cancel)
        val confirmLabel = context.getString(R.string.github_import_dialog_action_confirm)

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTransitionAnimationsEnabled provides true) {
                    val exitSnapshot = rememberGitHubTrackDialogExitSnapshot(pendingPreview.value)
                    val renderedPreview = exitSnapshot.resolve(pendingPreview.value)
                    SideEffect { observedSnapshot = exitSnapshot }
                    LiquidGlassDialog(
                        show = pendingPreview.value != null,
                        title = title,
                        summary = renderedPreview?.let { summary },
                        onDismissFinished = exitSnapshot::clear,
                    ) {
                        renderedPreview?.let {
                            GitHubTrackImportDialogActions(
                                canImport = it.canImport,
                                importInProgress = false,
                                actionsEnabled = pendingPreview.value != null,
                                onDismissRequest = {},
                                onCancel = { cancelCount++ },
                                onConfirmImport = { confirmCount++ },
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNode(hasText(title) and isHeading()).assertIsDisplayed()
        composeRule.onNode(hasText(summary)).assertIsDisplayed()
        composeRule.onNode(hasText(cancelLabel) and hasClickAction()).performClick()
        composeRule.onNode(hasText(confirmLabel) and hasClickAction()).performClick()

        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { pendingPreview.value = null }
        composeRule.mainClock.advanceTimeBy(EXIT_OBSERVATION_MILLIS)

        composeRule.onNode(hasText(summary)).assertIsDisplayed()
        composeRule.onNode(hasText(confirmLabel)).assertIsNotEnabled()
        composeRule.onNode(hasText(cancelLabel)).assertIsNotEnabled()
        assertEquals(preview, assertNotNull(observedSnapshot).retainedValue)

        finishExitAnimation()

        composeRule.onAllNodes(hasText(title)).assertCountEquals(0)
        assertNull(assertNotNull(observedSnapshot).retainedValue)
        assertEquals(1, cancelCount)
        assertEquals(1, confirmCount)
    }

    @Test
    fun importContentRoutesInvalidCloseAndDisablesBusyActions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val importInProgress = mutableStateOf(false)
        var cancelCount = 0
        var dismissCount = 0
        var confirmCount = 0
        val cancelLabel = context.getString(R.string.common_cancel)
        val closeLabel = context.getString(R.string.common_close)
        val importingLabel = context.getString(R.string.github_check_sheet_action_importing)

        composeRule.setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalTransitionAnimationsEnabled provides false) {
                    LiquidGlassDialog(show = true, title = "Import actions") {
                        GitHubTrackImportDialogActions(
                            canImport = false,
                            importInProgress = importInProgress.value,
                            onDismissRequest = { dismissCount++ },
                            onCancel = { cancelCount++ },
                            onConfirmImport = { confirmCount++ },
                        )
                    }
                }
            }
        }

        composeRule.onNode(hasText(cancelLabel) and hasClickAction()).performClick()
        composeRule.onNode(hasText(closeLabel) and hasClickAction()).performClick()
        composeRule.runOnIdle { importInProgress.value = true }
        composeRule.onNode(hasText(importingLabel)).assertIsNotEnabled()
        composeRule.onNode(hasText(cancelLabel)).assertIsNotEnabled()

        assertEquals(1, cancelCount)
        assertEquals(1, dismissCount)
        assertEquals(0, confirmCount)
    }

    @Test
    fun productionDialogsUseSharedHostAndRetainExitSnapshots() {
        val source = trackDialogsSource(GITHUB_TRACK_DIALOGS_SOURCE)

        // The two confirmations deliberately use *different* presentations now.
        //
        // Delete is chosen from the item's More menu, and Apple's pull-down-buttons guidance asks for an
        // action sheet there specifically — because it "appears in a different location from the menu",
        // so a second tap where the first one landed cannot confirm it. Import is not menu-originated, so
        // it stays an alert, which the Action sheets page explicitly allows for confirm-or-cancel.
        assertEquals(1, source.occurrencesOf("AppWindowDialogHost("))
        assertEquals(1, source.occurrencesOf("LiquidActionSheet("))
        assertTrue("role = LiquidActionRole.Destructive" in source)
        assertTrue("role = LiquidActionRole.Cancel" in source)
        // The sheet must not be walk-away-able mid-delete.
        assertTrue("dismissible = !deleteInProgress" in source)
        assertFalse("WindowDialog(" in source)
        assertEquals(
            2,
            source.occurrencesOf("val exitSnapshot = rememberGitHubTrackDialogExitSnapshot("),
        )
        assertEquals(2, source.occurrencesOf("onDismissFinished = exitSnapshot::clear"))
        assertTrue("show = pendingDeleteItem != null" in source)
        assertTrue("show = preview != null" in source)
        assertTrue("renderedDeleteItem?.let" in source)
        assertTrue("renderedPreview?.let" in source)
        assertFalse("return@" in source)
        assertTrue("R.string.github_delete_dialog_title" in source)
        assertTrue("R.string.github_delete_dialog_summary" in source)
        assertTrue("R.string.github_import_dialog_title" in source)
        assertTrue("R.string.github_import_dialog_summary_ready" in source)
        assertTrue("R.string.github_import_dialog_summary_invalid" in source)
        assertTrue("maxWidth = AppDialogDimensions.ContentRichMaxWidth" in source)
        assertTrue("actionsEnabled = preview != null" in source)
        assertTrue("onClick = if (canImport) onConfirmImport else onDismissRequest" in source)
    }

    private fun finishExitAnimation() {
        composeRule.mainClock.advanceTimeBy(EXIT_COMPLETION_MILLIS)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
    }
}

class GitHubTrackDialogsTestApp : Application()

private fun trackedApp(): GitHubTrackedApp =
    GitHubTrackedApp(
        repoUrl = "https://github.com/kyant0/backdrop",
        owner = "kyant0",
        repo = "backdrop",
        packageName = "io.github.kyant0.backdrop",
        appLabel = "Backdrop",
    )

private fun readyImportPreview(): GitHubTrackImportPreview =
    GitHubTrackImportPreview(
        payload = GitHubTrackedItemsImportPayload(),
        fileItemCount = 4,
        validCount = 3,
        duplicateCount = 1,
        invalidCount = 0,
        newCount = 2,
        updatedCount = 1,
        unchangedCount = 0,
        mergedCount = 3,
    )

private fun String.occurrencesOf(needle: String): Int = windowed(needle.length).count(needle::equals)

private fun trackDialogsSource(relativePath: String): String {
    val roots = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
    val source = roots.map { File(it, relativePath) }.firstOrNull(File::isFile)
    return requireNotNull(source) {
        "Unable to locate $relativePath from ${System.getProperty("user.dir")}"
    }.readText()
}

private const val EXIT_OBSERVATION_MILLIS = 16L
private const val EXIT_COMPLETION_MILLIS = 300L
private const val GITHUB_TRACK_DIALOGS_SOURCE =
    "app/src/main/java/os/kei/ui/page/main/github/sheet/GitHubTrackDialogs.kt"
