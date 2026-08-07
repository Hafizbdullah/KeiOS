package os.kei.ui.page.main.widget.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import os.kei.ui.liquidglass.R
import os.kei.ui.page.main.widget.dialog.LiquidActionRole
import os.kei.ui.page.main.widget.dialog.LiquidActionSheet
import os.kei.ui.page.main.widget.dialog.LiquidPresentationAction

@Stable
class UnsavedSheetDismissHandler(
    val showConfirmDialog: Boolean,
    val allowDismiss: Boolean,
    val requestDismiss: () -> Unit,
    val keepEditing: () -> Unit,
    val discardChanges: () -> Unit
)

@Composable
fun rememberUnsavedSheetDismissHandler(
    hasUnsavedChanges: Boolean,
    onDismissRequest: () -> Unit
): UnsavedSheetDismissHandler {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val currentHasUnsavedChanges by rememberUpdatedState(hasUnsavedChanges)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    LaunchedEffect(hasUnsavedChanges) {
        if (!hasUnsavedChanges) {
            showConfirmDialog = false
        }
    }

    return remember(showConfirmDialog, hasUnsavedChanges) {
        UnsavedSheetDismissHandler(
            showConfirmDialog = showConfirmDialog,
            allowDismiss = !hasUnsavedChanges,
            requestDismiss = {
                if (currentHasUnsavedChanges) {
                    showConfirmDialog = true
                } else {
                    currentOnDismissRequest()
                }
            },
            keepEditing = { showConfirmDialog = false },
            discardChanges = {
                showConfirmDialog = false
                currentOnDismissRequest()
            }
        )
    }
}

/**
 * The unsaved-changes confirmation, as an action sheet.
 *
 * This is the case Apple's sheet guidance names outright: *"If people have unsaved changes in the
 * sheet when they begin swiping to dismiss it, use an action sheet to let them confirm their action."*
 * It offers a choice about something the person deliberately started, which is an action sheet's job —
 * not an alert's, and an alert is what this used to be.
 *
 * It also used to be miuix's `WindowDialog` inside an `AppLiquidWindowBoundary`, so it had no glass at
 * all: the boundary blanks `LocalSceneBackdrop`, and every blur inside a Dialog window draws nothing.
 *
 * The roles do the layout. Discarding is destructive so it rises to the top where it is most
 * noticeable; keeping editing dismisses without acting, which makes it the Cancel, so it sinks to the
 * bottom behind a wider gap.
 */
@Composable
fun UnsavedSheetDismissConfirmDialog(
    show: Boolean,
    onKeepEditing: () -> Unit,
    onDiscardChanges: () -> Unit
) {
    LiquidActionSheet(
        show = show,
        title = stringResource(R.string.common_unsaved_changes_title),
        message = stringResource(R.string.common_unsaved_changes_summary),
        actions = listOf(
            LiquidPresentationAction(
                label = stringResource(R.string.common_discard_changes),
                onClick = onDiscardChanges,
                role = LiquidActionRole.Destructive
            ),
            LiquidPresentationAction(
                label = stringResource(R.string.common_keep_editing),
                onClick = onKeepEditing,
                role = LiquidActionRole.Cancel
            )
        ),
        onDismissRequest = onKeepEditing
    )
}
