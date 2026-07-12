@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import os.kei.core.prefs.UiPrefs
import top.yukonga.miuix.kmp.utils.RemovePlatformDialogDefaultEffects
import top.yukonga.miuix.kmp.utils.platformDialogProperties

enum class AppWindowDialogPresentation {
    Card,
    Fullscreen,
}

@Composable
fun AppWindowDialogHost(
    show: Boolean,
    onDismissRequest: (() -> Unit)? = null,
    dismissible: Boolean = true,
    onDismissFinished: (() -> Unit)? = null,
    presentation: AppWindowDialogPresentation = AppWindowDialogPresentation.Card,
    content: @Composable () -> Unit,
) {
    if (
        presentation == AppWindowDialogPresentation.Card &&
        UiPrefs.isLiquidDialogEnabled()
    ) {
        LiquidGlassDialog(
            show = show,
            onDismissRequest = onDismissRequest,
            dismissible = dismissible,
            onDismissFinished = onDismissFinished,
            content = content,
        )
        return
    }

    var rawDialogWasShown by remember { mutableStateOf(false) }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)
    LaunchedEffect(show) {
        if (show) {
            rawDialogWasShown = true
        } else if (rawDialogWasShown) {
            rawDialogWasShown = false
            currentOnDismissFinished?.invoke()
        }
    }
    if (!show) return

    Dialog(
        onDismissRequest = {
            if (dismissible) {
                currentOnDismissRequest?.invoke()
            }
        },
        properties = platformDialogProperties(),
    ) {
        RemovePlatformDialogDefaultEffects()
        BackHandler(enabled = dismissible) {
            currentOnDismissRequest?.invoke()
        }
        content()
    }
}
